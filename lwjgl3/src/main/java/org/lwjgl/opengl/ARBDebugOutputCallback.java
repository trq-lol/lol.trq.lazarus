/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/**
 * LWJGL 2's holder for an {@code ARB_debug_output} handler. Pass it to {@code
 * ARBDebugOutput.glDebugMessageCallbackARB}; Lazarus installs the handler through LWJGL 3.
 */
public final class ARBDebugOutputCallback {
    final Handler handler;

    /** Creates a callback that logs every message to standard error. */
    public ARBDebugOutputCallback() {
        this((source, type, id, severity, message) -> System.err.println(
                "[GL debug] source=0x" + Integer.toHexString(source) + " type=0x" + Integer.toHexString(type) + " id="
                        + id + " severity=0x" + Integer.toHexString(severity) + ": " + message));
    }

    /**
     * Creates a callback.
     *
     * @param handler receives each message
     */
    public ARBDebugOutputCallback(Handler handler) {
        this.handler = handler;
    }

    /** {@return the handler} */
    public Handler getHandler() {
        return handler;
    }

    /** Receives OpenGL debug messages. */
    public interface Handler {
        /**
         * Handles one message.
         *
         * @param source the source enum
         * @param type the type enum
         * @param id the message id
         * @param severity the severity enum
         * @param message the text
         */
        void handleMessage(int source, int type, int id, int severity, String message);
    }
}
