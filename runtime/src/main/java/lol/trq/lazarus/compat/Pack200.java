/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarOutputStream;

/**
 * Stand-in for {@code java.util.jar.Pack200}, removed in Java 14. Patched code calls this class instead.
 *
 * <p>Unpacking is delegated to Apache Commons Compress. Its unpacker reads segments while {@link
 * InputStream#available()} is positive, which stops at once on streams that report 0, such as an LZMA stream, so the
 * input is buffered first. The JDK unpacker left the jar complete in its target stream, so this one finishes it.
 */
public final class Pack200 {
    private Pack200() {}

    /** {@return a new unpacker} */
    public static Unpacker newUnpacker() {
        return (in, out) -> {
            byte[] packed = in.readAllBytes();
            org.apache.commons.compress.java.util.jar.Pack200.newUnpacker()
                    .unpack(new ByteArrayInputStream(packed), out);
            out.finish();
        };
    }

    /** Stand-in for {@code java.util.jar.Pack200.Unpacker}. */
    @FunctionalInterface
    public interface Unpacker {
        /**
         * Unpacks a Pack200 archive into a jar.
         *
         * @param in the packed archive
         * @param out the jar to write; finished but not closed on return
         * @throws IOException if reading or writing fails
         */
        void unpack(InputStream in, JarOutputStream out) throws IOException;
    }
}
