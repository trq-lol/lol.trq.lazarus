/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

/**
 * Read-only access to the core renderer's tracked OpenGL state, for render libraries that draw alongside the game.
 * Every answer comes from the emulator, never from a driver query, which stalls the pipeline on some drivers.
 *
 * <p>Only valid on the render thread while the core renderer is on ({@link #active()}). Binding getters return -1
 * when the state is unknown, for example right after a context switch; query the driver in that case.
 */
public final class GlState {
    private GlState() {}

    /** {@return whether the core renderer is on} */
    public static boolean active() {
        return lol.trq.lazarus.lwjgl3.Window.CORE;
    }

    /**
     * Copies the current modelview matrix, column-major, into {@code out}.
     *
     * @param out at least 16 floats
     */
    public static void modelView(float[] out) {
        System.arraycopy(CoreGl.modelViewMatrix(), 0, out, 0, 16);
    }

    /**
     * Copies the current projection matrix, column-major, into {@code out}.
     *
     * @param out at least 16 floats
     */
    public static void projection(float[] out) {
        System.arraycopy(CoreGl.projectionMatrix(), 0, out, 0, 16);
    }

    /** {@return the bound program, or -1 if unknown} */
    public static int program() {
        return CoreGl.trackedProgram();
    }

    /** {@return the bound vertex array, or -1 if unknown} */
    public static int vertexArray() {
        return CoreGl.trackedVertexArray();
    }

    /** {@return the array buffer the game last bound, or -1 if unknown} */
    public static int arrayBuffer() {
        return CoreGl.trackedArrayBuffer();
    }

    /**
     * Copies the viewport into {@code out}.
     *
     * @param out at least 4 ints: x, y, width, height
     * @return false, leaving {@code out} untouched, if the viewport is not known yet
     */
    public static boolean viewport(int[] out) {
        return CoreGl.trackedViewport(out);
    }
}
