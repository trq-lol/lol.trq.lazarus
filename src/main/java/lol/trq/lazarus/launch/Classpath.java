/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The application class path as URLs.
 *
 * <p>Java 9 and newer no longer expose the system class loader as a {@code URLClassLoader}, so a launcher that
 * builds its own class loader reads the class path from {@code java.class.path} instead.
 */
public final class Classpath {
    private Classpath() {}

    /** {@return the entries of {@code java.class.path} as URLs, in order, without duplicates} */
    public static List<URL> current() {
        return parse(System.getProperty("java.class.path", ""), File.pathSeparator);
    }

    /**
     * Splits a class path string into URLs.
     *
     * @param classpath the class path, entries separated by {@code separator}
     * @param separator the path separator, {@code ;} on Windows and {@code :} elsewhere
     * @return the entries as absolute file URLs, in order, with empty and duplicate entries dropped
     */
    public static List<URL> parse(String classpath, String separator) {
        Set<Path> seen = new LinkedHashSet<>();
        for (String entry : classpath.split(java.util.regex.Pattern.quote(separator))) {
            if (!entry.isBlank()) {
                seen.add(Path.of(entry.strip()).toAbsolutePath().normalize());
            }
        }
        List<URL> urls = new ArrayList<>(seen.size());
        for (Path path : seen) {
            try {
                urls.add(path.toUri().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("class path entry is not a valid URL: " + path, e);
            }
        }
        return List.copyOf(urls);
    }
}
