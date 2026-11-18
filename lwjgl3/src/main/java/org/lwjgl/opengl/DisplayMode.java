/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.opengl;

/** A display resolution, colour depth and refresh rate, as in LWJGL 2. */
public final class DisplayMode {
    private final int width;
    private final int height;
    private final int bitsPerPixel;
    private final int frequency;
    private final boolean fullscreenCapable;

    /**
     * Creates a windowed mode.
     *
     * @param width the width in pixels
     * @param height the height in pixels
     */
    public DisplayMode(int width, int height) {
        this(width, height, 0, 0, false);
    }

    DisplayMode(int width, int height, int bitsPerPixel, int frequency, boolean fullscreenCapable) {
        this.width = width;
        this.height = height;
        this.bitsPerPixel = bitsPerPixel;
        this.frequency = frequency;
        this.fullscreenCapable = fullscreenCapable;
    }

    /** {@return the width in pixels} */
    public int getWidth() {
        return width;
    }

    /** {@return the height in pixels} */
    public int getHeight() {
        return height;
    }

    /** {@return bits per pixel, or 0 for a windowed mode} */
    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    /** {@return the refresh rate in Hz, or 0 for a windowed mode} */
    public int getFrequency() {
        return frequency;
    }

    /** {@return whether this mode came from the monitor and can be used full screen} */
    public boolean isFullscreenCapable() {
        return fullscreenCapable;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DisplayMode m
                && m.width == width
                && m.height == height
                && m.bitsPerPixel == bitsPerPixel
                && m.frequency == frequency;
    }

    @Override
    public int hashCode() {
        return width ^ height ^ frequency ^ bitsPerPixel;
    }

    @Override
    public String toString() {
        return width + " x " + height + " x " + bitsPerPixel + " @" + frequency + "Hz";
    }
}
