/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.ByteBuffer;
import lol.trq.lazarus.lwjgl3.Input;
import lol.trq.lazarus.lwjgl3.Window;
import org.lwjgl.LWJGLException;
import org.lwjgl.glfw.GLFWVidMode;

/**
 * LWJGL 2's {@code Display}, on a GLFW window. Sizes are framebuffer pixels, so viewports are right on high-DPI
 * screens.
 */
public final class Display {
    private static DisplayMode mode = new DisplayMode(854, 480);
    private static boolean wasResized;

    private static final Drawable DRAWABLE = new Drawable() {
        @Override
        public boolean isCurrent() {
            return glfwGetCurrentContext() == Window.handle();
        }

        @Override
        public void makeCurrent() {
            glfwMakeContextCurrent(Window.handle());
            GL.setCapabilities(Window.capabilities());
        }

        @Override
        public void releaseContext() {
            if (isCurrent()) {
                glfwMakeContextCurrent(0L);
                GL.setCapabilities(null);
            }
        }

        @Override
        public void destroy() {
            Display.destroy();
        }
    };

    private Display() {}

    /** Creates the window with the default pixel format. */
    public static void create() throws LWJGLException {
        create(new PixelFormat());
    }

    /**
     * Creates the window.
     *
     * @param format the framebuffer format
     */
    public static void create(PixelFormat format) throws LWJGLException {
        create(format, new ContextAttribs());
    }

    /**
     * Creates the window with a context shared with another drawable. Sharing is ignored; the game has one window.
     *
     * @param format the framebuffer format
     * @param shared ignored
     */
    public static void create(PixelFormat format, Drawable shared) throws LWJGLException {
        create(format);
    }

    /**
     * Creates the window.
     *
     * @param format the framebuffer format
     * @param attribs the context attributes
     */
    public static void create(PixelFormat format, ContextAttribs attribs) throws LWJGLException {
        try {
            Window.create(format.depth, format.stencil, format.alpha, format.samples, attribs != null && attribs.debug);
        } catch (IllegalStateException e) {
            throw new LWJGLException(e.getMessage(), e);
        }
    }

    /** Destroys the window. */
    public static void destroy() {
        Window.destroy();
    }

    /** {@return whether the window exists} */
    public static boolean isCreated() {
        return Window.created();
    }

    /** Swaps buffers and processes window events. */
    public static void update() {
        update(true);
    }

    /**
     * Swaps buffers and, if asked, processes window events.
     *
     * @param processMessages whether to process events
     */
    public static void update(boolean processMessages) {
        if (!Window.created()) {
            return;
        }
        glfwSwapBuffers(Window.handle());
        if (processMessages) {
            Window.processMessages();
        }
        wasResized = Window.takeResized();
    }

    /** Processes window events. */
    public static void processMessages() {
        Window.processMessages();
    }

    /** Swaps buffers only. */
    public static void swapBuffers() {
        if (Window.created()) {
            glfwSwapBuffers(Window.handle());
        }
    }

    /**
     * Sleeps to cap the frame rate.
     *
     * @param fps the cap
     */
    public static void sync(int fps) {
        Window.sync(fps);
    }

    /** {@return whether the user asked to close the window} */
    public static boolean isCloseRequested() {
        return Window.closeRequested();
    }

    /** {@return whether the window has focus} */
    public static boolean isActive() {
        return Window.focused();
    }

    /** {@return whether the window is not minimized} */
    public static boolean isVisible() {
        return Window.visible();
    }

    /** {@return whether the window content may need repainting; always true} */
    public static boolean isDirty() {
        return true;
    }

    /** {@return whether the window size changed in the last update} */
    public static boolean wasResized() {
        return wasResized;
    }

    /** {@return the drawable width in pixels} */
    public static int getWidth() {
        return Window.framebufferWidth();
    }

    /** {@return the drawable height in pixels} */
    public static int getHeight() {
        return Window.framebufferHeight();
    }

