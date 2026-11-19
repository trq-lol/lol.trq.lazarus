/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * With {@code -Dlazarus.gl=core}, sends the fixed-function GL calls of every game class to the LWJGL 3 jar's
 * emulator. The emulated set is read from the emulator's own public static methods, so the two never drift.
 */
final class CoreGlPatch extends InvokeRewritePatch {
    private static final String EMULATOR = "lol/trq/lazarus/lwjgl3/gl/CoreGl";
    private static final Set<String> OWNERS = Set.of(
            "org/lwjgl/opengl/GL11",
            "org/lwjgl/opengl/GL13",
            "org/lwjgl/opengl/GL15",
            "org/lwjgl/opengl/GL20",
            "org/lwjgl/opengl/GL30",
            "org/lwjgl/opengl/ARBMultitexture",
            "org/lwjgl/opengl/ARBVertexBufferObject",
            "org/lwjgl/opengl/ARBShaderObjects");

    private final Set<String> emulated;

    private CoreGlPatch(Set<String> emulated) {
        this.emulated = emulated;
    }

    /** {@return whether the core renderer was requested and the emulator is on the class path} */
    static boolean enabled() {
        return "core".equals(System.getProperty("lazarus.gl"))
                && ClassLoader.getSystemResource(EMULATOR + ".class") != null;
    }

    /** {@return the patch, or {@code null} when the core renderer is off} */
    static ClassPatch create() {
        if (!enabled()) {
            return null;
        }
        Set<String> methods = new HashSet<>();
        try (InputStream in = ClassLoader.getSystemResourceAsStream(EMULATOR + ".class")) {
            new ClassReader(in.readAllBytes())
                    .accept(
                            new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public MethodVisitor visitMethod(int access, String n, String d, String s, String[] e) {
                                    int wanted = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                                    if ((access & wanted) == wanted && n.startsWith("gl")) {
                                        methods.add(n + d);
                                    }
                                    return null;
                                }
                            },
                            ClassReader.SKIP_CODE);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the GL emulator", e);
        }
        return new CoreGlPatch(Set.copyOf(methods));
    }

    @Override
    public String name() {
        return "core-gl";
    }

    @Override
    public Set<String> targets() {
        return Set.of();
    }

    @Override
    public boolean matches(String className) {
        if (className.startsWith("org/lwjgl/")) {
            return className.startsWith("org/lwjgl/util/");
        }
        return !className.startsWith("lol/trq/lazarus/")
                && !className.startsWith("java/")
                && !className.startsWith("javax/")
                && !className.startsWith("jdk/")
                && !className.startsWith("sun/")
                && !className.startsWith("com/sun/");
    }

    @Override
    protected boolean rewrite(MethodVisitor out, int op, String o, String m, String desc) {
        if (op != Opcodes.INVOKESTATIC || !OWNERS.contains(o) || !emulated.contains(m + desc)) {
            return false;
        }
        out.visitMethodInsn(op, EMULATOR, m, desc, false);
        return true;
    }
}
