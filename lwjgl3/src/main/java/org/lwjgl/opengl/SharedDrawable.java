/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import lol.trq.lazarus.lwjgl3.Window;
import org.lwjgl.LWJGLException;

/**
 * A second OpenGL context that shares objects with the window's, as in LWJGL 2. Backed by a hidden 1x1 GLFW window.
 * Forge's splash screen loads resources on this context while another thread draws to the window.
 */
public final class SharedDrawable implements Drawable {
    private long window;
    private GLCapabilities capabilities;

    /**
     * Creates the context. Must be called on the thread that created the window.
     *
     * @param shared the drawable to share objects with; only the game window is supported
     */
    public SharedDrawable(Drawable shared) throws LWJGLException {
        long previous = glfwGetCurrentContext();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(1, 1, "shared", NULL, Window.handle());
        if (window == NULL) {
            throw new LWJGLException("GLFW could not create a shared context");
        }
        glfwMakeContextCurrent(window);
        capabilities = GL.createCapabilities();
        glfwMakeContextCurrent(previous);
        GL.setCapabilities(previous == Window.handle() ? Window.capabilities() : null);
    }

    @Override
    public boolean isCurrent() {
        return window != NULL && glfwGetCurrentContext() == window;
    }

    @Override
    public void makeCurrent() {
        glfwMakeContextCurrent(window);
        GL.setCapabilities(capabilities);
    }

    @Override
    public void releaseContext() {
        if (isCurrent()) {
            glfwMakeContextCurrent(NULL);
            GL.setCapabilities(null);
        }
    }

    @Override
    public void destroy() {
        if (window != NULL) {
            releaseContext();
            glfwDestroyWindow(window);
            window = NULL;
        }
    }
}
