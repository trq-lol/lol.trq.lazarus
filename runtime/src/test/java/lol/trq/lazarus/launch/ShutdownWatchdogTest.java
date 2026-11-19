/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ShutdownWatchdogTest {
    /** A program whose shutdown never finishes on its own. */
    public static final class StuckShutdown {
        public static void main(String[] args) {
            ShutdownWatchdog.install();
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(
                            () -> {
                                try {
                                    Thread.sleep(Long.MAX_VALUE);
                                } catch (InterruptedException ignored) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "stuck-hook"));
            System.exit(0);
        }
    }

    @Test
    void haltsAStuckShutdownAndDumpsThreads() throws Exception {
        String java = ProcessHandle.current().info().command().orElse("java");
        Process process = new ProcessBuilder(
                        java,
                        "-Dlazarus.shutdownGrace=1",
                        "-cp",
                        System.getProperty("java.class.path"),
                        StuckShutdown.class.getName())
                .redirectErrorStream(true)
                .start();
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the watchdog did not halt the JVM");
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(1, process.exitValue(), output);
        assertTrue(output.contains("shutdown has not finished"), output);
        assertTrue(output.contains("\"stuck-hook\""), output);
    }
}
