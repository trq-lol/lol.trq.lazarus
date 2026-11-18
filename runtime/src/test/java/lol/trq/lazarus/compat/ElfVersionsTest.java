/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ElfVersionsTest {
    /**
     * Builds a minimal ELF64 LE file with a .dynstr and a .gnu.version_r that requires {@code GLIBC_2.2.5} and {@code
     * SUNWprivate_1.1} from two libraries.
     */
    static byte[] elf() {
        byte[] strtab = "\0libc.so.6\0GLIBC_2.2.5\0libjawt.so\0SUNWprivate_1.1\0".getBytes(StandardCharsets.ISO_8859_1);
        int libc = 1;
        int glibc = libc + "libc.so.6".length() + 1;
        int jawt = glibc + "GLIBC_2.2.5".length() + 1;
        int sunw = jawt + "libjawt.so".length() + 1;

        // Two Verneed entries (16 bytes each), each followed by one Vernaux (16 bytes).
        ByteBuffer vn = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        vn.putShort((short) 1).putShort((short) 1).putInt(libc).putInt(16).putInt(32);
        vn.putInt(0).putShort((short) 0).putShort((short) 2).putInt(glibc).putInt(0);
        vn.putShort((short) 1).putShort((short) 1).putInt(jawt).putInt(16).putInt(0);
        vn.putInt(0).putShort((short) 0).putShort((short) 3).putInt(sunw).putInt(0);

        int strOff = 64;
        int vnOff = strOff + strtab.length;
        int shOff = vnOff + 64;
        ByteBuffer out = ByteBuffer.allocate(shOff + 3 * 64).order(ByteOrder.LITTLE_ENDIAN);
        out.put(new byte[] {0x7f, 'E', 'L', 'F', 2, 1, 1});
        out.putLong(0x28, shOff);
        out.putShort(0x3a, (short) 64);
        out.putShort(0x3c, (short) 3);
        out.position(strOff);
        out.put(strtab);
        out.put(vn.array());
        // section 1: .dynstr (SHT_STRTAB = 3)
        int s1 = shOff + 64;
        out.putInt(s1 + 4, 3);
        out.putLong(s1 + 0x18, strOff);
        // section 2: .gnu.version_r, linked to section 1
        int s2 = shOff + 128;
        out.putInt(s2 + 4, ElfVersions.SHT_GNU_VERNEED);
        out.putLong(s2 + 0x18, vnOff);
        out.putInt(s2 + 0x28, 1);
        return out.array();
    }

    @Test
    void weakensOnlyTheNamedVersion() {
        byte[] bytes = elf();
        assertEquals(1, ElfVersions.weaken(bytes, "SUNWprivate_1.1"));
        ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int vnOff = 64 + "\0libc.so.6\0GLIBC_2.2.5\0libjawt.so\0SUNWprivate_1.1\0".length();
        assertEquals(0, b.getShort(vnOff + 16 + 4));
        assertEquals(ElfVersions.VER_FLG_WEAK, b.getShort(vnOff + 48 + 4));
        assertEquals(0, ElfVersions.weaken(bytes, "SUNWprivate_1.1"));
    }

    @Test
    void ignoresNonElf() {
        assertEquals(0, ElfVersions.weaken(new ByteArrayOutputStream().toByteArray(), "x"));
        assertEquals(0, ElfVersions.weaken(new byte[128], "x"));
    }
}
