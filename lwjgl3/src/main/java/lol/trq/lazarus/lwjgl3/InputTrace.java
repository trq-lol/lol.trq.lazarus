/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

/**
 * Input diagnostics, on with {@code -Dlazarus.debug.input=true}. Logs grab and focus changes as they happen, with the
 * caller, and once a second a summary of cursor events, raw movement and the movement the game read.
 */
public final class InputTrace {
    static final boolean ON = Boolean.getBoolean("lazarus.debug.input");

    private static long windowStart;
    private static int cursorEvents;
    private static long rawDX;
    private static long rawDY;
    private static long readDX;
    private static long readDY;
    private static int reads;
    private static int grabCalls;
    private static int cursorWarps;

    private InputTrace() {}

    static synchronized void cursor(int dx, int dy) {
        if (ON) {
            cursorEvents++;
            rawDX += dx;
            rawDY += dy;
        }
    }

    static synchronized void readX(int dx) {
        if (ON) {
            readDX += dx;
            reads++;
        }
    }

    static synchronized void readY(int dy) {
        if (ON) {
            readDY += dy;
        }
    }

    /**
     * Logs a grab change.
     *
     * @param grab the new state
     */
    public static synchronized void grab(boolean grab) {
        if (ON) {
            grabCalls++;
            log("setGrabbed(" + grab + ") from " + caller() + " focused=" + Window.focused());
        }
    }

    /**
     * Logs a cursor warp.
     *
     * @param x x in framebuffer pixels
     * @param y y in framebuffer pixels
     */
    public static synchronized void warp(int x, int y) {
        if (ON) {
            cursorWarps++;
            log("setCursorPosition(" + x + ", " + y + ") from " + caller());
        }
    }

    static synchronized void focus(boolean focused) {
        if (ON) {
            log("focus " + focused);
        }
    }

    /** Logs the last second's summary; called from the main thread's event pump. */
    static synchronized void tick() {
        if (!ON) {
            return;
        }
        long now = System.nanoTime();
        if (windowStart == 0) {
            windowStart = now;
            return;
        }
        if (now - windowStart < 1_000_000_000L) {
            return;
        }
        log("1s: cursorEvents=" + cursorEvents + " raw=(" + rawDX + "," + rawDY + ") read=(" + readDX + "," + readDY
                + ") reads=" + reads + " grabCalls=" + grabCalls + " warps=" + cursorWarps + " focused="
                + Window.focused() + " scale=" + Window.contentScaleX());
        windowStart = now;
        cursorEvents = 0;
        rawDX = rawDY = readDX = readDY = 0;
        reads = grabCalls = cursorWarps = 0;
    }

    private static String caller() {
        for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
            String c = e.getClassName();
            if (!c.startsWith("lol.trq.lazarus.lwjgl3")
                    && !c.startsWith("org.lwjgl.")
                    && !c.startsWith("java.lang.Thread")) {
                return c + "." + e.getMethodName() + ":" + e.getLineNumber();
            }
        }
        return "?";
    }

    private static void log(String message) {
        System.err.println("[Lazarus input] " + message);
    }
}
