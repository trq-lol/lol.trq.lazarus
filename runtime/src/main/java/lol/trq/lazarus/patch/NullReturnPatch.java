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
import org.objectweb.asm.tree.MethodNode;

/** Makes every method of a given name that returns an object return {@code null} without running its body. */
public final class NullReturnPatch implements ClassPatch {
    private final String name;
    private final String owner;
    private final String method;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     * @param method the name of the methods to neutralize
     */
    public NullReturnPatch(String name, String owner, String method) {
        this.name = name;
        this.owner = owner;
        this.method = method;
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
            int sort = Type.getReturnType(m.desc).getSort();
            if (!m.name.equals(method)
                    || (m.access & Opcodes.ACC_ABSTRACT) != 0
                    || (sort != Type.OBJECT && sort != Type.ARRAY)) {
                continue;
            }
            InsnList body = new InsnList();
            body.add(new InsnNode(Opcodes.ACONST_NULL));
            body.add(new InsnNode(Opcodes.ARETURN));
            m.instructions = body;
            m.tryCatchBlocks.clear();
            m.localVariables = null;
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
