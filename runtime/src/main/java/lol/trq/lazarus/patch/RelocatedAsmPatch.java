/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

/**
 * Points a library's private, relocated copy of ASM at the real ASM on the class path.
 *
 * <p>Mixin 0.7 bundles ASM 5 under {@code org/spongepowered/asm/lib}. ASM 5 cannot read class files newer than Java
 * 8, and Mixin reads JDK classes while it builds class hierarchies, so on Java 9 and newer it fails. ASM 9 is API
 * compatible with ASM 5 for everything Mixin uses, so references to the relocated copy are rewritten to {@code
 * org/objectweb/asm}. The fragment is matched anywhere in a name, so copies of Mixin that a mod relocated again are
 * covered as well.
 */
public final class RelocatedAsmPatch implements ClassPatch {
    private final String name;
    private final String owner;
    private final String relocatedAsm;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param owner a path fragment naming the library whose classes are patched, such as {@code org/spongepowered/}
     * @param relocatedAsm the path fragment of the relocated ASM, such as {@code org/spongepowered/asm/lib/}
     */
    public RelocatedAsmPatch(String name, String owner, String relocatedAsm) {
        this.name = name;
        this.owner = owner;
        this.relocatedAsm = relocatedAsm;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> targets() {
        return Set.of();
    }

    @Override
    public boolean matches(String className) {
        return className.contains(owner) && !className.contains(relocatedAsm);
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        if (!Bytes.contains(bytes, relocatedAsm.getBytes(StandardCharsets.ISO_8859_1))) {
            return bytes;
        }
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(
                new ClassRemapper(writer, new Remapper() {
                    @Override
                    public String map(String internalName) {
                        int at = internalName.indexOf(relocatedAsm);
                        if (at < 0) {
                            return internalName;
                        }
                        return "org/objectweb/asm/" + internalName.substring(at + relocatedAsm.length());
                    }
                }),
                0);
        return writer.toByteArray();
    }
}
