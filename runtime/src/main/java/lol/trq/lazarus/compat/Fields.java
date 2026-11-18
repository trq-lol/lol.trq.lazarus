/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

/**
 * Writes to fields that reflection refuses to write, including {@code static final} ones.
 *
 * <p>Forge fills {@code static final} fields such as {@code Blocks.stone} after the classes load. Java 8 allowed that
 * by clearing {@code Field.modifiers}; Java 12 hid that field, and Java 17 seals the JDK internals that replaced it.
 * {@link Unsafe} still writes such fields, and is what this class uses.
 */
public final class Fields {
    private static final Unsafe UNSAFE = loadUnsafe();

    private Fields() {}

    /**
     * Sets a field, final or not.
     *
     * @param field the field
     * @param target the instance, or {@code null} for a static field
     * @param value the value; boxed for primitive fields
     */
    @SuppressWarnings("removal")
    public static void set(Field field, Object target, Object value) {
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        Object base = isStatic ? UNSAFE.staticFieldBase(field) : target;
        long offset = isStatic ? UNSAFE.staticFieldOffset(field) : UNSAFE.objectFieldOffset(field);
        if (isStatic) {
            initialize(field.getDeclaringClass());
        }
        Class<?> type = field.getType();
        boolean isVolatile = Modifier.isVolatile(field.getModifiers());
        if (!type.isPrimitive()) {
            if (isVolatile) {
                UNSAFE.putObjectVolatile(base, offset, value);
            } else {
                UNSAFE.putObject(base, offset, value);
            }
        } else if (type == int.class) {
            UNSAFE.putInt(base, offset, (Integer) value);
        } else if (type == long.class) {
            UNSAFE.putLong(base, offset, (Long) value);
        } else if (type == boolean.class) {
            UNSAFE.putBoolean(base, offset, (Boolean) value);
        } else if (type == float.class) {
            UNSAFE.putFloat(base, offset, (Float) value);
        } else if (type == double.class) {
            UNSAFE.putDouble(base, offset, (Double) value);
        } else if (type == byte.class) {
            UNSAFE.putByte(base, offset, (Byte) value);
        } else if (type == short.class) {
            UNSAFE.putShort(base, offset, (Short) value);
        } else {
            UNSAFE.putChar(base, offset, (Character) value);
        }
    }

    private static void initialize(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Unsafe loadUnsafe() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            return (Unsafe) theUnsafe.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
