/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import org.objectweb.asm.Type;

/** ASM 5 behaviour that later ASM releases made strict. Patched code calls these instead. */
public final class LegacyAsm {
    private LegacyAsm() {}

    /**
     * {@link Type#getType(String)} that also accepts an internal name, as ASM 5 did.
     *
     * <p>ASM 5 parsed {@code "net/example/Foo"} without complaint and returned a type whose {@link
     * Type#getInternalName()} was the input. FML relies on that; ASM 9 throws instead.
     *
     * @param descriptor a type descriptor, or an internal name
     * @return the type
     */
    public static Type getType(String descriptor) {
        try {
            return Type.getType(descriptor);
        } catch (IllegalArgumentException e) {
            return Type.getObjectType(descriptor);
        }
    }

    /**
     * Null-safe {@link Type#equals(Object)}. FML compares a class's super type, which is {@code null} for {@code
     * module-info}.
     *
     * @param type the receiver, possibly {@code null}
     * @param other the other value
     * @return whether both are equal; {@code false} when {@code type} is {@code null}
     */
    public static boolean typeEquals(Type type, Object other) {
        return type != null && type.equals(other);
    }
}
