/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.smoke;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Locale;

/**
 * Frame time benchmark: a fixed world seed, a still player and a camera that turns one full circle, so runs are
 * comparable across JVMs, LWJGL versions and renderers. {@code -Dlazarus.smoke.benchDebug=true} keeps the F3 screen
 * open, which is thousands of tiny text draws.
 */
final class Bench {
    private final long durationNanos;
    private long[] frames = new long[1 << 16];
    private int count;
    private long startNanos;
    private long lastNanos;
    private float baseYaw;
    private long gcCountStart;
    private long gcMillisStart;
    private boolean running;
    private boolean done;

    Bench(int seconds) {
        this.durationNanos = seconds * 1_000_000_000L;
    }

    void start(Object mc) throws ReflectiveOperationException {
        Object settings = field(mc, "field_71474_y");
        settings.getClass().getField("field_74350_i").setInt(settings, 260);
        settings.getClass().getField("field_74352_v").setBoolean(settings, false);
        if (Boolean.getBoolean("lazarus.smoke.benchDebug")) {
            settings.getClass().getField("field_74330_P").setBoolean(settings, true);
        }
        try {
            Class.forName("org.lwjgl.opengl.Display")
                    .getMethod("setVSyncEnabled", boolean.class)
                    .invoke(null, false);
        } catch (ReflectiveOperationException | LinkageError e) {
            System.out.println("LAZARUS-BENCH could not turn vsync off: " + e);
        }
        Object player = field(mc, "field_71439_g");
        baseYaw = player.getClass().getField("field_70177_z").getFloat(player);
        long[] gc = gc();
        gcCountStart = gc[0];
        gcMillisStart = gc[1];
        startNanos = System.nanoTime();
        lastNanos = startNanos;
        running = true;
    }

    void frame(Object mc) throws ReflectiveOperationException {
        if (!running) {
            return;
        }
        long now = System.nanoTime();
        if (count == frames.length) {
            frames = Arrays.copyOf(frames, count * 2);
        }
        frames[count++] = now - lastNanos;
        lastNanos = now;
        float progress = (float) (now - startNanos) / durationNanos;
        Object player = field(mc, "field_71439_g");
        if (player != null) {
            float yaw = baseYaw + 360f * Math.min(progress, 1f);
            Class<?> type = player.getClass();
            type.getField("field_70177_z").setFloat(player, yaw);
            type.getField("field_70126_B").setFloat(player, yaw);
            type.getField("field_70125_A").setFloat(player, 0f);
            type.getField("field_70127_C").setFloat(player, 0f);
        }
        if (now - startNanos >= durationNanos) {
            running = false;
            done = true;
        }
    }

    boolean done() {
        return done;
    }

    void report(String optifine) {
        long[] sorted = Arrays.copyOf(frames, count);
        Arrays.sort(sorted);
        long total = 0;
        for (long f : sorted) {
            total += f;
        }
        double avgMs = total / 1e6 / count;
        long[] gc = gc();
        Runtime rt = Runtime.getRuntime();
        System.out.println(String.format(
                Locale.ROOT,
                "LAZARUS-BENCH fps=%.1f avg_ms=%.2f p50_ms=%.2f p99_ms=%.2f p999_ms=%.2f max_ms=%.2f low1_fps=%.1f"
                        + " frames=%d gc=%d gc_ms=%d heap_mb=%d java=%s gl=%s lwjgl=%s optifine=%s",
                1000.0 / avgMs,
                avgMs,
                pct(sorted, 0.50),
                pct(sorted, 0.99),
                pct(sorted, 0.999),
                sorted[count - 1] / 1e6,
                lowFps(sorted, 0.01),
                count,
                gc[0] - gcCountStart,
                gc[1] - gcMillisStart,
                (rt.totalMemory() - rt.freeMemory()) >> 20,
                System.getProperty("java.version"),
                System.getProperty("lazarus.gl", "compat"),
                lwjgl(),
                optifine));
    }

    private static double pct(long[] sorted, double p) {
        return sorted[Math.min(sorted.length - 1, (int) (sorted.length * p))] / 1e6;
    }

    /** The average frame rate over the slowest fraction of frames, the usual "1% low". */
    private static double lowFps(long[] sorted, double fraction) {
        int n = Math.max(1, (int) (sorted.length * fraction));
        long sum = 0;
        for (int i = sorted.length - n; i < sorted.length; i++) {
            sum += sorted[i];
        }
        return 1e9 / (sum / (double) n);
    }

    private static long[] gc() {
        long count = 0;
        long millis = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            count += Math.max(0, bean.getCollectionCount());
            millis += Math.max(0, bean.getCollectionTime());
        }
        return new long[] {count, millis};
    }

    private static String lwjgl() {
        try {
            Class.forName("org.lwjgl.glfw.GLFW", false, Bench.class.getClassLoader());
            return "3";
        } catch (ClassNotFoundException | LinkageError e) {
            return "2";
        }
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        return owner.getClass().getField(name).get(owner);
    }
}
