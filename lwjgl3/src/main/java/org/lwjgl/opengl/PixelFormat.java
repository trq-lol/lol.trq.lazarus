/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/** The framebuffer a window is created with, as in LWJGL 2. Immutable; each {@code with} returns a copy. */
public final class PixelFormat {
    final int alpha;
    final int depth;
    final int stencil;
    final int samples;

    /** Creates the default format: 24-bit depth, no alpha, stencil or multisampling. */
    public PixelFormat() {
        this(0, 24, 0, 0);
    }

    /**
     * Creates a format.
     *
     * @param alpha alpha bits
     * @param depth depth bits
     * @param stencil stencil bits
     */
    public PixelFormat(int alpha, int depth, int stencil) {
        this(alpha, depth, stencil, 0);
    }

    /**
     * Creates a format.
     *
     * @param alpha alpha bits
     * @param depth depth bits
     * @param stencil stencil bits
     * @param samples multisample count, 0 for none
     */
    public PixelFormat(int alpha, int depth, int stencil, int samples) {
        this.alpha = alpha;
        this.depth = depth;
        this.stencil = stencil;
        this.samples = samples;
    }

    /**
     * {@return a copy with these depth bits}
     *
     * @param depth depth bits
     */
    public PixelFormat withDepthBits(int depth) {
        return new PixelFormat(alpha, depth, stencil, samples);
    }

    /**
     * {@return a copy with these stencil bits}
     *
     * @param stencil stencil bits
     */
    public PixelFormat withStencilBits(int stencil) {
        return new PixelFormat(alpha, depth, stencil, samples);
    }

    /**
     * {@return a copy with these alpha bits}
     *
     * @param alpha alpha bits
     */
    public PixelFormat withAlphaBits(int alpha) {
        return new PixelFormat(alpha, depth, stencil, samples);
    }

    /**
     * {@return a copy with this multisample count}
     *
     * @param samples multisample count, 0 for none
     */
    public PixelFormat withSamples(int samples) {
        return new PixelFormat(alpha, depth, stencil, samples);
    }

    /** {@return alpha bits} */
    public int getAlphaBits() {
        return alpha;
    }

    /** {@return depth bits} */
    public int getDepthBits() {
        return depth;
    }

    /** {@return stencil bits} */
    public int getStencilBits() {
        return stencil;
    }

    /** {@return multisample count} */
    public int getSamples() {
        return samples;
    }
}
