/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

import java.util.Map;
import java.util.WeakHashMap;

/** LWJGL 2's {@code GLContext}: the current context's capabilities. */
public final class GLContext {
    private static final Map<GLCapabilities, ContextCapabilities> CACHE = new WeakHashMap<>();

    private GLContext() {}

    /** {@return the capabilities of the context current on the calling thread} */
    public static ContextCapabilities getCapabilities() {
        GLCapabilities caps = GL.getCapabilities();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(caps, ContextCapabilities::new);
        }
    }

    /**
     * {@return the address of an OpenGL function in the current context, or 0}
     *
     * @param name the function name
     */
    public static long getFunctionAddress(String name) {
        return GL.getFunctionProvider().getFunctionAddress(name);
    }
}
