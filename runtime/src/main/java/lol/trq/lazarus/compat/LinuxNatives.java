/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Prepares LWJGL 2's Linux natives for the running JDK. See {@link ElfVersions}.
 *
 * <p>The launcher's natives directory is never modified: the natives are copied to a temporary directory, patched
 * there, and LWJGL is pointed at it through its own {@code org.lwjgl.librarypath} property.
 */
public final class LinuxNatives {
    static final String JAWT_VERSION = "SUNWprivate_1.1";

    private LinuxNatives() {}

    /** Patches and redirects LWJGL's natives when running on Linux and they need it. */
    public static void prepare() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("linux")
                || System.getProperty("org.lwjgl.librarypath") != null) {
            return;
        }
        for (String dir : System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            Path natives = Path.of(dir.isBlank() ? "." : dir);
            Path lwjgl = natives.resolve("liblwjgl64.so");
            if (!Files.isRegularFile(lwjgl)) {
                lwjgl = natives.resolve("liblwjgl.so");
            }
            if (!Files.isRegularFile(lwjgl)) {
                continue;
            }
            try {
                byte[] bytes = Files.readAllBytes(lwjgl);
                if (ElfVersions.weaken(bytes, JAWT_VERSION) == 0) {
                    return;
                }
                Path copy = Files.createTempDirectory("lazarus-natives-");
                copy.toFile().deleteOnExit();
                try (Stream<Path> files = Files.list(natives)) {
                    for (Path file : files.filter(Files::isRegularFile).toList()) {
                        Path target = copy.resolve(file.getFileName());
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                        target.toFile().deleteOnExit();
                    }
                }
                Files.write(copy.resolve(lwjgl.getFileName()), bytes);
                System.setProperty(
                        "org.lwjgl.librarypath", copy.toAbsolutePath().toString());
            } catch (IOException e) {
                System.err.println("[Lazarus] could not prepare LWJGL natives: " + e);
            }
            return;
        }
    }
}
