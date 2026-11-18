/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.ARBDebugOutputCallback;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.system.MemoryUtil;

/**
 * LWJGL 2 OpenGL methods whose LWJGL 3 counterparts were renamed or changed shape. Lazarus's agent adds a method with
 * the LWJGL 2 name and descriptor to the LWJGL 3 class, delegating here; each method here is named {@code
 * <Class>_<method>}.
 */
public final class Gl2 {
    private Gl2() {}

    // GL11: LWJGL 2 dropped the type suffix on buffer variants.

    public static void GL11_glFog(int pname, FloatBuffer params) {
        GL11.glFogfv(pname, params);
    }

    public static void GL11_glGetFloat(int pname, FloatBuffer params) {
        GL11.glGetFloatv(pname, params);
    }

    public static void GL11_glGetInteger(int pname, IntBuffer params) {
        GL11.glGetIntegerv(pname, params);
    }

    public static void GL11_glLight(int light, int pname, FloatBuffer params) {
        GL11.glLightfv(light, pname, params);
    }

    public static void GL11_glLightModel(int pname, FloatBuffer params) {
        GL11.glLightModelfv(pname, params);
    }

    public static void GL11_glMultMatrix(FloatBuffer m) {
        GL11.glMultMatrixf(m);
    }

    public static void GL11_glTexEnv(int target, int pname, FloatBuffer params) {
        GL11.glTexEnvfv(target, pname, params);
    }

    public static void GL11_glTexGen(int coord, int pname, FloatBuffer params) {
        GL11.glTexGenfv(coord, pname, params);
    }

    // GL20

    public static void GL20_glShaderSource(int shader, ByteBuffer source) {
        GL20.glShaderSource(shader, text(source));
    }

    public static void GL20_glUniform1(int location, FloatBuffer values) {
        GL20.glUniform1fv(location, values);
    }

    public static void GL20_glUniform1(int location, IntBuffer values) {
        GL20.glUniform1iv(location, values);
    }

    public static void GL20_glUniform2(int location, FloatBuffer values) {
        GL20.glUniform2fv(location, values);
    }

    public static void GL20_glUniform2(int location, IntBuffer values) {
        GL20.glUniform2iv(location, values);
    }

    public static void GL20_glUniform3(int location, FloatBuffer values) {
        GL20.glUniform3fv(location, values);
    }

    public static void GL20_glUniform3(int location, IntBuffer values) {
        GL20.glUniform3iv(location, values);
    }

    public static void GL20_glUniform4(int location, FloatBuffer values) {
        GL20.glUniform4fv(location, values);
    }

    public static void GL20_glUniform4(int location, IntBuffer values) {
        GL20.glUniform4iv(location, values);
    }

    public static void GL20_glUniformMatrix2(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix2fv(location, transpose, values);
    }

    public static void GL20_glUniformMatrix3(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix3fv(location, transpose, values);
    }

    public static void GL20_glUniformMatrix4(int location, boolean transpose, FloatBuffer values) {
        GL20.glUniformMatrix4fv(location, transpose, values);
    }

    // ARBShaderObjects

    public static void ARBShaderObjects_glGetObjectParameterARB(int object, int pname, IntBuffer params) {
        ARBShaderObjects.glGetObjectParameterivARB(object, pname, params);
    }

    public static void ARBShaderObjects_glShaderSourceARB(int shader, ByteBuffer source) {
        ARBShaderObjects.glShaderSourceARB(shader, text(source));
    }

    public static void ARBShaderObjects_glUniform1ARB(int location, FloatBuffer values) {
        ARBShaderObjects.glUniform1fvARB(location, values);
    }

    public static void ARBShaderObjects_glUniform1ARB(int location, IntBuffer values) {
        ARBShaderObjects.glUniform1ivARB(location, values);
    }

    public static void ARBShaderObjects_glUniform2ARB(int location, FloatBuffer values) {
        ARBShaderObjects.glUniform2fvARB(location, values);
    }

    public static void ARBShaderObjects_glUniform2ARB(int location, IntBuffer values) {
        ARBShaderObjects.glUniform2ivARB(location, values);
    }

    public static void ARBShaderObjects_glUniform3ARB(int location, FloatBuffer values) {
        ARBShaderObjects.glUniform3fvARB(location, values);
    }

    public static void ARBShaderObjects_glUniform3ARB(int location, IntBuffer values) {
        ARBShaderObjects.glUniform3ivARB(location, values);
    }

    public static void ARBShaderObjects_glUniform4ARB(int location, FloatBuffer values) {
        ARBShaderObjects.glUniform4fvARB(location, values);
    }

    public static void ARBShaderObjects_glUniform4ARB(int location, IntBuffer values) {
        ARBShaderObjects.glUniform4ivARB(location, values);
    }

    public static void ARBShaderObjects_glUniformMatrix2ARB(int location, boolean transpose, FloatBuffer values) {
        ARBShaderObjects.glUniformMatrix2fvARB(location, transpose, values);
    }

    public static void ARBShaderObjects_glUniformMatrix3ARB(int location, boolean transpose, FloatBuffer values) {
        ARBShaderObjects.glUniformMatrix3fvARB(location, transpose, values);
    }

    public static void ARBShaderObjects_glUniformMatrix4ARB(int location, boolean transpose, FloatBuffer values) {
        ARBShaderObjects.glUniformMatrix4fvARB(location, transpose, values);
    }

    // ARBDebugOutput

    private static GLDebugMessageARBCallback debugCallback;

    public static void ARBDebugOutput_glDebugMessageCallbackARB(ARBDebugOutputCallback callback) {
        GLDebugMessageARBCallback previous = debugCallback;
        if (callback == null) {
            debugCallback = null;
            ARBDebugOutput.glDebugMessageCallbackARB(null, MemoryUtil.NULL);
        } else {
            ARBDebugOutputCallback.Handler handler = callback.getHandler();
            debugCallback = GLDebugMessageARBCallback.create(
                    (source, type, id, severity, length, message, user) -> handler.handleMessage(
                            source, type, id, severity, GLDebugMessageARBCallback.getMessage(length, message)));
            ARBDebugOutput.glDebugMessageCallbackARB(debugCallback, MemoryUtil.NULL);
        }
        if (previous != null) {
            previous.free();
        }
    }

    /** Reads a shader source buffer, from its position to its limit, without moving it. */
    static String text(ByteBuffer source) {
        ByteBuffer copy = source.duplicate();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        int end = bytes.length;
        while (end > 0 && bytes[end - 1] == 0) {
            end--;
        }
        return new String(bytes, 0, end, StandardCharsets.UTF_8);
    }
}
