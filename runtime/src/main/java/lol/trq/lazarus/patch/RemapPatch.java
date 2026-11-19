/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

/** Replaces references to removed JDK types with drop-in equivalents, in the named classes only. */
public final class RemapPatch implements ClassPatch {
    private final String name;
    private final Set<String> targets;
    private final Map<String, String> mapping;

    /**
     * Creates the patch.
     *
     * @param name a short name for logs
     * @param targets the internal names of the classes to patch
     * @param mapping internal type names to replace, mapped to their replacements
     */
    public RemapPatch(String name, Set<String> targets, Map<String, String> mapping) {
        this.name = name;
        this.targets = Set.copyOf(targets);
        this.mapping = Map.copyOf(mapping);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Set<String> targets() {
        return targets;
    }

    @Override
    public byte[] apply(String className, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(new ClassRemapper(writer, new SimpleRemapper(Opcodes.ASM9, mapping)), 0);
        return writer.toByteArray();
    }
}
