/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Adds public static methods to a class, each delegating to a static hook with the same descriptor. A method the
 * class already declares is left alone, so the patch is harmless on a library that still has it.
 *
 * <p>Used to give LWJGL 3 classes the LWJGL 2 names and shapes that old code links against.
 */
public final class AddMethodsPatch extends TreePatch {
    /**
     * One method to add.
     *
     * @param name the method name
     * @param descriptor the method descriptor, shared by the hook
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name
     */
    public record Method(String name, String descriptor, String hookOwner, String hookName) {}

    private final List<Method> methods;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to add to
     * @param methods the methods to add
     */
    public AddMethodsPatch(String name, String owner, List<Method> methods) {
        super(name, owner);
        this.methods = List.copyOf(methods);
    }

    @Override
    protected boolean transform(ClassNode node) {
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
            loadArgs(m.instructions, Type.getArgumentTypes(add.descriptor()), 0);
            m.instructions.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, add.hookOwner(), add.hookName(), add.descriptor(), false));
            m.instructions.add(new InsnNode(Type.getReturnType(add.descriptor()).getOpcode(Opcodes.IRETURN)));
            node.methods.add(m);
            changed = true;
        }
        return changed;
    }
}
