/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Replaces a static method's body with a call to a static hook.
 *
 * <p>The hook receives the declaring class first, then the method's parameters, and returns the method's return type.
 */
public final class ReplaceBodyPatch extends TreePatch {
    private final String method;
    private final String descriptor;
    private final String hookOwner;
    private final String hookName;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     * @param method the static method to replace
     * @param descriptor its descriptor
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name; it takes {@code Class} and then the method's parameters
     */
    public ReplaceBodyPatch(
            String name, String owner, String method, String descriptor, String hookOwner, String hookName) {
        super(name, owner);
        this.method = method;
        this.descriptor = descriptor;
        this.hookOwner = hookOwner;
        this.hookName = hookName;
    }

    @Override
    protected boolean transform(ClassNode node) {
        boolean changed = false;
        for (MethodNode m : node.methods) {
            if (!m.name.equals(method) || !m.desc.equals(descriptor) || (m.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }
            Type[] args = Type.getArgumentTypes(descriptor);
            Type returnType = Type.getReturnType(descriptor);
            InsnList body = new InsnList();
            body.add(new LdcInsnNode(Type.getObjectType(owner())));
            loadArgs(body, args, 0);
            Type[] hookArgs = new Type[args.length + 1];
            hookArgs[0] = Type.getType(Class.class);
            System.arraycopy(args, 0, hookArgs, 1, args.length);
            body.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, hookOwner, hookName, Type.getMethodDescriptor(returnType, hookArgs), false));
            body.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
            replaceBody(m, body);
            changed = true;
        }
        return changed;
    }
}
