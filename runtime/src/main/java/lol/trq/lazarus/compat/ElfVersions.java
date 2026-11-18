/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Marks symbol version requirements in a 64-bit little-endian ELF shared library as weak.
 *
 * <p>LWJGL 2's {@code liblwjgl64.so} requires {@code JAWT_GetAWT@SUNWprivate_1.1} from the JDK's {@code libjawt.so}.
 * Some JDK builds no longer tag their symbols with that version, and the dynamic linker then refuses to load LWJGL.
 * A weak requirement ({@code VER_FLG_WEAK}) lets the load succeed; the symbol still binds to the unversioned
 * definition.
 */
public final class ElfVersions {
    static final int SHT_GNU_VERNEED = 0x6ffffffe;
    static final int VER_FLG_WEAK = 0x2;

    private ElfVersions() {}

    /**
     * Marks every requirement on {@code versionName} weak.
     *
     * @param elf the library's bytes; modified in place
     * @param versionName the version to weaken, such as {@code SUNWprivate_1.1}
     * @return how many requirements were changed; 0 if the file is not a 64-bit little-endian ELF or has none
     */
    public static int weaken(byte[] elf, String versionName) {
        if (elf.length < 64
                || elf[0] != 0x7f
                || elf[1] != 'E'
                || elf[2] != 'L'
                || elf[3] != 'F'
                || elf[4] != 2
                || elf[5] != 1) {
            return 0;
        }
        ByteBuffer b = ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN);
        long shoff = b.getLong(0x28);
        int shentsize = Short.toUnsignedInt(b.getShort(0x3a));
        int shnum = Short.toUnsignedInt(b.getShort(0x3c));
        int changed = 0;
        for (int i = 0; i < shnum; i++) {
            int sh = (int) (shoff + (long) i * shentsize);
            if (b.getInt(sh + 4) != SHT_GNU_VERNEED) {
                continue;
            }
            int offset = (int) b.getLong(sh + 0x18);
            int link = b.getInt(sh + 0x28);
            int strtab = (int) b.getLong((int) (shoff + (long) link * shentsize) + 0x18);
            int need = offset;
            while (true) {
                int cnt = Short.toUnsignedInt(b.getShort(need + 2));
                int aux = need + b.getInt(need + 8);
                for (int a = 0; a < cnt; a++) {
                    int name = b.getInt(aux + 8);
                    if (versionName.equals(cString(elf, strtab + name))) {
                        int flags = Short.toUnsignedInt(b.getShort(aux + 4));
                        if ((flags & VER_FLG_WEAK) == 0) {
                            b.putShort(aux + 4, (short) (flags | VER_FLG_WEAK));
                            changed++;
                        }
                    }
                    int next = b.getInt(aux + 12);
                    if (next == 0) {
                        break;
                    }
                    aux += next;
                }
                int nextNeed = b.getInt(need + 12);
                if (nextNeed == 0) {
                    break;
                }
                need += nextNeed;
            }
        }
        return changed;
    }

    private static String cString(byte[] bytes, int start) {
        int end = start;
        while (end < bytes.length && bytes[end] != 0) {
            end++;
        }
        return new String(bytes, start, end - start, StandardCharsets.ISO_8859_1);
    }
}
