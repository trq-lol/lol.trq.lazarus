/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Makes a {@code boolean} method return {@code true} early when a static hook, given the method's arguments, says so.
 *
 * <p>The hook takes exactly the method's parameters (not the receiver) and returns {@code boolean}. When it returns
 * {@code false} the original method body runs unchanged.
 */
public final class ShortCircuitPatch implements ClassPatch {
    private final String name;
    private final String owner;
    private final String method;
    private final String descriptor;
    private final String hookOwner;
    private final String hookName;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     * @param method the method to patch
     * @param descriptor its descriptor; must return {@code Z}
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name; its descriptor is the method's parameters returning {@code Z}
     */
    public ShortCircuitPatch(
            String name, String owner, String method, String descriptor, String hookOwner, String hookName) {
        if (!descriptor.endsWith(")Z")) {
            throw new IllegalArgumentException("only boolean methods can be short-circuited: " + descriptor);
        }
        this.name = name;
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.hookOwner = hookOwner;
        this.hookName = hookName;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> targets() {
        return Set.of(owner);
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean changed = false;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(method) || !m.desc.equals(descriptor)) {
                continue;
            }
            boolean isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
            InsnList head = new InsnList();
            int slot = isStatic ? 0 : 1;
            for (Type arg : Type.getArgumentTypes(m.desc)) {
                head.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
                slot += arg.getSize();
            }
            LabelNode proceed = new LabelNode();
            head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, hookOwner, hookName, descriptor, false));
            head.add(new JumpInsnNode(Opcodes.IFEQ, proceed));
            head.add(new InsnNode(Opcodes.ICONST_1));
            head.add(new InsnNode(Opcodes.IRETURN));
            head.add(proceed);
            head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
            m.instructions.insert(head);
            changed = true;
        }
        if (!changed) {
            return bytes;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }
}
