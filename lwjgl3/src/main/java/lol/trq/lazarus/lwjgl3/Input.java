/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Collects GLFW input callbacks into LWJGL 2-style event queues and state. Filled during {@code glfwPollEvents} on the
 * main thread, read by {@code Keyboard} and {@code Mouse}.
 */
public final class Input {
    /** One keyboard event, LWJGL 2 style: a key code, a state, and the character it typed. */
    public static final class KeyEvent {
        public final int key;
        public final boolean down;
        public final boolean repeat;
        public final long nanos;
        public char character;

        KeyEvent(int key, boolean down, boolean repeat, char character, long nanos) {
            this.key = key;
            this.down = down;
            this.repeat = repeat;
            this.character = character;
            this.nanos = nanos;
        }
    }

    /** One mouse event, LWJGL 2 style. {@code button} is -1 for movement and wheel events. */
    public record MouseEvent(int button, boolean down, int x, int y, int dx, int dy, int dwheel, long nanos) {}

    private static final Deque<KeyEvent> KEYS = new ArrayDeque<>();
    private static final Deque<MouseEvent> MOUSE = new ArrayDeque<>();
    private static final boolean[] KEY_DOWN = new boolean[256];
    private static final boolean[] BUTTON_DOWN = new boolean[16];
    private static volatile boolean repeatEvents;

    private static double lastCursorX = Double.NaN;
    private static double lastCursorY = Double.NaN;
    private static int mouseX;
    private static int mouseY;
    private static int accumDX;
    private static int accumDY;
    private static int accumWheel;
    private static boolean insideWindow;

    private Input() {}

    /**
     * Registers the input callbacks on a window.
     *
     * @param window the GLFW window
     */
    public static void install(long window) {
        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> onKey(key, action));
        glfwSetCharCallback(window, (w, codepoint) -> onChar(codepoint));
        glfwSetMouseButtonCallback(window, (w, button, action, mods) -> onButton(button, action == GLFW_PRESS));
        glfwSetCursorPosCallback(window, (w, x, y) -> onCursor(w, x, y));
        glfwSetScrollCallback(window, (w, dx, dy) -> onScroll(dy));
        glfwSetCursorEnterCallback(window, (w, entered) -> insideWindow = entered);
    }

    static synchronized void onKey(int glfwKey, int action) {
        int key = KeyCodes.toLwjgl2(glfwKey);
        boolean down = action != GLFW_RELEASE;
        boolean repeat = action == GLFW_REPEAT;
        if (key > 0) {
            KEY_DOWN[key] = down;
        }
        if (repeat && !repeatEvents) {
            return;
        }
        KEYS.add(new KeyEvent(key, down, repeat, '\0', System.nanoTime()));
    }

    /** Attaches a typed character to the key press that produced it, as LWJGL 2 reported them together. */
    static synchronized void onChar(int codepoint) {
        char[] chars = Character.toChars(codepoint);
        KeyEvent last = KEYS.peekLast();
        int start = 0;
        if (last != null && last.down && last.character == '\0' && last.key != 0) {
            last.character = chars[0];
            start = 1;
        }
        for (int i = start; i < chars.length; i++) {
            KEYS.add(new KeyEvent(0, true, false, chars[i], System.nanoTime()));
        }
    }

    static synchronized void onButton(int button, boolean down) {
        if (button >= 0 && button < BUTTON_DOWN.length) {
            BUTTON_DOWN[button] = down;
        }
        MOUSE.add(new MouseEvent(button, down, mouseX, mouseY, 0, 0, 0, System.nanoTime()));
    }

    private static synchronized void onCursor(long window, double x, double y) {
        float scale = Window.contentScaleX();
        int fbHeight = Window.framebufferHeight();
        int newX = (int) Math.round(x * scale);
        int newY = fbHeight - 1 - (int) Math.round(y * Window.contentScaleY());
        int dx = 0;
        int dy = 0;
        if (!Double.isNaN(lastCursorX)) {
            dx = (int) Math.round((x - lastCursorX) * scale);
            dy = -(int) Math.round((y - lastCursorY) * Window.contentScaleY());
        }
        lastCursorX = x;
        lastCursorY = y;
        mouseX = newX;
        mouseY = newY;
        accumDX += dx;
        accumDY += dy;
        MOUSE.add(new MouseEvent(-1, false, newX, newY, dx, dy, 0, System.nanoTime()));
    }

    static synchronized void onScroll(double dy) {
        int wheel = (int) Math.round(dy * 120);
        accumWheel += wheel;
        MOUSE.add(new MouseEvent(-1, false, mouseX, mouseY, 0, 0, wheel, System.nanoTime()));
    }

    /** {@return the next keyboard event, or {@code null}} */
    public static synchronized KeyEvent nextKey() {
        return KEYS.poll();
    }

    /** {@return the next mouse event, or {@code null}} */
    public static synchronized MouseEvent nextMouse() {
        return MOUSE.poll();
    }

    /**
     * {@return whether an LWJGL 2 key is down}
     *
     * @param key the LWJGL 2 key code
     */
    public static synchronized boolean isKeyDown(int key) {
        return key > 0 && key < KEY_DOWN.length && KEY_DOWN[key];
    }

    /**
     * {@return whether a mouse button is down}
     *
     * @param button the button, 0 left, 1 right, 2 middle
     */
    public static synchronized boolean isButtonDown(int button) {
        return button >= 0 && button < BUTTON_DOWN.length && BUTTON_DOWN[button];
    }

    /**
     * Turns key repeat events on or off.
     *
     * @param enabled whether repeats are queued
     */
    public static void setRepeatEvents(boolean enabled) {
        repeatEvents = enabled;
    }

    /** {@return whether key repeat events are queued} */
    public static boolean repeatEvents() {
        return repeatEvents;
    }

    /** {@return the cursor x in framebuffer pixels} */
    public static synchronized int mouseX() {
        return mouseX;
    }

    /** {@return the cursor y in framebuffer pixels, from the bottom} */
    public static synchronized int mouseY() {
        return mouseY;
    }

    /** {@return the x movement since the last call} */
    public static synchronized int takeDX() {
        int v = accumDX;
        accumDX = 0;
        return v;
    }

    /** {@return the y movement since the last call, upwards positive} */
    public static synchronized int takeDY() {
        int v = accumDY;
        accumDY = 0;
        return v;
    }

    /** {@return the wheel movement since the last call, 120 per notch} */
    public static synchronized int takeWheel() {
        int v = accumWheel;
        accumWheel = 0;
        return v;
    }

    /** {@return whether the cursor is over the window} */
    public static boolean insideWindow() {
        return insideWindow;
    }

    /** Forgets the last cursor position so the next movement does not produce a jump. */
    public static synchronized void resetCursorDelta() {
        lastCursorX = Double.NaN;
        lastCursorY = Double.NaN;
        accumDX = 0;
        accumDY = 0;
    }

    /** Clears all queued events and held keys, as when the window loses focus. */
    public static synchronized void clear() {
        KEYS.clear();
        MOUSE.clear();
        java.util.Arrays.fill(KEY_DOWN, false);
        java.util.Arrays.fill(BUTTON_DOWN, false);
    }
}
