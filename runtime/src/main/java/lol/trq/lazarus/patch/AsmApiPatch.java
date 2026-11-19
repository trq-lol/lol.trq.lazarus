/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Raises the ASM api level that legacy code passes to ASM visitor constructors.
 *
 * <p>Code written for ASM 4 or 5 constructs visitors with {@code Opcodes.ASM4} or {@code ASM5}. On a current ASM,
 * such a visitor throws when it meets a class feature newer than its api level (modules, nest mates, records,
 * permitted subclasses), which every class compiled for Java 9 or newer carries. Raising the constant to {@code
 * ASM9} makes those visits default to pass-through, which is what the legacy code would have done had it known
 * about them.
 *
 * <p>Only a constant that reaches a constructor taking an {@code int} first, within a few instructions, is changed.
 */
public final class AsmApiPatch extends TreePatch {
    static final int ASM4 = 4 << 16;
    static final int ASM5 = 5 << 16;
    static final int ASM6 = 6 << 16;
    private static final int LOOKAHEAD = 8;
    private static final byte[] ASM_MARKER = "org/objectweb/asm/".getBytes(StandardCharsets.ISO_8859_1);

    private final Set<String> prefixes;

    /**
     * Creates the patch.
     *
     * @param prefixes internal-name prefixes of the legacy code to patch
     */
    public AsmApiPatch(Set<String> prefixes) {
        super("asm-api-level", null);
        this.prefixes = Set.copyOf(prefixes);
    }

    @Override
    public Set<String> targets() {
        return Set.of();
    }

    @Override
    public Set<String> prefixes() {
        return prefixes;
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        if (!Bytes.contains(bytes, ASM_MARKER)) {
            return bytes;
        }
        return super.apply(className, bytes);
    }

    @Override
    protected boolean transform(ClassNode node) {
        boolean changed = false;
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (isApiConstant(insn) && feedsAsmConstructor(insn)) {
                    method.instructions.set(insn, insn = new LdcInsnNode(Opcodes.ASM9));
                    changed = true;
                }
            }
        }
        return changed;
    }

    @Override
    protected int writerFlags() {
        return 0;
    }

    private static boolean isApiConstant(AbstractInsnNode insn) {
        return insn instanceof LdcInsnNode ldc
                && ldc.cst instanceof Integer value
                && (value == ASM4 || value == ASM5 || value == ASM6);
    }

    /** Whether the constant reaches an ASM constructor taking an int first, within a few instructions. */
    private static boolean feedsAsmConstructor(AbstractInsnNode insn) {
        AbstractInsnNode next = insn.getNext();
        for (int i = 0; next != null && i < LOOKAHEAD; i++, next = next.getNext()) {
            if (next instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && call.name.equals("<init>")) {
                return call.desc.startsWith("(I");
            }
        }
        return false;
    }
}
