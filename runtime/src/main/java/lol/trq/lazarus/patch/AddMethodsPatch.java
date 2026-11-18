/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Adds public static methods to a class, each delegating to a static hook with the same descriptor. A method the
 * class already declares is left alone, so the patch is harmless on a library that still has it.
 *
 * <p>Used to give LWJGL 3 classes the LWJGL 2 names and shapes that old code links against.
 */
public final class AddMethodsPatch implements ClassPatch {
    /**
     * One method to add.
     *
     * @param name the method name
     * @param descriptor the method descriptor, shared by the hook
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name
     */
    public record Method(String name, String descriptor, String hookOwner, String hookName) {}

    private final String name;
    private final String owner;
    private final List<Method> methods;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to add to
     * @param methods the methods to add
     */
    public AddMethodsPatch(String name, String owner, List<Method> methods) {
        this.name = name;
        this.owner = owner;
        this.methods = List.copyOf(methods);
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
        Set<String> existing = new HashSet<>();
        for (MethodNode m : node.methods) {
            existing.add(m.name + m.desc);
        }
        boolean changed = false;
        for (Method add : methods) {
            if (!existing.add(add.name() + add.descriptor())) {
                continue;
            }
            MethodNode m =
                    new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, add.name(), add.descriptor(), null, null);
            int slot = 0;
            for (Type arg : Type.getArgumentTypes(add.descriptor())) {
                m.instructions.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
                slot += arg.getSize();
            }
            m.instructions.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, add.hookOwner(), add.hookName(), add.descriptor(), false));
            m.instructions.add(new InsnNode(Type.getReturnType(add.descriptor()).getOpcode(Opcodes.IRETURN)));
            node.methods.add(m);
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
