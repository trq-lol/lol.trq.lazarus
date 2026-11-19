/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.lwjgl3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class NoContextGlTest {
    private static final int GL_VERSION = 0x1F02;

    @Test
    void nullWithoutContextAndGlIsNeverCalled() {
        assertNull(NoContextGl.query(GL_VERSION, () -> false, n -> {
            throw new AssertionError("called GL with no context");
        }));
    }

    @Test
    void delegatesWithContext() {
        assertEquals("3.2.0 " + GL_VERSION, NoContextGl.query(GL_VERSION, () -> true, n -> "3.2.0 " + n));
    }
}
