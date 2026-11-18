/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

/**
 * The single game window, on GLFW. Holds everything LWJGL 2's static {@code Display} kept: the window, its OpenGL
 * context, the display mode, full screen state and the frame limiter.
 *
 * <p>GLFW requires window creation and event polling on the thread that initialized it (on macOS, the process's first
 * thread). Buffer swaps and context switches work from any thread, which Forge's splash screen relies on.
 */
public final class Window {
    private static long handle = NULL;
    private static Thread mainThread;
    private static GLCapabilities capabilities;
    private static boolean initialized;

    private static String title = "Game";
    private static int width = 854;
    private static int height = 480;
    private static int refreshRate;
    private static boolean fullscreen;
    private static boolean resizable;
    private static boolean vsync;
    private static List<ByteBuffer> icons = List.of();

    private static int framebufferWidth;
    private static int framebufferHeight;
    private static float scaleX = 1f;
    private static float scaleY = 1f;
    private static boolean resized;
    private static boolean focused = true;
    private static boolean iconified;

    private static long nextFrame;

    private Window() {}

    /** Initializes GLFW once, on the calling thread. */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW could not be initialized");
        }
        initialized = true;
    }

    /**
     * Creates the window and makes its context current.
     *
     * @param depth depth bits
     * @param stencil stencil bits
     * @param alpha alpha bits
     * @param samples multisample count
     * @param debug whether to request a debug context
     */
    public static synchronized void create(int depth, int stencil, int alpha, int samples, boolean debug) {
        if (handle != NULL) {
            throw new IllegalStateException("the window is already created");
        }
        init();
        mainThread = Thread.currentThread();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, resizable ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_DEPTH_BITS, depth);
        glfwWindowHint(GLFW_STENCIL_BITS, stencil);
        glfwWindowHint(GLFW_ALPHA_BITS, alpha);
        glfwWindowHint(GLFW_SAMPLES, samples);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, debug ? GLFW_TRUE : GLFW_FALSE);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
        glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);

        long monitor = fullscreen ? glfwGetPrimaryMonitor() : NULL;
        handle = glfwCreateWindow(width, height, title, monitor, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("GLFW could not create a window for this pixel format");
        }
        if (!fullscreen) {
            center();
        }
        glfwMakeContextCurrent(handle);
        capabilities = GL.createCapabilities();
        glfwSwapInterval(vsync ? 1 : 0);
        applyIcons();

        glfwSetFramebufferSizeCallback(handle, (w, fw, fh) -> {
            framebufferWidth = fw;
            framebufferHeight = fh;
            resized = true;
        });
        glfwSetWindowContentScaleCallback(handle, (w, sx, sy) -> updateScale());
        glfwSetWindowSizeCallback(handle, (w, ww, wh) -> updateScale());
        glfwSetWindowFocusCallback(handle, (w, f) -> {
            focused = f;
            if (!f) {
                Input.clear();
            }
        });
        glfwSetWindowIconifyCallback(handle, (w, i) -> iconified = i);
        Input.install(handle);
        updateScale();
        glfwShowWindow(handle);
        glfwFocusWindow(handle);
        // macOS gives a Retina window its 2x framebuffer only once it is on screen: measure again after showing it,
        // and report a resize so the game sizes its viewport to the real framebuffer.
        glfwPollEvents();
        updateScale();
        resized = true;
        focused = glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE;
    }

    /** Destroys the window. */
    public static synchronized void destroy() {
        if (handle == NULL) {
            return;
        }
        glfwDestroyWindow(handle);
        handle = NULL;
        capabilities = null;
    }

    /** Swaps buffers, and on the main thread also processes window events. */
    public static void update() {
        if (handle == NULL) {
            return;
        }
        glfwSwapBuffers(handle);
        processMessages();
    }

    /** Processes window events, only on the main thread; GLFW forbids it elsewhere. */
    public static void processMessages() {
        if (handle != NULL && Thread.currentThread() == mainThread) {
            glfwPollEvents();
        }
    }

    /**
     * Sleeps so the caller runs at most {@code fps} frames per second. Keeps a schedule of frame deadlines and
     * resets it after falling more than a frame behind, so a slow frame does not cause a burst.
     *
     * @param fps the frame rate cap
     */
    public static void sync(int fps) {
        if (fps <= 0) {
            return;
        }
        long frame = 1_000_000_000L / fps;
        long now = System.nanoTime();
        if (nextFrame == 0 || now - nextFrame > frame) {
            nextFrame = now;
        }
        nextFrame += frame;
        try {
            while (true) {
                long left = nextFrame - System.nanoTime();
                if (left <= 0) {
                    break;
                }
                if (left > 2_000_000L) {
                    Thread.sleep(1);
                } else {
                    Thread.yield();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sets the window size used in windowed mode, and the resolution used full screen.
     *
     * @param w width
     * @param h height
     * @param refresh refresh rate in Hz, 0 for any
     */
    public static synchronized void setMode(int w, int h, int refresh) {
        width = w;
        height = h;
        refreshRate = refresh;
        if (handle != NULL) {
            applyMode();
        }
    }

    /**
     * Switches between full screen and windowed.
     *
     * @param on whether to go full screen
     */
    public static synchronized void setFullscreen(boolean on) {
        fullscreen = on;
        if (handle != NULL) {
            applyMode();
        }
    }

    private static void applyMode() {
        if (fullscreen) {
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode desktop = glfwGetVideoMode(monitor);
            int rate = refreshRate > 0 ? refreshRate : desktop.refreshRate();
            glfwSetWindowMonitor(handle, monitor, 0, 0, width, height, rate);
        } else {
            glfwSetWindowMonitor(handle, NULL, 0, 0, width, height, GLFW_DONT_CARE);
            center();
        }
        glfwSwapInterval(vsync ? 1 : 0);
        updateScale();
        resized = true;
    }

    private static void center() {
        GLFWVidMode desktop = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (desktop != null) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var w = stack.mallocInt(1);
                var h = stack.mallocInt(1);
                glfwGetWindowSize(handle, w, h);
                glfwSetWindowPos(handle, (desktop.width() - w.get(0)) / 2, (desktop.height() - h.get(0)) / 2);
            }
        }
    }

    private static void updateScale() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ww = stack.mallocInt(1);
            var wh = stack.mallocInt(1);
            var fw = stack.mallocInt(1);
            var fh = stack.mallocInt(1);
            glfwGetWindowSize(handle, ww, wh);
            glfwGetFramebufferSize(handle, fw, fh);
            if (fw.get(0) != framebufferWidth || fh.get(0) != framebufferHeight) {
                resized = true;
            }
            framebufferWidth = fw.get(0);
            framebufferHeight = fh.get(0);
            scaleX = ww.get(0) > 0 ? (float) fw.get(0) / ww.get(0) : 1f;
            scaleY = wh.get(0) > 0 ? (float) fh.get(0) / wh.get(0) : 1f;
        }
    }

    /**
     * Sets the window icons from LWJGL 2-style square RGBA buffers.
     *
     * @param buffers RGBA pixels; each buffer is a square image
     * @return how many icons were accepted
     */
    public static synchronized int setIcons(ByteBuffer[] buffers) {
        List<ByteBuffer> copy = new ArrayList<>();
        for (ByteBuffer buffer : buffers) {
            if (buffer != null && buffer.remaining() >= 4) {
                copy.add(buffer.duplicate());
            }
        }
        icons = copy;
        if (handle != NULL) {
            applyIcons();
        }
        return copy.size();
    }

    private static void applyIcons() {
        if (icons.isEmpty() || org.lwjgl.LWJGLUtil.getPlatform() == org.lwjgl.LWJGLUtil.PLATFORM_MACOSX) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer images = GLFWImage.malloc(icons.size(), stack);
            for (int i = 0; i < icons.size(); i++) {
                ByteBuffer src = icons.get(i);
                int side = (int) Math.sqrt(src.remaining() / 4.0);
                ByteBuffer pixels = stack.malloc(side * side * 4);
                pixels.put(src.duplicate().limit(src.position() + side * side * 4))
                        .flip();
                images.get(i).set(side, side, pixels);
            }
            glfwSetWindowIcon(handle, images);
        }
    }

    /**
     * Sets the title.
     *
     * @param newTitle the title
     */
    public static synchronized void setTitle(String newTitle) {
        title = newTitle == null ? "" : newTitle;
        if (handle != NULL) {
            glfwSetWindowTitle(handle, title);
        }
    }

    /**
     * Sets whether the window can be resized.
     *
     * @param on whether it can
     */
    public static synchronized void setResizable(boolean on) {
        resizable = on;
        if (handle != NULL) {
            glfwSetWindowAttrib(handle, GLFW_RESIZABLE, on ? GLFW_TRUE : GLFW_FALSE);
        }
    }

    /**
     * Turns vertical sync on or off for the calling thread's context.
     *
     * @param on whether to sync
     */
    public static synchronized void setVSync(boolean on) {
        vsync = on;
        if (handle != NULL && glfwGetCurrentContext() == handle) {
            glfwSwapInterval(on ? 1 : 0);
        }
    }

    /** {@return the GLFW window, or {@code NULL}} */
    public static long handle() {
        return handle;
    }

    /** {@return the main context's LWJGL 3 capabilities} */
    public static GLCapabilities capabilities() {
        return capabilities;
    }

    /** {@return whether the window exists} */
    public static boolean created() {
        return handle != NULL;
    }

    /** {@return whether the user asked to close the window} */
    public static boolean closeRequested() {
        return handle != NULL && glfwWindowShouldClose(handle);
    }

    /** {@return whether the window has keyboard focus} */
    public static boolean focused() {
        return focused;
    }

    /** {@return whether the window is not minimized} */
    public static boolean visible() {
        return !iconified;
    }

    /** {@return whether the window is full screen} */
    public static boolean fullscreen() {
        return fullscreen;
    }

    /** {@return the width games should use: framebuffer pixels once created, the mode before} */
    public static int framebufferWidth() {
        return handle != NULL ? framebufferWidth : width;
    }

    /** {@return the height games should use: framebuffer pixels once created, the mode before} */
    public static int framebufferHeight() {
        return handle != NULL ? framebufferHeight : height;
    }

    /** {@return framebuffer pixels per window unit, horizontally} */
    public static float contentScaleX() {
        return scaleX;
    }

    /** {@return framebuffer pixels per window unit, vertically} */
    public static float contentScaleY() {
        return scaleY;
    }

    /** {@return the configured mode width} */
    public static int modeWidth() {
        return width;
    }

    /** {@return the configured mode height} */
    public static int modeHeight() {
        return height;
    }

    /** {@return the configured refresh rate} */
    public static int modeRefreshRate() {
        return refreshRate;
    }

    /** {@return whether the calling thread created the window, the only thread that may poll its events} */
    public static boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    /** {@return whether the size changed since the last call} */
    public static synchronized boolean takeResized() {
        boolean r = resized;
        resized = false;
        return r;
    }
}
