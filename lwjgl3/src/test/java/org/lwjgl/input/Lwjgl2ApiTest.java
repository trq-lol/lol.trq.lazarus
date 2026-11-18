/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.PixelFormat;

class Lwjgl2ApiTest {
    @Test
    void keyNamesMatchLwjgl2() {
        assertEquals("ESCAPE", Keyboard.getKeyName(Keyboard.KEY_ESCAPE));
        assertEquals("A", Keyboard.getKeyName(Keyboard.KEY_A));
        assertEquals("1", Keyboard.getKeyName(Keyboard.KEY_1));
        assertEquals("LMETA", Keyboard.getKeyName(Keyboard.KEY_LMETA));
        assertEquals("NONE", Keyboard.getKeyName(Keyboard.KEY_NONE));
        assertNull(Keyboard.getKeyName(0xFF));
        assertEquals(Keyboard.KEY_F3, Keyboard.getKeyIndex("F3"));
        assertEquals(Keyboard.KEY_NONE, Keyboard.getKeyIndex("NOT_A_KEY"));
    }

    @Test
    void pixelFormatCopiesOnChange() {
        PixelFormat base = new PixelFormat();
        PixelFormat deep = base.withDepthBits(32).withSamples(4);
        assertNotSame(base, deep);
        assertEquals(24, base.getDepthBits());
        assertEquals(32, deep.getDepthBits());
        assertEquals(4, deep.getSamples());
    }

    @Test
    void windowedDisplayModeHasNoDepthOrRate() {
        DisplayMode mode = new DisplayMode(854, 480);
        assertEquals(854, mode.getWidth());
        assertEquals(480, mode.getHeight());
        assertEquals(0, mode.getBitsPerPixel());
        assertEquals(0, mode.getFrequency());
    }
}
