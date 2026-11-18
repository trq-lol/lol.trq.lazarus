/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.input;

import static org.lwjgl.glfw.GLFW.*;

import lol.trq.lazarus.lwjgl3.Input;
import lol.trq.lazarus.lwjgl3.Window;
import org.lwjgl.LWJGLException;

/**
 * LWJGL 2's {@code Mouse}, fed by GLFW. Positions are framebuffer pixels with y from the bottom, wheel steps are 120
 * per notch, and a grabbed cursor uses raw motion where the platform supports it.
 */
public final class Mouse {
    private static boolean created;
    private static boolean grabbed;
    private static Input.MouseEvent current;

    private Mouse() {}

    /** Creates the mouse. The window must exist. */
    public static void create() throws LWJGLException {
        if (!Window.created()) {
            throw new IllegalStateException("Display must be created");
        }
        created = true;
    }

    /** {@return whether the mouse was created; true once the window exists} */
    public static boolean isCreated() {
        return created || Window.created();
    }

    /** Destroys the mouse. */
    public static void destroy() {
        created = false;
    }

    /** Processes pending window events. */
    public static void poll() {
        Window.processMessages();
    }

    /**
     * Moves to the next event.
     *
     * @return whether there was one
     */
    public static boolean next() {
        current = Input.nextMouse();
        return current != null;
    }

    /** {@return the current event's button, or -1 for movement and wheel events} */
    public static int getEventButton() {
        return current == null ? -1 : current.button();
    }

    /** {@return whether the current event is a button press} */
    public static boolean getEventButtonState() {
        return current != null && current.down();
    }

    /** {@return the current event's x movement} */
    public static int getEventDX() {
        return current == null ? 0 : current.dx();
    }

    /** {@return the current event's y movement} */
    public static int getEventDY() {
        return current == null ? 0 : current.dy();
    }

    /** {@return the cursor x at the current event} */
    public static int getEventX() {
        return current == null ? 0 : current.x();
    }

    /** {@return the cursor y at the current event} */
    public static int getEventY() {
        return current == null ? 0 : current.y();
    }

    /** {@return the current event's wheel movement} */
    public static int getEventDWheel() {
        return current == null ? 0 : current.dwheel();
    }

    /** {@return the current event's time in nanoseconds} */
    public static long getEventNanoseconds() {
        return current == null ? 0 : current.nanos();
    }

    /** {@return the cursor x in framebuffer pixels} */
    public static int getX() {
        return Input.mouseX();
    }

    /** {@return the cursor y in framebuffer pixels, from the bottom} */
    public static int getY() {
        return Input.mouseY();
    }

    /** {@return the x movement since the last call} */
    public static int getDX() {
        return Input.takeDX();
    }

    /** {@return the y movement since the last call} */
    public static int getDY() {
        return Input.takeDY();
    }

    /** {@return the wheel movement since the last call} */
    public static int getDWheel() {
        return Input.takeWheel();
    }

    /**
     * {@return whether a button is held}
     *
     * @param button 0 left, 1 right, 2 middle
     */
    public static boolean isButtonDown(int button) {
        return Input.isButtonDown(button);
    }

    /** {@return the button count} */
    public static int getButtonCount() {
        return 8;
    }

    /**
     * {@return a button's name}
     *
     * @param button the button
     */
    public static String getButtonName(int button) {
        return button >= 0 && button < 8 ? "BUTTON" + button : null;
    }

    /**
     * {@return a button's index}
     *
     * @param name the name, such as {@code "BUTTON0"}
     */
    public static int getButtonIndex(String name) {
        return name != null && name.startsWith("BUTTON") ? Integer.parseInt(name.substring(6)) : -1;
    }

    /** {@return whether a wheel is present; assumed} */
    public static boolean hasWheel() {
        return true;
    }

    /** {@return whether the cursor is over the window} */
    public static boolean isInsideWindow() {
        return Input.insideWindow();
    }

    /** {@return whether the cursor is grabbed} */
    public static boolean isGrabbed() {
        return grabbed;
    }

    /**
     * Grabs or releases the cursor. A grabbed cursor is hidden and reports unbounded, unaccelerated movement.
     *
     * @param grab whether to grab
     */
    public static void setGrabbed(boolean grab) {
        grabbed = grab;
        long window = Window.handle();
        if (window == 0) {
            return;
        }
        glfwSetInputMode(window, GLFW_CURSOR, grab ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, grab ? GLFW_TRUE : GLFW_FALSE);
        }
        Input.resetCursorDelta();
    }

    /**
     * Moves the cursor.
     *
     * @param x x in framebuffer pixels
     * @param y y in framebuffer pixels, from the bottom
     */
    public static void setCursorPosition(int x, int y) {
        long window = Window.handle();
        if (window == 0) {
            return;
        }
        double wx = x / Window.contentScaleX();
        double wy = (Window.framebufferHeight() - 1 - y) / Window.contentScaleY();
        glfwSetCursorPos(window, wx, wy);
        Input.resetCursorDelta();
    }

    /** Clears queued events and the movement accumulators. */
    public static void clearState() {
        Input.takeDX();
        Input.takeDY();
        Input.takeWheel();
    }
}
