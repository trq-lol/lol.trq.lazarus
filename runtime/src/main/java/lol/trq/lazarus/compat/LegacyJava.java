/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

/** Java version checks that 2015-era libraries get wrong on Java 9 and newer. */
public final class LegacyJava {
    private LegacyJava() {}

    /**
     * {@return whether a {@code java.specification.version} string names Java 9 or newer}
     *
     * <p>Java 9 dropped the {@code 1.} prefix ({@code "17"}, not {@code "1.17"}), so libraries that only know the old
     * scheme fail to parse it.
     *
     * @param version the version string, possibly {@code null}
     */
    public static boolean isModern(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return end > 0 && Integer.parseInt(version.substring(0, end)) >= 9;
    }
}
