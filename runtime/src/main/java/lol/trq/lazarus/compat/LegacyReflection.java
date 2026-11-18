/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Stand-in for {@code sun.reflect.ReflectionFactory}, which Java 9 moved and Java 17 sealed.
 *
 * <p>Forge never links against {@code ReflectionFactory}: it looks up the factory and a few of its methods
 * reflectively, keeps them in static fields, and calls them with {@link Method#invoke}. {@link #install(Class)} fills
 * those fields with this class and its methods, which have the same shapes, so Forge's own code runs unchanged.
 */
public final class LegacyReflection {
    /** The factory instance Forge stores as {@code reflectionFactory}. */
    public static final LegacyReflection FACTORY = new LegacyReflection();

    private LegacyReflection() {}

    /**
     * Fills a Forge class's reflection-factory fields with Lazarus stand-ins. Absent fields are skipped.
     *
     * <p>Fields set by name: {@code reflectionFactory}, {@code newConstructorAccessor}, {@code newInstance}, {@code
     * newFieldAccessor}, {@code fieldAccessorSet}, and {@code isSetup} (to {@code true}).
     *
     * @param owner the Forge class
     */
    public static void install(Class<?> owner) {
        try {
            assign(owner, "reflectionFactory", FACTORY);
            assign(
                    owner,
                    "newConstructorAccessor",
                    LegacyReflection.class.getMethod("newConstructorAccessor", Constructor.class));
            assign(owner, "newInstance", ConstructorAccessor.class.getMethod("newInstance", Object[].class));
            assign(
                    owner,
                    "newFieldAccessor",
                    LegacyReflection.class.getMethod("newFieldAccessor", Field.class, boolean.class));
            assign(owner, "fieldAccessorSet", FieldAccessor.class.getMethod("set", Object.class, Object.class));
            assign(owner, "isSetup", Boolean.TRUE);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Same shape as {@code ReflectionFactory.newFieldAccessor}.
     *
     * @param field the field
     * @param override ignored; the accessor always writes
     * @return an accessor that writes the field, final or not
     */
    public Object newFieldAccessor(Field field, boolean override) {
        return new FieldAccessor(field);
    }

    /**
     * Same shape as {@code ReflectionFactory.newConstructorAccessor}.
     *
     * @param constructor the constructor; may belong to an enum
     * @return an accessor that invokes it
     * @throws IllegalAccessException if the constructor's class is not open to Lazarus
     */
    public Object newConstructorAccessor(Constructor<?> constructor) throws IllegalAccessException {
        Class<?> owner = constructor.getDeclaringClass();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, MethodHandles.lookup());
        return new ConstructorAccessor(lookup.unreflectConstructor(constructor));
    }

    /** Same shape as {@code sun.reflect.FieldAccessor}, reduced to {@code set}. */
    public static final class FieldAccessor {
        private final Field field;

        FieldAccessor(Field field) {
            this.field = field;
        }

        /**
         * Writes the field.
         *
         * @param target the instance, or {@code null} for a static field
         * @param value the value
         */
        public void set(Object target, Object value) {
            Fields.set(field, target, value);
        }
    }

    /**
     * Same shape as {@code sun.reflect.ConstructorAccessor}.
     *
     * <p>Unlike {@link Constructor#newInstance}, a method handle may construct an enum constant, which is what Forge's
     * {@code EnumHelper} needs.
     */
    public static final class ConstructorAccessor {
        private final MethodHandle handle;

        ConstructorAccessor(MethodHandle handle) {
            this.handle = handle;
        }

        /**
         * Invokes the constructor.
         *
         * @param args the arguments
         * @return the new instance
         * @throws Throwable whatever the constructor throws
         */
        public Object newInstance(Object[] args) throws Throwable {
            return handle.invokeWithArguments(args == null ? new Object[0] : Arrays.copyOf(args, args.length));
        }
    }

    private static void assign(Class<?> owner, String name, Object value) {
        Field field;
        try {
            field = owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return;
        }
        if (!Modifier.isStatic(field.getModifiers())) {
            return;
        }
        Fields.set(field, null, value);
    }
}
