/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.util.concurrent.atomic.AtomicInteger;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;

/** With {@code -Dlazarus.gl.debug=true}, prints driver-reported GL errors with the Java stack that caused them. */
final class Debug {
    private static final int LIMIT = 50;
    private static final AtomicInteger printed = new AtomicInteger();

    private Debug() {}

    static void install() {
        if (!org.lwjgl.opengl.GL.getCapabilities().GL_KHR_debug) {
            System.err.println("[lazarus] lazarus.gl.debug: the driver has no KHR_debug");
            return;
        }
        GL43C.glEnable(KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS);
        GL43C.glDebugMessageCallback(
                (source, type, id, severity, length, message, user) -> {
                    if (type != KHRDebug.GL_DEBUG_TYPE_ERROR || printed.incrementAndGet() > LIMIT) {
                        return;
                    }
                    System.err.println("[lazarus] GL error: " + GLDebugMessageCallback.getMessage(length, message));
                    new Throwable("GL call site").printStackTrace();
                },
                0L);
    }
}
