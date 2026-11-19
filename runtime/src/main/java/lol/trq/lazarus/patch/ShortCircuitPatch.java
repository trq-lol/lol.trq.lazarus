/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

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

/**
 * Makes a {@code boolean} method return {@code true} early when a static hook, given the method's arguments, says so.
 *
 * <p>The hook takes exactly the method's parameters (not the receiver) and returns {@code boolean}. When it returns
 * {@code false} the original method body runs unchanged.
 */
public final class ShortCircuitPatch extends TreePatch {
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
        super(name, owner);
        if (!descriptor.endsWith(")Z")) {
            throw new IllegalArgumentException("only boolean methods can be short-circuited: " + descriptor);
        }
        this.method = method;
        this.descriptor = descriptor;
        this.hookOwner = hookOwner;
        this.hookName = hookName;
    }

    @Override
    protected boolean transform(ClassNode node) {
        boolean changed = false;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(method) || !m.desc.equals(descriptor)) {
                continue;
            }
            InsnList head = new InsnList();
            loadArgs(head, Type.getArgumentTypes(m.desc), (m.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1);
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
        return changed;
    }
}
