/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/** LWJGL 2's GL error helpers, used by lwjgl_util's GLU and by Forge's splash screen. */
public final class Util {
    private Util() {}

    /**
     * Throws when the GL error flag is set.
     *
     * @throws OpenGLException if {@code glGetError} reports an error
     */
    public static void checkGLError() throws OpenGLException {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            throw new OpenGLException(translateGLErrorString(error) + " (0x" + Integer.toHexString(error) + ")");
        }
    }

    /**
     * {@return a readable name for a GL error code}
     *
     * @param error the {@code glGetError} value
     */
    public static String translateGLErrorString(int error) {
        return switch (error) {
            case GL11.GL_NO_ERROR -> "No error";
            case GL11.GL_INVALID_ENUM -> "Invalid enum";
            case GL11.GL_INVALID_VALUE -> "Invalid value";
            case GL11.GL_INVALID_OPERATION -> "Invalid operation";
            case GL11.GL_STACK_OVERFLOW -> "Stack overflow";
            case GL11.GL_STACK_UNDERFLOW -> "Stack underflow";
            case GL11.GL_OUT_OF_MEMORY -> "Out of memory";
            case 0x0506 -> "Invalid framebuffer operation";
            default -> null;
        };
    }
}
