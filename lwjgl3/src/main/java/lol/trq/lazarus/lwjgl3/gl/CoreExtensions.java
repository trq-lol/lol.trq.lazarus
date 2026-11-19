/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

/**
 * The ARB and EXT extension calls of Minecraft, Forge and OptiFine, on their core equivalents. A core profile need not
 * expose {@code ARB_shader_objects}, {@code ARB_vertex_shader} or {@code EXT_framebuffer_object} (Apple's does not),
 * so the agent redirects these calls here, with the same names and descriptors, while the core renderer is on. The
 * enum values of these extensions equal their core counterparts.
 */
public final class CoreExtensions {
    private CoreExtensions() {}

    // ---- ARB_shader_objects (handles are shader and program names in core)

    public static int glCreateProgramObjectARB() {
        CoreGl.flush();
        return GL20C.glCreateProgram();
    }

    public static void glAttachObjectARB(int program, int shader) {
        CoreGl.flush();
        GL20C.glAttachShader(program, shader);
    }

    public static void glDetachObjectARB(int program, int shader) {
        CoreGl.flush();
        GL20C.glDetachShader(program, shader);
    }

    public static void glDeleteObjectARB(int object) {
        CoreGl.flush();
        if (GL20C.glIsProgram(object)) {
            GL20C.glDeleteProgram(object);
        } else if (GL20C.glIsShader(object)) {
            GL20C.glDeleteShader(object);
        }
    }

    public static void glValidateProgramARB(int program) {
        CoreGl.flush();
        GL20C.glValidateProgram(program);
    }

    public static int glGetObjectParameteriARB(int object, int pname) {
        CoreGl.flush();
        return GL20C.glIsProgram(object) ? GL20C.glGetProgrami(object, pname) : GL20C.glGetShaderi(object, pname);
    }

    public static void glGetObjectParameterARB(int object, int pname, IntBuffer params) {
        params.put(params.position(), glGetObjectParameteriARB(object, pname));
    }

    public static String glGetInfoLogARB(int object, int maxLength) {
        CoreGl.flush();
        return GL20C.glIsProgram(object)
                ? GL20C.glGetProgramInfoLog(object, maxLength)
                : GL20C.glGetShaderInfoLog(object, maxLength);
    }

    public static void glGetInfoLogARB(int object, IntBuffer length, ByteBuffer infoLog) {
        String log = glGetInfoLogARB(object, Math.max(1, infoLog.remaining()));
        byte[] bytes = log.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int n = Math.min(bytes.length, Math.max(0, infoLog.remaining() - 1));
        infoLog.put(infoLog.position(), bytes, 0, n);
        if (infoLog.remaining() > n) {
            infoLog.put(infoLog.position() + n, (byte) 0);
        }
        if (length != null) {
            length.put(length.position(), n);
        }
    }

    public static void glUniform1fARB(int location, float x) {
        CoreGl.flush();
        GL20C.glUniform1f(location, x);
    }

    public static void glUniform2fARB(int location, float x, float y) {
        CoreGl.flush();
        GL20C.glUniform2f(location, x, y);
    }

    public static void glUniform3fARB(int location, float x, float y, float z) {
        CoreGl.flush();
        GL20C.glUniform3f(location, x, y, z);
    }

    public static void glUniform4fARB(int location, float x, float y, float z, float w) {
        CoreGl.flush();
        GL20C.glUniform4f(location, x, y, z, w);
    }

    public static void glUniform1iARB(int location, int x) {
        CoreGl.flush();
        GL20C.glUniform1i(location, x);
    }

    public static void glUniform2iARB(int location, int x, int y) {
        CoreGl.flush();
        GL20C.glUniform2i(location, x, y);
    }

    public static void glUniform4iARB(int location, int x, int y, int z, int w) {
        CoreGl.flush();
        GL20C.glUniform4i(location, x, y, z, w);
    }

    public static void glUniform1ARB(int location, FloatBuffer v) {
        CoreGl.flush();
        GL20C.glUniform1fv(location, v);
    }

    public static void glUniform2ARB(int location, FloatBuffer v) {
        CoreGl.flush();
        GL20C.glUniform2fv(location, v);
    }

    public static void glUniform3ARB(int location, FloatBuffer v) {
        CoreGl.flush();
        GL20C.glUniform3fv(location, v);
    }

