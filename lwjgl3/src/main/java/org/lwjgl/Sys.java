/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/** LWJGL 2's {@code Sys}: time, version and opening URLs. */
public final class Sys {
    private static final long TIMER_RESOLUTION = 1000L;

    private Sys() {}

    /** {@return the version string, naming both the emulated LWJGL 2 API and the LWJGL 3 underneath} */
    public static String getVersion() {
        return "2.9.4 (Lazarus on LWJGL " + Version.getVersion() + ")";
    }

    /** {@return the timer's ticks per second; {@link #getTime()} counts milliseconds} */
    public static long getTimerResolution() {
        return TIMER_RESOLUTION;
    }

    /** {@return a monotonic time in milliseconds} */
    public static long getTime() {
        return System.nanoTime() / 1_000_000L;
    }

    /** {@return whether the running JVM is 64-bit} */
    public static boolean is64Bit() {
        return System.getProperty("os.arch", "").contains("64");
    }

    /**
     * Opens a URL in the system browser.
     *
     * @param url the URL
     * @return whether a browser was started
     */
    public static boolean openURL(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
            String[] command =
                    switch (LWJGLUtil.getPlatform()) {
                        case LWJGLUtil.PLATFORM_WINDOWS ->
                            new String[] {"rundll32", "url.dll,FileProtocolHandler", url};
                        case LWJGLUtil.PLATFORM_MACOSX -> new String[] {"open", url};
                        default -> new String[] {"xdg-open", url};
                    };
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException | URISyntaxException | UnsupportedOperationException e) {
            return false;
        }
    }

    /**
     * Shows an alert. LWJGL 3 has no native dialogs; the message goes to standard error.
     *
     * @param title the title
     * @param message the message
     */
    public static void alert(String title, String message) {
        System.err.println("[" + title + "] " + message);
    }
}
