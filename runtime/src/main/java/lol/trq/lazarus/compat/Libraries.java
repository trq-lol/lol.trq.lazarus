/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

/** Libraries Lazarus adds to the class path, which FML must not scan for mods. */
public final class Libraries {
    /** File name prefixes of the libraries a Lazarus launch profile adds. */
    static final List<String> PREFIXES = List.of(
            "lol.trq.lazarus", "asm-", "mixin-", "sponge-mixin-", "mixinextras-", "lwjgl-", "commons-compress-");

    private Libraries() {}

    /**
     * {@return whether a class path jar is a library rather than a mod}
     *
     * <p>Extra prefixes can be given, comma-separated, in {@code -Dlazarus.libraryPrefixes}.
     *
     * @param file the jar
     */
    public static boolean isLibrary(File file) {
        String name = file.getName();
        if (!name.endsWith(".jar")) {
            return false;
        }
        String extra = System.getProperty("lazarus.libraryPrefixes", "");
        return Stream.concat(PREFIXES.stream(), Stream.of(extra.split(",")))
                .filter(p -> !p.isBlank())
                .anyMatch(name::startsWith);
    }
}
