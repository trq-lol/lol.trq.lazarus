/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import java.util.Map;

/**
 * Ends a JVM whose shutdown has stalled. When shutdown begins it starts a daemon thread; if the JVM is still alive
 * after the grace period, the thread prints every thread's stack to standard error and halts.
 *
 * <p>Minecraft stops its integrated server from a shutdown hook, and on macOS the game's main thread is also the
 * window's thread, so a shutdown that waits on it can hang with the window frozen and the mouse captured.
 * {@code -Dlazarus.shutdownGrace=<seconds>} sets the grace period; 0 turns the watchdog off.
 */
final class ShutdownWatchdog {
    private ShutdownWatchdog() {}

    static void install() {
        long grace = Long.getLong("lazarus.shutdownGrace", 20L);
        if (grace <= 0) {
            return;
        }
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        () -> {
                            Thread watchdog = new Thread(() -> watch(grace), "lazarus-shutdown-watchdog");
                            watchdog.setDaemon(true);
                            watchdog.start();
                        },
                        "lazarus-shutdown-watchdog-starter"));
    }

    private static void watch(long graceSeconds) {
        try {
            Thread.sleep(graceSeconds * 1000L);
        } catch (InterruptedException e) {
            return;
        }
        StringBuilder dump = new StringBuilder("[Lazarus] shutdown has not finished after ")
                .append(graceSeconds)
                .append(" s; halting. Thread dump follows.\n");
        for (Map.Entry<Thread, StackTraceElement[]> entry :
                Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            dump.append('"')
                    .append(thread.getName())
                    .append("\" ")
                    .append(thread.getState())
                    .append('\n');
            for (StackTraceElement frame : entry.getValue()) {
                dump.append("    at ").append(frame).append('\n');
            }
        }
        System.err.print(dump);
        System.err.flush();
        Runtime.getRuntime().halt(1);
    }
}
