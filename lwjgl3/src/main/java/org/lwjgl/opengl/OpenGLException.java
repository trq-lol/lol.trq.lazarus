/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/** LWJGL 2's unchecked OpenGL error, which LWJGL 3 removed. */
public class OpenGLException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /** Creates an exception without a message. */
    public OpenGLException() {}

    /**
     * Creates an exception for an OpenGL error code.
     *
     * @param errorCode the {@code glGetError} value
     */
    public OpenGLException(int errorCode) {
        super("OpenGL error: 0x" + Integer.toHexString(errorCode));
    }

    /**
     * Creates an exception.
     *
     * @param message the message
     */
    public OpenGLException(String message) {
        super(message);
    }

    /**
     * Creates an exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public OpenGLException(String message, Throwable cause) {
        super(message, cause);
    }
}
