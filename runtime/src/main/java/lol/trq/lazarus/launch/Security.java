/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import com.sun.management.HotSpotDiagnosticMXBean;
import java.lang.management.ManagementFactory;

/** Checks of JVM settings that keep the game safe on modern Java. */
final class Security {
    private Security() {}

    /**
     * {@return whether HotSpot's Base64 intrinsics are on}
     *
     * <p>Java4Shell (CVE-2026-114938, disclosed 2026-11-17): on x86-64 CPUs with AVX-512 VBMI, the Base64 decode
     * intrinsic in Java 17, 21, 25 and 27 writes past its output buffer for crafted input, which can lead to remote
     * code execution. Java 8, the game's original runtime, does not have the intrinsic. Turning the intrinsics off
     * is the advised stopgap until every JDK in use is patched. Returns {@code false} when the option cannot be
     * read, so a JVM without it is not relaunched forever.
     */
    static boolean base64IntrinsicsEnabled() {
        try {
            HotSpotDiagnosticMXBean hotspot = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            return hotspot != null
                    && Boolean.parseBoolean(
                            hotspot.getVMOption("UseBASE64Intrinsics").getValue());
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }
}
