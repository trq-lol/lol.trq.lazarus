/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3.gl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * With {@code -Dlazarus.gl.debug=true}, writes every shader that fails to compile, and every program that fails to
 * link, to {@code lazarus-shaders/} in the game folder: the original source, the translated source with line numbers,
 * and the driver's log.
 */
final class ShaderDumps {
    private static final Path DIR = Path.of("lazarus-shaders");
    private static final Map<Integer, String[]> sources = new ConcurrentHashMap<>();

    private ShaderDumps() {}

    static void remember(int shader, String original, String translated) {
        sources.put(shader, new String[] {original, translated});
    }

    static void compileFailed(int shader, String log) {
        String[] source = sources.getOrDefault(shader, new String[] {"", ""});
        write(
                "shader-" + shader + ".txt",
                "COMPILE FAILED\n\n" + log + "\n\n---- translated\n" + numbered(source[1]) + "\n---- original\n"
                        + source[0]);
    }

    static void linkFailed(int program, String log) {
        write("program-" + program + ".txt", "LINK FAILED\n\n" + log);
    }

    private static String numbered(String text) {
        StringBuilder out = new StringBuilder();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            out.append(String.format("%4d  ", i + 1)).append(lines[i]).append('\n');
        }
        return out.toString();
    }

    private static void write(String name, String text) {
        try {
            Files.createDirectories(DIR);
            Path file = DIR.resolve(name);
            Files.writeString(file, text);
            System.err.println("[lazarus] shader failure written to " + file.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[lazarus] could not write shader dump " + name + ": " + e);
        }
    }
}
