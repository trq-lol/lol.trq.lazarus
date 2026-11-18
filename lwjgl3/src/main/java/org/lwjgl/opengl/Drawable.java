/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

import org.lwjgl.LWJGLException;

/** Something with an OpenGL context that a thread can make current, as in LWJGL 2. */
public interface Drawable {
    /** {@return whether this drawable's context is current on the calling thread} */
    boolean isCurrent() throws LWJGLException;

    /** Makes this drawable's context current on the calling thread. */
    void makeCurrent() throws LWJGLException;

    /** Releases this drawable's context from the calling thread. */
    void releaseContext() throws LWJGLException;

    /** Destroys the drawable and its context. */
    void destroy();
}
