/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Translates GLFW key codes to LWJGL 2 key codes. LWJGL 2 used DirectInput scan codes (0 to 255), which games store
 * in their key binding files, so the translation must match LWJGL 2 exactly.
 */
public final class KeyCodes {
    /** LWJGL 2 code for each GLFW key, or 0 ({@code KEY_NONE}). */
    private static final int[] TO_LWJGL2 = new int[GLFW_KEY_LAST + 1];

    static {
        map(GLFW_KEY_ESCAPE, 0x01);
        map(GLFW_KEY_1, 0x02);
        map(GLFW_KEY_2, 0x03);
        map(GLFW_KEY_3, 0x04);
        map(GLFW_KEY_4, 0x05);
        map(GLFW_KEY_5, 0x06);
        map(GLFW_KEY_6, 0x07);
        map(GLFW_KEY_7, 0x08);
        map(GLFW_KEY_8, 0x09);
        map(GLFW_KEY_9, 0x0A);
        map(GLFW_KEY_0, 0x0B);
        map(GLFW_KEY_MINUS, 0x0C);
        map(GLFW_KEY_EQUAL, 0x0D);
        map(GLFW_KEY_BACKSPACE, 0x0E);
        map(GLFW_KEY_TAB, 0x0F);
        map(GLFW_KEY_Q, 0x10);
        map(GLFW_KEY_W, 0x11);
        map(GLFW_KEY_E, 0x12);
        map(GLFW_KEY_R, 0x13);
        map(GLFW_KEY_T, 0x14);
        map(GLFW_KEY_Y, 0x15);
        map(GLFW_KEY_U, 0x16);
        map(GLFW_KEY_I, 0x17);
        map(GLFW_KEY_O, 0x18);
        map(GLFW_KEY_P, 0x19);
        map(GLFW_KEY_LEFT_BRACKET, 0x1A);
        map(GLFW_KEY_RIGHT_BRACKET, 0x1B);
        map(GLFW_KEY_ENTER, 0x1C);
        map(GLFW_KEY_LEFT_CONTROL, 0x1D);
        map(GLFW_KEY_A, 0x1E);
        map(GLFW_KEY_S, 0x1F);
        map(GLFW_KEY_D, 0x20);
        map(GLFW_KEY_F, 0x21);
        map(GLFW_KEY_G, 0x22);
        map(GLFW_KEY_H, 0x23);
        map(GLFW_KEY_J, 0x24);
        map(GLFW_KEY_K, 0x25);
        map(GLFW_KEY_L, 0x26);
        map(GLFW_KEY_SEMICOLON, 0x27);
        map(GLFW_KEY_APOSTROPHE, 0x28);
        map(GLFW_KEY_GRAVE_ACCENT, 0x29);
        map(GLFW_KEY_LEFT_SHIFT, 0x2A);
        map(GLFW_KEY_BACKSLASH, 0x2B);
        map(GLFW_KEY_Z, 0x2C);
        map(GLFW_KEY_X, 0x2D);
        map(GLFW_KEY_C, 0x2E);
        map(GLFW_KEY_V, 0x2F);
        map(GLFW_KEY_B, 0x30);
        map(GLFW_KEY_N, 0x31);
        map(GLFW_KEY_M, 0x32);
        map(GLFW_KEY_COMMA, 0x33);
        map(GLFW_KEY_PERIOD, 0x34);
        map(GLFW_KEY_SLASH, 0x35);
        map(GLFW_KEY_RIGHT_SHIFT, 0x36);
        map(GLFW_KEY_KP_MULTIPLY, 0x37);
        map(GLFW_KEY_LEFT_ALT, 0x38);
        map(GLFW_KEY_SPACE, 0x39);
        map(GLFW_KEY_CAPS_LOCK, 0x3A);
        map(GLFW_KEY_F1, 0x3B);
        map(GLFW_KEY_F2, 0x3C);
        map(GLFW_KEY_F3, 0x3D);
        map(GLFW_KEY_F4, 0x3E);
        map(GLFW_KEY_F5, 0x3F);
        map(GLFW_KEY_F6, 0x40);
        map(GLFW_KEY_F7, 0x41);
        map(GLFW_KEY_F8, 0x42);
        map(GLFW_KEY_F9, 0x43);
        map(GLFW_KEY_F10, 0x44);
        map(GLFW_KEY_NUM_LOCK, 0x45);
        map(GLFW_KEY_SCROLL_LOCK, 0x46);
        map(GLFW_KEY_KP_7, 0x47);
        map(GLFW_KEY_KP_8, 0x48);
        map(GLFW_KEY_KP_9, 0x49);
        map(GLFW_KEY_KP_SUBTRACT, 0x4A);
        map(GLFW_KEY_KP_4, 0x4B);
        map(GLFW_KEY_KP_5, 0x4C);
        map(GLFW_KEY_KP_6, 0x4D);
        map(GLFW_KEY_KP_ADD, 0x4E);
        map(GLFW_KEY_KP_1, 0x4F);
        map(GLFW_KEY_KP_2, 0x50);
        map(GLFW_KEY_KP_3, 0x51);
        map(GLFW_KEY_KP_0, 0x52);
        map(GLFW_KEY_KP_DECIMAL, 0x53);
        map(GLFW_KEY_F11, 0x57);
        map(GLFW_KEY_F12, 0x58);
        map(GLFW_KEY_F13, 0x64);
        map(GLFW_KEY_F14, 0x65);
        map(GLFW_KEY_F15, 0x66);
        map(GLFW_KEY_F16, 0x67);
        map(GLFW_KEY_F17, 0x68);
        map(GLFW_KEY_F18, 0x69);
        map(GLFW_KEY_F19, 0x71);
        map(GLFW_KEY_KP_EQUAL, 0x8D);
        map(GLFW_KEY_KP_ENTER, 0x9C);
        map(GLFW_KEY_RIGHT_CONTROL, 0x9D);
        map(GLFW_KEY_KP_DIVIDE, 0xB5);
        map(GLFW_KEY_PRINT_SCREEN, 0xB7);
        map(GLFW_KEY_RIGHT_ALT, 0xB8);
        map(GLFW_KEY_PAUSE, 0xC5);
        map(GLFW_KEY_HOME, 0xC7);
        map(GLFW_KEY_UP, 0xC8);
        map(GLFW_KEY_PAGE_UP, 0xC9);
        map(GLFW_KEY_LEFT, 0xCB);
        map(GLFW_KEY_RIGHT, 0xCD);
        map(GLFW_KEY_END, 0xCF);
        map(GLFW_KEY_DOWN, 0xD0);
        map(GLFW_KEY_PAGE_DOWN, 0xD1);
        map(GLFW_KEY_INSERT, 0xD2);
        map(GLFW_KEY_DELETE, 0xD3);
        map(GLFW_KEY_LEFT_SUPER, 0xDB);
        map(GLFW_KEY_RIGHT_SUPER, 0xDC);
        map(GLFW_KEY_MENU, 0xDD);
    }

    private KeyCodes() {}

    private static void map(int glfw, int lwjgl2) {
        TO_LWJGL2[glfw] = lwjgl2;
    }

    /**
     * {@return the LWJGL 2 code for a GLFW key, or 0 when it has none}
     *
     * @param glfwKey a {@code GLFW_KEY_*} value
     */
    public static int toLwjgl2(int glfwKey) {
        return glfwKey >= 0 && glfwKey < TO_LWJGL2.length ? TO_LWJGL2[glfwKey] : 0;
    }

    /**
     * {@return the GLFW key for an LWJGL 2 code, or {@code GLFW_KEY_UNKNOWN}}
     *
     * @param lwjgl2Key an LWJGL 2 {@code KEY_*} value
     */
    public static int toGlfw(int lwjgl2Key) {
        if (lwjgl2Key <= 0) {
            return GLFW_KEY_UNKNOWN;
        }
        for (int glfw = 0; glfw < TO_LWJGL2.length; glfw++) {
            if (TO_LWJGL2[glfw] == lwjgl2Key) {
                return glfw;
            }
        }
        return GLFW_KEY_UNKNOWN;
    }
}