    public static void glUniform4ARB(int location, FloatBuffer v) {
        CoreGl.flush();
        GL20C.glUniform4fv(location, v);
    }

    public static void glUniform1ARB(int location, IntBuffer v) {
        CoreGl.flush();
        GL20C.glUniform1iv(location, v);
    }

    public static void glUniform2ARB(int location, IntBuffer v) {
        CoreGl.flush();
        GL20C.glUniform2iv(location, v);
    }

    public static void glUniform3ARB(int location, IntBuffer v) {
        CoreGl.flush();
        GL20C.glUniform3iv(location, v);
    }

    public static void glUniform4ARB(int location, IntBuffer v) {
        CoreGl.flush();
        GL20C.glUniform4iv(location, v);
    }

    public static void glUniformMatrix2ARB(int location, boolean transpose, FloatBuffer m) {
        CoreGl.flush();
        GL20C.glUniformMatrix2fv(location, transpose, m);
    }

    public static void glUniformMatrix3ARB(int location, boolean transpose, FloatBuffer m) {
        CoreGl.flush();
        GL20C.glUniformMatrix3fv(location, transpose, m);
    }

    public static void glUniformMatrix4ARB(int location, boolean transpose, FloatBuffer m) {
        CoreGl.flush();
        GL20C.glUniformMatrix4fv(location, transpose, m);
    }

    // ---- ARB_vertex_shader

    public static void glBindAttribLocationARB(int program, int index, CharSequence name) {
        CoreGl.flush();
        GL20C.glBindAttribLocation(program, index, name);
    }

    public static int glGetAttribLocationARB(int program, CharSequence name) {
        CoreGl.flush();
        return GL20C.glGetAttribLocation(program, name);
    }

    // ---- ARB_geometry_shader4: core has no program-level geometry parameters, and OptiFine sets them only for
    // geometry shaders, which a 4.1 core program declares in GLSL instead.

    public static void glProgramParameteriARB(int program, int pname, int value) {
        CoreGl.flush();
    }

    // ---- ARB_vertex_buffer_object

    public static int glGenBuffersARB() {
        CoreGl.flush();
        return GL15C.glGenBuffers();
    }

    public static void glDeleteBuffersARB(int buffer) {
        CoreGl.flush();
        GL15C.glDeleteBuffers(buffer);
    }

    public static void glBufferDataARB(int target, ByteBuffer data, int usage) {
        CoreGl.flush();
        GL15C.glBufferData(target, data, usage);
    }

    // ---- EXT_blend_func_separate

    public static void glBlendFuncSeparateEXT(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        CoreGl.flush();
        GL14C.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    // ---- EXT_framebuffer_object

    public static int glGenFramebuffersEXT() {
        CoreGl.flush();
        return GL30C.glGenFramebuffers();
    }

    public static void glBindFramebufferEXT(int target, int framebuffer) {
        CoreGl.flush();
        GL30C.glBindFramebuffer(target, framebuffer);
    }

    public static void glDeleteFramebuffersEXT(int framebuffer) {
        CoreGl.flush();
        GL30C.glDeleteFramebuffers(framebuffer);
    }

    public static int glCheckFramebufferStatusEXT(int target) {
        CoreGl.flush();
        return GL30C.glCheckFramebufferStatus(target);
    }

    public static void glFramebufferTexture2DEXT(int target, int attachment, int texTarget, int texture, int level) {
        CoreGl.flush();
        GL30C.glFramebufferTexture2D(target, attachment, texTarget, texture, level);
    }

    public static void glFramebufferRenderbufferEXT(int target, int attachment, int rbTarget, int renderbuffer) {
        CoreGl.flush();
        GL30C.glFramebufferRenderbuffer(target, attachment, rbTarget, renderbuffer);
    }

    public static int glGenRenderbuffersEXT() {
        CoreGl.flush();
        return GL30C.glGenRenderbuffers();
    }

    public static void glBindRenderbufferEXT(int target, int renderbuffer) {
        CoreGl.flush();
        GL30C.glBindRenderbuffer(target, renderbuffer);
    }

    public static void glDeleteRenderbuffersEXT(int renderbuffer) {
        CoreGl.flush();
        GL30C.glDeleteRenderbuffers(renderbuffer);
    }

    public static void glRenderbufferStorageEXT(int target, int format, int width, int height) {
        CoreGl.flush();
        GL30C.glRenderbufferStorage(target, format, width, height);
    }
}
