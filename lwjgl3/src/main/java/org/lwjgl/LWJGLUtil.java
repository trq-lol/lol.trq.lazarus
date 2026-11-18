/* SPDX-License-Identifier: LGPL-3.0-or-later */
package org.lwjgl;

import java.util.Locale;

/** The part of LWJGL 2's {@code LWJGLUtil} that games use: platform detection. */
public final class LWJGLUtil {
    /** Linux and other Unix-likes. */
    public static final int PLATFORM_LINUX = 1;

    /** macOS. */
    public static final int PLATFORM_MACOSX = 2;

    /** Windows. */
    public static final int PLATFORM_WINDOWS = 3;

    /** The name LWJGL 2 used for Linux. */
    public static final String PLATFORM_LINUX_NAME = "linux";

    /** The name LWJGL 2 used for macOS. */
    public static final String PLATFORM_MACOSX_NAME = "macosx";

    /** The name LWJGL 2 used for Windows. */
    public static final String PLATFORM_WINDOWS_NAME = "windows";

    /** Whether LWJGL debug output is on ({@code -Dorg.lwjgl.util.Debug=true}). */
    public static final boolean DEBUG = Boolean.getBoolean("org.lwjgl.util.Debug");

    private static final int PLATFORM = detect();

    private LWJGLUtil() {}

    /** {@return the platform: {@link #PLATFORM_LINUX}, {@link #PLATFORM_MACOSX} or {@link #PLATFORM_WINDOWS}} */
    public static int getPlatform() {
        return PLATFORM;
    }

    /** {@return the platform's LWJGL 2 name} */
    public static String getPlatformName() {
        return switch (PLATFORM) {
            case PLATFORM_MACOSX -> PLATFORM_MACOSX_NAME;
            case PLATFORM_WINDOWS -> PLATFORM_WINDOWS_NAME;
            default -> PLATFORM_LINUX_NAME;
        };
    }

    /**
     * Logs a debug line when {@link #DEBUG} is on.
     *
     * @param message the line
     */
    public static void log(CharSequence message) {
        if (DEBUG) {
            System.err.println("[LWJGL] " + message);
        }
    }

    private static int detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.startsWith("windows")) {
            return PLATFORM_WINDOWS;
        }
        if (os.startsWith("mac") || os.startsWith("darwin")) {
            return PLATFORM_MACOSX;
        }
        return PLATFORM_LINUX;
    }
}
