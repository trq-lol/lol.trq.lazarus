/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class CompatTest {
    enum Color {
        RED("red");

        final String label;

        Color(String label) {
            this.label = label;
        }
    }

    static final class Holder {
        static final Object VALUE = new Object();
        static final int NUMBER = Integer.parseInt("1");
        final String name = String.valueOf("before");
    }

    /** Mirrors the reflection-factory fields Forge keeps in ObjectHolderRef and EnumHelper. */
    @SuppressWarnings("unused")
    static final class FakeForge {
        private static Object reflectionFactory;
        private static Method newConstructorAccessor;
        private static Method newInstance;
        private static Method newFieldAccessor;
        private static Method fieldAccessorSet;
        private static boolean isSetup;
    }

    @Test
    void fieldsWritesStaticFinalObjectAndPrimitive() throws Exception {
        Object replacement = new Object();
        Fields.set(Holder.class.getDeclaredField("VALUE"), null, replacement);
        Fields.set(Holder.class.getDeclaredField("NUMBER"), null, 42);
        assertSame(replacement, read(Holder.class, "VALUE", null));
        assertEquals(42, read(Holder.class, "NUMBER", null));
    }

    @Test
    void fieldsWritesInstanceFinal() throws Exception {
        Holder holder = new Holder();
        Fields.set(Holder.class.getDeclaredField("name"), holder, "after");
        assertEquals("after", read(Holder.class, "name", holder));
    }

    @Test
    void legacyReflectionInstallsForgeShapedFields() throws Exception {
        LegacyReflection.install(FakeForge.class);
        Object factory = read(FakeForge.class, "reflectionFactory", null);
        assertSame(LegacyReflection.FACTORY, factory);
        assertEquals(Boolean.TRUE, read(FakeForge.class, "isSetup", null));

        Method newConstructorAccessor = (Method) read(FakeForge.class, "newConstructorAccessor", null);
        Method newInstance = (Method) read(FakeForge.class, "newInstance", null);
        Constructor<Color> ctor = Color.class.getDeclaredConstructor(String.class, int.class, String.class);
        Object accessor = newConstructorAccessor.invoke(factory, ctor);
        Color blue = (Color) newInstance.invoke(accessor, new Object[] {new Object[] {"BLUE", 1, "blue"}});
        assertEquals("BLUE", blue.name());
        assertEquals(1, blue.ordinal());
        assertEquals("blue", blue.label);

        Method newFieldAccessor = (Method) read(FakeForge.class, "newFieldAccessor", null);
        Method set = (Method) read(FakeForge.class, "fieldAccessorSet", null);
        Object writer = newFieldAccessor.invoke(factory, Holder.class.getDeclaredField("VALUE"), false);
        set.invoke(writer, null, "written");
        assertEquals("written", read(Holder.class, "VALUE", null));
    }

    @Test
    void legacyAsmAcceptsInternalNames() {
        assertEquals("net/example/Foo", LegacyAsm.getType("net/example/Foo").getInternalName());
        assertEquals(Type.INT_TYPE, LegacyAsm.getType("I"));
        assertFalse(LegacyAsm.typeEquals(null, Type.INT_TYPE));
        assertTrue(LegacyAsm.typeEquals(Type.INT_TYPE, Type.INT_TYPE));
    }

    @Test
    void legacyJavaRecognisesModernVersions() {
        assertTrue(LegacyJava.isModern("17"));
        assertTrue(LegacyJava.isModern("25"));
        assertTrue(LegacyJava.isModern("9"));
        assertFalse(LegacyJava.isModern("1.8"));
        assertFalse(LegacyJava.isModern(""));
        assertFalse(LegacyJava.isModern(null));
    }

    @Test
    void librariesRecognisesLazarusAdditions() {
        assertTrue(Libraries.isLibrary(new File("asm-9.11.1.jar")));
        assertTrue(Libraries.isLibrary(new File("lol.trq.lazarus-0.1.0.jar")));
        assertFalse(Libraries.isLibrary(new File("OptiFine_1.8.9_HD_U_M5.jar")));
        assertFalse(Libraries.isLibrary(new File("asm-9.11.1.txt")));
    }

    @Test
    void pack200UnpacksFromStreamsThatReportNothingAvailable() throws Exception {
        ByteArrayOutputStream jar = new ByteArrayOutputStream();
        try (JarOutputStream out = new JarOutputStream(jar)) {
            byte[] bytes = classFile();
            JarEntry stored = new JarEntry("a/B.class");
            java.util.zip.CRC32 crc = new java.util.zip.CRC32();
            crc.update(bytes);
            stored.setMethod(JarEntry.STORED);
            stored.setSize(bytes.length);
            stored.setCompressedSize(bytes.length);
            stored.setCrc(crc.getValue());
            out.putNextEntry(stored);
            out.write(bytes);
            out.closeEntry();
        }
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        org.apache.commons.compress.java.util.jar.Pack200.newPacker()
                .pack(new JarInputStream(new ByteArrayInputStream(jar.toByteArray())), packed);

        InputStream noAvailable = new FilterInputStream(new ByteArrayInputStream(packed.toByteArray())) {
            @Override
            public int available() {
                return 0;
            }
        };
        ByteArrayOutputStream unpacked = new ByteArrayOutputStream();
        Pack200.newUnpacker().unpack(noAvailable, new JarOutputStream(unpacked));
        try (JarInputStream in = new JarInputStream(new ByteArrayInputStream(unpacked.toByteArray()))) {
            JarEntry entry = in.getNextJarEntry();
            assertNotNull(entry);
            assertEquals("a/B.class", entry.getName());
        }
    }

    private static byte[] classFile() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "a/B", null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static Object read(Class<?> owner, String name, Object target) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
