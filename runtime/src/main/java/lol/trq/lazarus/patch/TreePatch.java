/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** A patch of one class that edits its tree form; the class is rewritten only when the edit changed it. */
abstract class TreePatch implements ClassPatch {
    private final String name;
    private final String owner;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     */
    TreePatch(String name, String owner) {
        this.name = name;
        this.owner = owner;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> targets() {
        return Set.of(owner);
    }

    /** {@return the internal name of the class to patch} */
    final String owner() {
        return owner;
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        if (!transform(node)) {
            return bytes;
        }
        ClassWriter writer = new ClassWriter(writerFlags());
        node.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Edits the class in place.
     *
     * @param node the class
     * @return whether anything changed
     */
    protected abstract boolean transform(ClassNode node);

    /** {@return the {@link ClassWriter} flags used to write a changed class} */
    protected int writerFlags() {
        return ClassWriter.COMPUTE_MAXS;
    }

    /**
     * Appends loads of the given arguments from consecutive local slots.
     *
     * @param code the instructions to append to
     * @param args the argument types
     * @param firstSlot the slot of the first argument
     */
    static void loadArgs(InsnList code, Type[] args, int firstSlot) {
        int slot = firstSlot;
        for (Type arg : args) {
            code.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
            slot += arg.getSize();
        }
    }

    /**
     * Replaces a method's code, dropping everything tied to the old instructions.
     *
     * @param method the method
     * @param body its new instructions
     */
    static void replaceBody(MethodNode method, InsnList body) {
        method.instructions = body;
        method.tryCatchBlocks.clear();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
    }
}
