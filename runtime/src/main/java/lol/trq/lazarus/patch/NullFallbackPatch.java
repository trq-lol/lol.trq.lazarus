/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Gives a static lookup method a fallback: when it returns {@code null} and a hook accepts its arguments, it returns
 * the value of a static field instead.
 *
 * <p>The original method is renamed and a new method with the original name and descriptor wraps it. The hook takes
 * the method's parameters and returns {@code boolean}.
 */
public final class NullFallbackPatch extends TreePatch {
    private final String method;
    private final String descriptor;
    private final String hookOwner;
    private final String hookName;
    private final String fallbackField;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     * @param method the static method to wrap
     * @param descriptor its descriptor; must return an object
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name; its descriptor is the method's parameters returning {@code Z}
     * @param fallbackField a static field of {@code owner} whose type is the method's return type
     */
    public NullFallbackPatch(
            String name,
            String owner,
            String method,
            String descriptor,
            String hookOwner,
            String hookName,
            String fallbackField) {
        super(name, owner);
        this.method = method;
        this.descriptor = descriptor;
        this.hookOwner = hookOwner;
        this.hookName = hookName;
        this.fallbackField = fallbackField;
    }

    @Override
    protected boolean transform(ClassNode node) {
        MethodNode original = null;
        for (MethodNode m : node.methods) {
            if (m.name.equals(method) && m.desc.equals(descriptor) && (m.access & Opcodes.ACC_STATIC) != 0) {
                original = m;
            }
        }
        if (original == null) {
            return false;
        }
        String renamed = "lazarus$" + method;
        original.name = renamed;
        int access = original.access;
        original.access = (access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PRIVATE;

        Type returnType = Type.getReturnType(descriptor);
        Type[] args = Type.getArgumentTypes(descriptor);
        MethodNode wrapper = new MethodNode(access, method, descriptor, null, null);
        InsnList code = wrapper.instructions;
        loadArgs(code, args, 0);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner(), renamed, descriptor, false));
        LabelNode done = new LabelNode();
        LabelNode noFallback = new LabelNode();
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new JumpInsnNode(Opcodes.IFNONNULL, done));
        code.add(new InsnNode(Opcodes.POP));
        loadArgs(code, args, 0);
        code.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, hookOwner, hookName, Type.getMethodDescriptor(Type.BOOLEAN_TYPE, args), false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, noFallback));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, owner(), fallbackField, returnType.getDescriptor()));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(noFallback);
        code.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        code.add(new InsnNode(Opcodes.ACONST_NULL));
        code.add(new InsnNode(Opcodes.ARETURN));
        code.add(done);
        code.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[] {returnType.getInternalName()}));
        code.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(wrapper);
        return true;
    }
}
