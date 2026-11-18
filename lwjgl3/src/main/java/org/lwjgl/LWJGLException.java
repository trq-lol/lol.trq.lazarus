/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl;

/** LWJGL 2's checked exception, which LWJGL 3 removed. */
public class LWJGLException extends Exception {
    private static final long serialVersionUID = 1L;

    /** Creates an exception without a message. */
    public LWJGLException() {}

    /**
     * Creates an exception.
     *
     * @param message the message
     */
    public LWJGLException(String message) {
        super(message);
    }

    /**
     * Creates an exception.
     *
     * @param message the message
     * @param cause the cause
     */
    public LWJGLException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception.
     *
     * @param cause the cause
     */
    public LWJGLException(Throwable cause) {
        super(cause);
    }
}
