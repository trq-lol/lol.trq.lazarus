/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * With {@code -Dlazarus.gl=core}, sends the fixed-function GL calls of every game class to the LWJGL 3 jar's
 * emulator, and flushes the emulator's batched draws before every other OpenGL call. The emulated set is read from
 * the emulator's own public static methods, so the two never drift.
 */
final class CoreGlPatch extends InvokeRewritePatch {
    private static final String EMULATOR = "lol/trq/lazarus/lwjgl3/gl/CoreGl";
    private static final String EXTENSIONS = "lol/trq/lazarus/lwjgl3/gl/CoreExtensions";
    private static final Set<String> OWNERS = Set.of(
            "org/lwjgl/opengl/GL11",
            "org/lwjgl/opengl/GL13",
            "org/lwjgl/opengl/GL15",
            "org/lwjgl/opengl/GL20",
            "org/lwjgl/opengl/GL30",
            "org/lwjgl/opengl/ARBMultitexture",
            "org/lwjgl/opengl/ARBVertexBufferObject",
            "org/lwjgl/opengl/ARBShaderObjects",
            "org/lwjgl/opengl/ARBVertexShader",
            "org/lwjgl/opengl/ARBGeometryShader4",
            "org/lwjgl/opengl/EXTBlendFuncSeparate",
            "org/lwjgl/opengl/EXTFramebufferObject");

    private static final boolean DEBUG = Boolean.getBoolean("lazarus.gl.debug");
    /** Fixed-function GL11 calls with no core equivalent that the emulator does not implement. */
    private static final Set<String> UNEMULATED_LEGACY = Set.of(
            "glColor4ub",
            "glVertex2d",
            "glVertex3d",
            "glVertex2i",
            "glTexCoord2d",
            "glRotated",
            "glFrustum",
            "glPolygonMode",
            "glClipPlane",
            "glRasterPos2f",
            "glBitmap",
            "glDrawPixels",
            "glLightf",
            "glMaterial",
            "glMaterialf",
            "glNormal3b",
            "glPushClientAttrib",
            "glPopClientAttrib",
            "glInterleavedArrays",
            "glArrayElement",
            "glEdgeFlag",
            "glIndexi",
            "glListBase",
            "glIsList");

    private static final Set<String> REPORTED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Emulated name and descriptor, to the class that implements it. */
    private final Map<String, String> emulated;

    private CoreGlPatch(Map<String, String> emulated) {
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
        Map<String, String> methods = new HashMap<>();
        for (String owner : List.of(EXTENSIONS, EMULATOR)) {
            try (InputStream in = ClassLoader.getSystemResourceAsStream(owner + ".class")) {
                new ClassReader(in.readAllBytes())
                        .accept(
                                new ClassVisitor(Opcodes.ASM9) {
                                    @Override
                                    public MethodVisitor visitMethod(
                                            int access, String n, String d, String s, String[] e) {
                                        int wanted = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                                        if ((access & wanted) == wanted && n.startsWith("gl")) {
                                            methods.put(n + d, owner);
                                        }
                                        return null;
                                    }
                                },
                                ClassReader.SKIP_CODE);
            } catch (IOException | NullPointerException e) {
                throw new IllegalStateException("cannot read the GL emulator class " + owner, e);
            }
        }
        return new CoreGlPatch(Map.copyOf(methods));
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
        if (op != Opcodes.INVOKESTATIC || !o.startsWith("org/lwjgl/opengl/")) {
            return false;
        }
        String target = OWNERS.contains(o) ? emulated.get(m + desc) : null;
        if (target != null) {
            out.visitMethodInsn(op, target, m, desc, false);
            return true;
        }
        if (DEBUG && o.equals("org/lwjgl/opengl/GL11") && UNEMULATED_LEGACY.contains(m) && REPORTED.add(m)) {
            System.err.println("[lazarus] core renderer: " + m + " is not emulated and will fail on a core profile");
        }
        // Any other OpenGL call may depend on, or change, state that pending batched draws were built with.
        out.visitMethodInsn(Opcodes.INVOKESTATIC, EMULATOR, "flush", "()V", false);
        out.visitMethodInsn(op, o, m, desc, false);
        return true;
    }
}
