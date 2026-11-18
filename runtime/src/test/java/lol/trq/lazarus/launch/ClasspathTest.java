/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClasspathTest {
    @Test
    void keepsOrderAndDropsEmptyAndDuplicateEntries() throws Exception {
        Path a = Path.of("libs", "a.jar").toAbsolutePath();
        Path b = Path.of("libs", "b.jar").toAbsolutePath();
        String sep = ":";
        List<URL> urls = Classpath.parse("libs/b.jar" + sep + sep + "libs/a.jar" + sep + " libs/b.jar ", sep);
        assertEquals(List.of(b.toUri().toURL(), a.toUri().toURL()), urls);
    }

    @Test
    void emptyClasspathIsEmpty() {
        assertTrue(Classpath.parse("", ";").isEmpty());
    }

    @Test
    void currentReadsTheRunningClasspath() {
        assertTrue(!Classpath.current().isEmpty());
    }
}
