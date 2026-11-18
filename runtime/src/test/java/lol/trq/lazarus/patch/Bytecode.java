/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.function.Consumer;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Builds and loads small classes for patch tests. */
final class Bytecode {
    private Bytecode() {}

    /** Defines classes from bytes, one loader per test. */
    static final class Loader extends ClassLoader {
        Loader() {
            super(Bytecode.class.getClassLoader());
        }

        Class<?> define(String internalName, byte[] bytes) {
            return defineClass(internalName.replace('/', '.'), bytes, 0, bytes.length);
        }
    }

    /**
     * Writes a public class with a public no-arg constructor and one public static method.
     *
     * @param name the internal name
     * @param superName the internal name of the superclass
     * @param method the method's name
     * @param desc the method's descriptor
     * @param body writes the method's code; frames and maxs are computed
     * @return the class file
     */
    static byte[] staticMethod(
            String name, String superName, String method, String desc, Consumer<MethodVisitor> body) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null, superName, null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, method, desc, null, null);
        mv.visitCode();
        body.accept(mv);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }
}
