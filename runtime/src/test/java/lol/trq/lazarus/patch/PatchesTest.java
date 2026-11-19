/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class PatchesTest {
    /** Hooks the patched test classes call. Public so the defined classes can link to them. */
    public static final class Hooks {
        public static String twiceObject(Object o) {
            return String.valueOf(o) + o;
        }

        public static int length(String s) {
            return -s.length();
        }

        public static boolean isJar(File file) {
            return file.getName().endsWith(".jar");
        }

        public static boolean isModern(String version) {
            return version.equals("17");
        }

        public static String replaced(Class<?> owner, String arg) {
            return owner.getSimpleName() + ":" + arg;
        }
    }

    static final String HOOKS = Type.getInternalName(Hooks.class);

    @Test
    void asmApiLevelRaisesVisitorApi() throws Exception {
        String name = "test/LegacyVisitor";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "org/objectweb/asm/ClassVisitor", null);
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitLdcInsn(AsmApiPatch.ASM5);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/objectweb/asm/ClassVisitor", "<init>", "(I)V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        cw.visitEnd();

        ClassPatch patch = new AsmApiPatch(Set.of("test/"));
        assertTrue(patch.matches(name));
        byte[] patched = patch.apply(name, cw.toByteArray());
        Object visitor =
                new Bytecode.Loader().define(name, patched).getConstructor().newInstance();
        Field api = ClassVisitor.class.getDeclaredField("api");
        api.setAccessible(true);
        assertEquals(Opcodes.ASM9, api.getInt(visitor));
    }

    @Test
    void asmApiLevelLeavesClassesWithoutAsmAlone() {
        byte[] bytes = Bytecode.staticMethod("test/Plain", "java/lang/Object", "f", "()I", mv -> {
            mv.visitLdcInsn(AsmApiPatch.ASM5);
            mv.visitInsn(Opcodes.IRETURN);
        });
        assertSame(bytes, new AsmApiPatch(Set.of("test/")).apply("test/Plain", bytes));
    }

    @Test
    void redirectStaticCall() throws Exception {
        String name = "test/CallsValueOf";
        byte[] bytes =
                Bytecode.staticMethod(name, "java/lang/Object", "f", "(Ljava/lang/String;)Ljava/lang/String;", mv -> {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "java/lang/String",
                            "valueOf",
                            "(Ljava/lang/Object;)Ljava/lang/String;",
                            false);
                    mv.visitInsn(Opcodes.ARETURN);
                });
        RedirectPatch patch = new RedirectPatch(
                "t",
                Set.of(name),
                Opcodes.INVOKESTATIC,
                "java/lang/String",
                "valueOf",
                "(Ljava/lang/Object;)Ljava/lang/String;",
                HOOKS,
                "twiceObject");
        Method f = new Bytecode.Loader().define(name, patch.apply(name, bytes)).getMethod("f", String.class);
        assertEquals("abab", f.invoke(null, "ab"));
    }

    @Test
    void redirectLeavesClassesWithoutTheCallAlone() {
        String name = "test/NoCall";
        byte[] bytes = Bytecode.staticMethod(name, "java/lang/Object", "f", "()V", mv -> mv.visitInsn(Opcodes.RETURN));
        RedirectPatch patch = new RedirectPatch(
                "t", Set.of(name), Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "()V", HOOKS, "x");
        assertSame(bytes, patch.apply(name, bytes));
    }

    @Test
    void redirectVirtualCallPassesReceiver() throws Exception {
        String name = "test/CallsLength";
        byte[] bytes = Bytecode.staticMethod(name, "java/lang/Object", "f", "(Ljava/lang/String;)I", mv -> {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", false);
            mv.visitInsn(Opcodes.IRETURN);
        });
        byte[] patched = new RedirectPatch(
                        "t", Set.of(name), Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I", HOOKS, "length")
                .apply(name, bytes);
        Method f = new Bytecode.Loader().define(name, patched).getMethod("f", String.class);
        assertEquals(-5, f.invoke(null, "hello"));
    }

    @Test
    void shortCircuitReturnsTrueWhenHookAgrees() throws Exception {
        String name = "test/IsLibrary";
        byte[] bytes = Bytecode.staticMethod(name, "java/lang/Object", "isLibrary", "(Ljava/io/File;)Z", mv -> {
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitInsn(Opcodes.IRETURN);
        });
        byte[] patched =
                new ShortCircuitPatch("t", name, "isLibrary", "(Ljava/io/File;)Z", HOOKS, "isJar").apply(name, bytes);
        Method m = new Bytecode.Loader().define(name, patched).getMethod("isLibrary", File.class);
        assertEquals(true, m.invoke(null, new File("asm-9.jar")));
        assertEquals(false, m.invoke(null, new File("notes.txt")));
    }

    @Test
    void nullFallbackOnlyAppliesWhenOriginalReturnsNullAndHookAgrees() throws Exception {
        String name = "test/Versions";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
            @Override
            protected String getCommonSuperClass(String a, String b) {
                return "java/lang/Object";
            }
        };
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        cw.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL,
                "NEWEST",
                "Ljava/lang/String;",
                null,
                "1.8");
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC,
                "get", "(Ljava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        Label none = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("1.7");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
        mv.visitJumpInsn(Opcodes.IFEQ, none);
        mv.visitLdcInsn("1.7");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(none);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();

        byte[] patched = new NullFallbackPatch(
                        "t", name, "get", "(Ljava/lang/String;)Ljava/lang/String;", HOOKS, "isModern", "NEWEST")
                .apply(name, cw.toByteArray());
        Method get = new Bytecode.Loader().define(name, patched).getMethod("get", String.class);
        assertEquals("1.7", get.invoke(null, "1.7"));
        assertEquals("1.8", get.invoke(null, "17"));
        assertNull(get.invoke(null, "banana"));
    }

    @Test
    void replaceBodyCallsHookWithOwner() throws Exception {
        String name = "test/Replaced";
        byte[] bytes =
                Bytecode.staticMethod(name, "java/lang/Object", "f", "(Ljava/lang/String;)Ljava/lang/String;", mv -> {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    mv.visitInsn(Opcodes.ARETURN);
                });
        byte[] patched = new ReplaceBodyPatch(
                        "t", name, "f", "(Ljava/lang/String;)Ljava/lang/String;", HOOKS, "replaced")
                .apply(name, bytes);
        Method f = new Bytecode.Loader().define(name, patched).getMethod("f", String.class);
        assertEquals("Replaced:x", f.invoke(null, "x"));
    }

    @Test
    void nullReturnSkipsTheBody() throws Exception {
        String name = "test/Lookup";
        byte[] bytes = Bytecode.staticMethod(
                name, "java/lang/Object", "lookup", "(Ljava/lang/String;)Ljava/lang/String;", mv -> {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitInsn(Opcodes.ARETURN);
                });
        byte[] patched = new NullReturnPatch("t", name, "lookup").apply(name, bytes);
        Method lookup = new Bytecode.Loader().define(name, patched).getMethod("lookup", String.class);
        assertNull(lookup.invoke(null, "ldap://example/x"));
    }

    @Test
    void log4shellPatchesTargetLog4j() {
        assertTrue(Patches.all().stream()
                .anyMatch(p -> p.matches("org/apache/logging/log4j/core/pattern/MessagePatternConverter")));
        assertTrue(Patches.all().stream().anyMatch(p -> p.matches("org/apache/logging/log4j/core/lookup/JndiLookup")));
    }

    @Test
    void remapRewritesTypeReferences() {
        String name = "test/UsesPack200";
        byte[] bytes = Bytecode.staticMethod(name, "java/lang/Object", "f", "()Ljava/lang/Object;", mv -> {
            mv.visitTypeInsn(Opcodes.NEW, "java/util/jar/Pack200");
            mv.visitInsn(Opcodes.ARETURN);
        });
        byte[] patched =
                new RemapPatch("t", Set.of(name), Map.of("java/util/jar/Pack200", "x/Pack200")).apply(name, bytes);
        assertTrue(references(patched).contains("x/Pack200"));
        assertFalse(references(patched).contains("java/util/jar/Pack200"));
    }

    @Test
    void relocatedAsmPointsAtRealAsm() {
        String name = "com/example/shadow/org/spongepowered/asm/mixin/Thing";
        byte[] bytes = Bytecode.staticMethod(name, "java/lang/Object", "f", "()Ljava/lang/Object;", mv -> {
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitTypeInsn(Opcodes.CHECKCAST, "com/example/shadow/org/spongepowered/asm/lib/Type");
            mv.visitInsn(Opcodes.ARETURN);
        });
        RelocatedAsmPatch patch = new RelocatedAsmPatch("t", "org/spongepowered/", "org/spongepowered/asm/lib/");
        assertTrue(patch.matches(name));
        assertFalse(patch.matches("org/spongepowered/asm/lib/ClassReader"));
        List<String> refs = references(patch.apply(name, bytes));
        assertTrue(refs.contains("org/objectweb/asm/Type"), refs.toString());
    }

    @Test
    void suffixPatchMatchesRelocatedCopies() {
        ClassPatch inner = new RemapPatch("t", Set.of(), Map.of());
        SuffixPatch patch = new SuffixPatch(inner, "org/spongepowered/asm/service/mojang/MixinServiceLaunchWrapper");
        assertTrue(patch.matches("org/spongepowered/asm/service/mojang/MixinServiceLaunchWrapper"));
        assertTrue(patch.matches("a/b/org/spongepowered/asm/service/mojang/MixinServiceLaunchWrapper"));
        assertFalse(patch.matches("org/spongepowered/asm/service/mojang/Other"));
    }

    @Test
    void everyPatchHasANameAndATarget() {
        for (ClassPatch patch : Patches.all()) {
            assertFalse(patch.name().isBlank());
        }
        assertTrue(Patches.all().stream()
                .anyMatch(p -> p.matches("net/minecraftforge/fml/common/patcher/ClassPatchManager")));
        assertTrue(Patches.all().stream().anyMatch(p -> p.matches("net/minecraftforge/common/util/EnumHelper")));
    }

    @Test
    void apacheCommonsNamesAreNotRelocated() {
        assertEquals("org/apache/commons/lang3/JavaVersion", Patches.apacheCommons("lang3/JavaVersion"));
    }

    @Test
    void containsFindsNeedle() {
        byte[] hay = "abcorg/objectweb/asm/xyz".getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(Bytes.contains(hay, "org/objectweb/asm/".getBytes(StandardCharsets.ISO_8859_1)));
        assertFalse(Bytes.contains(hay, "org/spongepowered".getBytes(StandardCharsets.ISO_8859_1)));
        assertArrayEquals(new byte[0], new byte[0]);
    }

    static List<String> references(byte[] bytes) {
        java.util.List<String> out = new java.util.ArrayList<>();
        new ClassReader(bytes)
                .accept(
                        new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitTypeInsn(int opcode, String type) {
                                        out.add(type);
                                    }
                                };
                            }
                        },
                        0);
        return out;
    }
}
