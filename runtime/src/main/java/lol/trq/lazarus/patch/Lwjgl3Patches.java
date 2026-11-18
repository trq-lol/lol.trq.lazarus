/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/**
 * Patches that let LWJGL 2 code run on LWJGL 3: LWJGL 2-shaped aliases added to LWJGL 3's GL and AL classes, and
 * paulscode's OpenAL lifecycle calls redirected. They only act when the Lazarus LWJGL 3 jar is on the class path.
 */
final class Lwjgl3Patches {
    static final String GL2 = "lol/trq/lazarus/lwjgl3/Gl2";
    static final String AL2 = "lol/trq/lazarus/lwjgl3/Al2";
    static final String CLIPBOARD = "lol/trq/lazarus/lwjgl3/Clipboard";
    static final String GUI_SCREEN = "net/minecraft/client/gui/GuiScreen";

    private static final String FB = "Ljava/nio/FloatBuffer;";
    private static final String IB = "Ljava/nio/IntBuffer;";
    private static final String BB = "Ljava/nio/ByteBuffer;";

    private Lwjgl3Patches() {}

    /** {@return whether the LWJGL 3 compatibility jar is on the system class path} */
    static boolean active() {
        return ClassLoader.getSystemResource(GL2 + ".class") != null;
    }

    static List<ClassPatch> all() {
        List<ClassPatch> patches = new ArrayList<>();
        patches.add(aliases("org/lwjgl/opengl/GL11", GL2, "GL11", new String[][] {
            {"glFog", "(I" + FB + ")V"},
            {"glGetFloat", "(I" + FB + ")V"},
            {"glGetInteger", "(I" + IB + ")V"},
            {"glLight", "(II" + FB + ")V"},
            {"glLightModel", "(I" + FB + ")V"},
            {"glMultMatrix", "(" + FB + ")V"},
            {"glTexEnv", "(II" + FB + ")V"},
            {"glTexGen", "(II" + FB + ")V"},
        }));
        patches.add(aliases("org/lwjgl/opengl/GL20", GL2, "GL20", uniforms("glShaderSource", "glUniform", "")));
        patches.add(aliases(
                "org/lwjgl/opengl/ARBShaderObjects",
                GL2,
                "ARBShaderObjects",
                concat(
                        uniforms("glShaderSourceARB", "glUniform", "ARB"),
                        new String[][] {{"glGetObjectParameterARB", "(II" + IB + ")V"}})));
        patches.add(aliases("org/lwjgl/opengl/ARBDebugOutput", GL2, "ARBDebugOutput", new String[][] {
            {"glDebugMessageCallbackARB", "(Lorg/lwjgl/opengl/ARBDebugOutputCallback;)V"}
        }));
        patches.add(aliases("org/lwjgl/openal/AL10", AL2, "AL10", new String[][] {
            {"alListener", "(I" + FB + ")V"}, {"alSource", "(II" + FB + ")V"}, {"alSourceStop", "(" + IB + ")V"}
        }));
        patches.add(aliases("org/lwjgl/openal/AL", AL2, "AL", new String[][] {{"isCreated", "()Z"}}));
        for (String method : List.of("create", "destroy")) {
            patches.add(new ConditionalPatch(
                    new RedirectPatch(
                            "lwjgl3-openal-" + method,
                            Set.of("paulscode/sound/libraries/LibraryLWJGLOpenAL"),
                            Opcodes.INVOKESTATIC,
                            "org/lwjgl/openal/AL",
                            method,
                            "()V",
                            AL2,
                            method),
                    Lwjgl3Patches::active));
        }
        // Minecraft's clipboard goes through AWT, which is headless on macOS with LWJGL 3; use GLFW's (SRG names).
        patches.add(new ConditionalPatch(
                new ReplaceBodyPatch(
                        "lwjgl3-clipboard-get",
                        GUI_SCREEN,
                        "func_146277_j",
                        "()Ljava/lang/String;",
                        CLIPBOARD,
                        "getClipboardString"),
                Lwjgl3Patches::active));
        patches.add(new ConditionalPatch(
                new ReplaceBodyPatch(
                        "lwjgl3-clipboard-set",
                        GUI_SCREEN,
                        "func_146275_d",
                        "(Ljava/lang/String;)V",
                        CLIPBOARD,
                        "setClipboardString"),
                Lwjgl3Patches::active));
        return patches;
    }

    private static String[][] uniforms(String shaderSource, String uniform, String suffix) {
        List<String[]> out = new ArrayList<>();
        out.add(new String[] {shaderSource, "(I" + BB + ")V"});
        for (int n = 1; n <= 4; n++) {
            out.add(new String[] {uniform + n + suffix, "(I" + FB + ")V"});
            out.add(new String[] {uniform + n + suffix, "(I" + IB + ")V"});
        }
        for (int n = 2; n <= 4; n++) {
            out.add(new String[] {uniform + "Matrix" + n + suffix, "(IZ" + FB + ")V"});
        }
        return out.toArray(String[][]::new);
    }

    private static String[][] concat(String[][] a, String[][] b) {
        String[][] out = new String[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static ClassPatch aliases(String owner, String hookOwner, String prefix, String[][] methods) {
        List<AddMethodsPatch.Method> list = new ArrayList<>();
        for (String[] m : methods) {
            list.add(new AddMethodsPatch.Method(m[0], m[1], hookOwner, prefix + "_" + m[0]));
        }
        String simple = owner.substring(owner.lastIndexOf('/') + 1);
        return new ConditionalPatch(
                new AddMethodsPatch("lwjgl3-aliases-" + simple, owner, list), Lwjgl3Patches::active);
    }
}