    /** {@return the window's x position} */
    public static int getX() {
        int[] x = new int[1];
        int[] y = new int[1];
        if (Window.created()) {
            glfwGetWindowPos(Window.handle(), x, y);
        }
        return x[0];
    }

    /** {@return the window's y position} */
    public static int getY() {
        int[] x = new int[1];
        int[] y = new int[1];
        if (Window.created()) {
            glfwGetWindowPos(Window.handle(), x, y);
        }
        return y[0];
    }

    /**
     * Sets the title.
     *
     * @param title the title
     */
    public static void setTitle(String title) {
        Window.setTitle(title);
    }

    /** {@return the title is not readable through GLFW; returns an empty string} */
    public static String getTitle() {
        return "";
    }

    /**
     * Sets the icons.
     *
     * @param icons square RGBA images
     * @return how many were accepted
     */
    public static int setIcon(ByteBuffer[] icons) {
        return Window.setIcons(icons);
    }

    /**
     * Sets whether the window can be resized.
     *
     * @param resizable whether it can
     */
    public static void setResizable(boolean resizable) {
        Window.setResizable(resizable);
    }

    /**
     * Turns vertical sync on or off.
     *
     * @param sync whether to sync
     */
    public static void setVSyncEnabled(boolean sync) {
        Window.setVSync(sync);
    }

    /**
     * Switches between full screen and windowed.
     *
     * @param fullscreen whether to go full screen
     */
    public static void setFullscreen(boolean fullscreen) {
        Window.setFullscreen(fullscreen);
    }

    /**
     * Switches to full screen at a mode's resolution.
     *
     * @param newMode the mode
     */
    public static void setDisplayModeAndFullscreen(DisplayMode newMode) {
        setDisplayMode(newMode);
        setFullscreen(true);
    }

    /** {@return whether the window is full screen} */
    public static boolean isFullscreen() {
        return Window.fullscreen();
    }

    /**
     * Sets the window size, or the full screen resolution.
     *
     * @param newMode the mode
     */
    public static void setDisplayMode(DisplayMode newMode) {
        mode = newMode;
        Window.setMode(newMode.getWidth(), newMode.getHeight(), newMode.getFrequency());
    }

    /** {@return the current mode} */
    public static DisplayMode getDisplayMode() {
        return mode;
    }

    /** {@return the primary monitor's current mode} */
    public static DisplayMode getDesktopDisplayMode() {
        Window.init();
        GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vm == null) {
            return mode;
        }
        return new DisplayMode(
                vm.width(), vm.height(), vm.redBits() + vm.greenBits() + vm.blueBits(), vm.refreshRate(), true);
    }

    /** {@return the primary monitor's supported modes} */
    public static DisplayMode[] getAvailableDisplayModes() {
        Window.init();
        GLFWVidMode.Buffer modes = glfwGetVideoModes(glfwGetPrimaryMonitor());
        if (modes == null) {
            return new DisplayMode[] {getDesktopDisplayMode()};
        }
        DisplayMode[] out = new DisplayMode[modes.limit()];
        for (int i = 0; i < out.length; i++) {
            GLFWVidMode vm = modes.get(i);
            out[i] = new DisplayMode(
                    vm.width(), vm.height(), vm.redBits() + vm.greenBits() + vm.blueBits(), vm.refreshRate(), true);
        }
        return out;
    }

    /** {@return the window's drawable} */
    public static Drawable getDrawable() {
        return DRAWABLE;
    }

    /**
     * Moves the window.
     *
     * @param x the x position
     * @param y the y position
     */
    public static void setLocation(int x, int y) {
        if (Window.created() && x >= 0 && y >= 0) {
            glfwSetWindowPos(Window.handle(), x, y);
        }
    }

    /** {@return the adapter name; not available through GLFW} */
    public static String getAdapter() {
        return null;
    }

    /** {@return the driver version; not available through GLFW} */
    public static String getVersion() {
        return null;
    }

    /** {@return the GLFW window handle, for code that wants LWJGL 3 directly} */
    public static long getWindow() {
        return Window.handle();
    }

    static void resetInput() {
        Input.clear();
    }
}
