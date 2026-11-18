/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Replaces a static method's body with a call to a static hook.
 *
 * <p>The hook receives the declaring class first, then the method's parameters, and returns the method's return type.
 */
public final class ReplaceBodyPatch implements ClassPatch {
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
     * @param method the static method to replace
     * @param descriptor its descriptor
     * @param hookOwner the internal name of the hook's class
     * @param hookName the hook's name; it takes {@code Class} and then the method's parameters
     */
    public ReplaceBodyPatch(
            String name, String owner, String method, String descriptor, String hookOwner, String hookName) {
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
            if (!m.name.equals(method) || !m.desc.equals(descriptor) || (m.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }
            Type[] args = Type.getArgumentTypes(descriptor);
            Type returnType = Type.getReturnType(descriptor);
            InsnList body = new InsnList();
            body.add(new LdcInsnNode(Type.getObjectType(owner)));
            int slot = 0;
            for (Type arg : args) {
                body.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
                slot += arg.getSize();
            }
            Type[] hookArgs = new Type[args.length + 1];
            hookArgs[0] = Type.getType(Class.class);
            System.arraycopy(args, 0, hookArgs, 1, args.length);
            body.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, hookOwner, hookName, Type.getMethodDescriptor(returnType, hookArgs), false));
            body.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
            m.instructions = body;
            m.tryCatchBlocks.clear();
            m.localVariables = null;
            m.visibleLocalVariableAnnotations = null;
            m.invisibleLocalVariableAnnotations = null;
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
