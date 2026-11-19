/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * OpenGL queries that are safe without a current context. LWJGL 2 returned {@code null} from {@code glGetString}
 * when no context was current; LWJGL 3 calls through a missing function pointer and the JVM aborts. Minecraft's crash
 * report asks for the GL vendor, renderer and version even when the window never opened, which would hide the real
 * error. Lazarus's agent sends every {@code GL11.glGetString(int)} call here.
 */
public final class NoContextGl {
    private NoContextGl() {}

    /** {@return whether this thread has OpenGL capabilities, that is, a current context LWJGL has loaded} */
    public static boolean hasContext() {
        try {
            return GL.getCapabilities() != null;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * {@return the string, or {@code null} with no current context}
     *
     * @param name the string to query, such as {@code GL_VENDOR}
     */
    public static String glGetString(int name) {
        return query(name, NoContextGl::hasContext, GL11::glGetString);
    }

    static String query(int name, BooleanSupplier context, IntFunction<String> gl) {
        return context.getAsBoolean() ? gl.apply(name) : null;
    }
}
