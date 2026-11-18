/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.agent;

/**
 * Logging for code that runs before the game configures Log4j.
 *
 * <p>Touching Log4j from the agent would initialize it before Minecraft's configuration is on the class path.
 */
public final class Log {
    private static final boolean DEBUG = Boolean.getBoolean("lazarus.debug");

    private Log() {}

    /**
     * Logs an informational line when {@code -Dlazarus.debug=true}.
     *
     * @param message the line
     */
    public static void info(String message) {
        if (DEBUG) {
            System.err.println("[Lazarus] " + message);
        }
    }

    /**
     * Logs an error with its stack trace.
     *
     * @param message the line
     * @param error the cause
     */
    public static void error(String message, Throwable error) {
        System.err.println("[Lazarus] ERROR " + message);
        error.printStackTrace();
    }
}
