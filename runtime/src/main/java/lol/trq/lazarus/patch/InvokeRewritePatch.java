/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** A patch that rewrites method calls; the class is rewritten only when a call was. */
abstract class InvokeRewritePatch implements ClassPatch {
    @Override
    public byte[] apply(String className, byte[] bytes) {
        boolean[] changed = {false};
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM9, writer) {
                            @Override
                            public MethodVisitor visitMethod(int access, String n, String d, String s, String[] e) {
                                return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, n, d, s, e)) {
                                    @Override
                                    public void visitMethodInsn(int op, String o, String m, String desc, boolean itf) {
                                        if (rewrite(mv, op, o, m, desc)) {
                                            changed[0] = true;
                                        } else {
                                            super.visitMethodInsn(op, o, m, desc, itf);
                                        }
                                    }
                                };
                            }
                        },
                        0);
        return changed[0] ? writer.toByteArray() : bytes;
    }

    /**
     * Emits a replacement for one call, or nothing.
     *
     * @param out where to emit the replacement
     * @param opcode the call's opcode
     * @param owner the called method's class
     * @param name the called method's name
     * @param descriptor the called method's descriptor
     * @return whether a replacement was emitted
     */
    protected abstract boolean rewrite(MethodVisitor out, int opcode, String owner, String name, String descriptor);
}
