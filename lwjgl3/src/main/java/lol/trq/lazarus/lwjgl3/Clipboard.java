/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.lwjgl.glfw.GLFW.glfwGetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;

/**
 * Minecraft's clipboard on GLFW. Minecraft 1.8.9 reads and writes the clipboard through AWT's {@code Toolkit}, which
 * fails under headless AWT (required on macOS with LWJGL 3, where AWT would otherwise take over the main thread).
 * Lazarus replaces {@code GuiScreen.getClipboardString} and {@code setClipboardString} with these.
 */
public final class Clipboard {
    private Clipboard() {}

    /**
     * Replaces {@code GuiScreen.getClipboardString()}.
     *
     * @param owner {@code GuiScreen}
     * @return the clipboard text, or an empty string
     */
    public static String getClipboardString(Class<?> owner) {
        if (!Window.created()) {
            return "";
        }
        String text = glfwGetClipboardString(Window.handle());
        return text == null ? "" : text;
    }

    /**
     * Replaces {@code GuiScreen.setClipboardString(String)}.
     *
     * @param owner {@code GuiScreen}
     * @param text the text to copy; empty or {@code null} is ignored, as in Minecraft
     */
    public static void setClipboardString(Class<?> owner, String text) {
        if (Window.created() && text != null && !text.isEmpty()) {
            glfwSetClipboardString(Window.handle(), text);
        }
    }
}
