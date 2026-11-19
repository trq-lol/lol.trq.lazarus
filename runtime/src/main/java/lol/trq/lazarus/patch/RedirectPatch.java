/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Set;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Redirects every call to one method to a static replacement.
 *
 * <p>A static call keeps its descriptor. A virtual call becomes a static call whose first parameter is the receiver.
 */
public final class RedirectPatch extends InvokeRewritePatch {
    private final String name;
    private final Set<String> targets;
    private final int opcode;
    private final String owner;
    private final String method;
    private final String descriptor;
    private final String newOwner;
    private final String newMethod;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param targets the internal names of the classes to patch
     * @param opcode {@code INVOKESTATIC} or {@code INVOKEVIRTUAL}, the call to match
     * @param owner the internal name of the called method's class
     * @param method the called method's name
     * @param descriptor the called method's descriptor
     * @param newOwner the internal name of the replacement's class
     * @param newMethod the replacement's name
     */
    public RedirectPatch(
            String name,
            Set<String> targets,
            int opcode,
            String owner,
            String method,
            String descriptor,
            String newOwner,
            String newMethod) {
        this.name = name;
        this.targets = Set.copyOf(targets);
        this.opcode = opcode;
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.newOwner = newOwner;
        this.newMethod = newMethod;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> targets() {
        return targets;
    }

    @Override
    protected boolean rewrite(MethodVisitor out, int op, String o, String m, String d) {
        if (op != opcode || !o.equals(owner) || !m.equals(method) || !d.equals(descriptor)) {
            return false;
        }
        String newDesc = op == Opcodes.INVOKESTATIC ? d : "(L" + o + ";" + d.substring(1);
        out.visitMethodInsn(Opcodes.INVOKESTATIC, newOwner, newMethod, newDesc, false);
        return true;
    }
}
