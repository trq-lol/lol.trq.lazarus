/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Makes every method of a given name that returns an object return {@code null} without running its body. */
public final class NullReturnPatch extends TreePatch {
    private final String method;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner the internal name of the class to patch
     * @param method the name of the methods to neutralize
     */
    public NullReturnPatch(String name, String owner, String method) {
        super(name, owner);
        this.method = method;
    }

    @Override
    protected boolean transform(ClassNode node) {
        boolean changed = false;
        for (MethodNode m : node.methods) {
            int sort = Type.getReturnType(m.desc).getSort();
            if (!m.name.equals(method)
                    || (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0
                    || (sort != Type.OBJECT && sort != Type.ARRAY)) {
                continue;
            }
            InsnList body = new InsnList();
            body.add(new InsnNode(Opcodes.ACONST_NULL));
            body.add(new InsnNode(Opcodes.ARETURN));
            replaceBody(m, body);
            changed = true;
        }
        return changed;
    }
}
