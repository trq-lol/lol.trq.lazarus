/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InputTest {
    @BeforeEach
    void reset() {
        Input.clear();
        Input.setRepeatEvents(false);
        while (Input.nextKey() != null) {}
        while (Input.nextMouse() != null) {}
    }

    @Test
    void keyCodesMatchLwjgl2() {
        assertEquals(0x01, KeyCodes.toLwjgl2(GLFW_KEY_ESCAPE));
        assertEquals(0x1E, KeyCodes.toLwjgl2(GLFW_KEY_A));
        assertEquals(0x11, KeyCodes.toLwjgl2(GLFW_KEY_W));
        assertEquals(0x39, KeyCodes.toLwjgl2(GLFW_KEY_SPACE));
        assertEquals(0x2A, KeyCodes.toLwjgl2(GLFW_KEY_LEFT_SHIFT));
        assertEquals(0x9D, KeyCodes.toLwjgl2(GLFW_KEY_RIGHT_CONTROL));
        assertEquals(0xC8, KeyCodes.toLwjgl2(GLFW_KEY_UP));
        assertEquals(0x0B, KeyCodes.toLwjgl2(GLFW_KEY_0));
        assertEquals(0x57, KeyCodes.toLwjgl2(GLFW_KEY_F11));
        assertEquals(0, KeyCodes.toLwjgl2(GLFW_KEY_WORLD_1));
        assertEquals(0, KeyCodes.toLwjgl2(GLFW_KEY_UNKNOWN));
        assertEquals(GLFW_KEY_A, KeyCodes.toGlfw(0x1E));
    }

    @Test
    void characterJoinsItsKeyPress() {
        Input.onKey(GLFW_KEY_A, GLFW_PRESS);
        Input.onChar('a');
        Input.KeyEvent press = Input.nextKey();
        assertEquals(0x1E, press.key);
        assertTrue(press.down);
        assertEquals('a', press.character);
        assertNull(Input.nextKey());
        assertTrue(Input.isKeyDown(0x1E));
    }

    @Test
    void characterWithoutKeyGetsItsOwnEvent() {
        Input.onChar('é');
        Input.KeyEvent event = Input.nextKey();
        assertEquals(0, event.key);
        assertTrue(event.down);
        assertEquals('é', event.character);
    }

    @Test
    void supplementaryCharactersBecomeTwoEvents() {
        Input.onKey(GLFW_KEY_E, GLFW_PRESS);
        Input.onChar(0x1F600);
        Input.KeyEvent first = Input.nextKey();
        Input.KeyEvent second = Input.nextKey();
        assertEquals(Character.highSurrogate(0x1F600), first.character);
        assertEquals(Character.lowSurrogate(0x1F600), second.character);
    }

    @Test
    void releaseClearsHeldKeyAndRepeatsAreOptIn() {
        Input.onKey(GLFW_KEY_W, GLFW_PRESS);
        Input.onKey(GLFW_KEY_W, GLFW_REPEAT);
        Input.onKey(GLFW_KEY_W, GLFW_RELEASE);
        assertTrue(Input.nextKey().down);
        Input.KeyEvent release = Input.nextKey();
        assertFalse(release.down);
        assertNull(Input.nextKey());
        assertFalse(Input.isKeyDown(0x11));

        Input.setRepeatEvents(true);
        Input.onKey(GLFW_KEY_W, GLFW_REPEAT);
        assertTrue(Input.nextKey().repeat);
    }

    @Test
    void wheelUsesLwjgl2Units() {
        Input.onScroll(1.0);
        Input.onScroll(-2.0);
        assertEquals(120, Input.nextMouse().dwheel());
        assertEquals(-240, Input.nextMouse().dwheel());
        assertEquals(-120, Input.takeWheel());
        assertEquals(0, Input.takeWheel());
    }

    @Test
    void buttonsAreTracked() {
        Input.onButton(GLFW_MOUSE_BUTTON_RIGHT, true);
        assertTrue(Input.isButtonDown(1));
        Input.MouseEvent event = Input.nextMouse();
        assertEquals(1, event.button());
        assertTrue(event.down());
        Input.onButton(GLFW_MOUSE_BUTTON_RIGHT, false);
        assertFalse(Input.isButtonDown(1));
    }

    @Test
    void shaderSourceStopsAtTrailingNulls() {
        ByteBuffer src = ByteBuffer.allocateDirect(16);
        src.put("void main(){}\0\0".getBytes(StandardCharsets.UTF_8)).flip();
        assertEquals("void main(){}", Gl2.text(src));
        assertEquals(0, src.position());
    }
}
