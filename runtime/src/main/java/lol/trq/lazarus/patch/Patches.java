/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.patch;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

/** The patch set. Each entry documents the JDK change it works around. */
public final class Patches {
    /** The Lazarus stand-in for {@code java.util.jar.Pack200}. */
    static final String PACK200 = "lol/trq/lazarus/compat/Pack200";

    private Patches() {}

    /** {@return every patch} */
    public static List<ClassPatch> all() {
        List<ClassPatch> patches = new java.util.ArrayList<>(base());
        patches.addAll(Lwjgl3Patches.all());
        ClassPatch coreGl = CoreGlPatch.create();
        if (coreGl != null) {
            patches.add(coreGl);
        }
        return List.copyOf(patches);
    }

    private static List<ClassPatch> base() {
        return List.of(
                mixinModernAsm(),
                asmApiLevel(),
                pack200(),
                lenientAsmTypes(),
                nullSafeSuperType(),
                launcherLibraries(),
                commonsLangJavaVersion(),
                holderRefs("net/minecraftforge/fml/common/registry/ObjectHolderRef"),
                holderRefs("net/minecraftforge/fml/common/registry/ItemStackHolderRef"),
                enumHelperSetup(),
                enumHelperFieldWrites(),
                securityManager(),
                systemClassPath(),
                mixinClassBytes(),
                log4jMessageLookups(),
                log4jJndi());
    }

    /**
     * {@code java.util.jar.Pack200} was removed in Java 14. FML's {@code ClassPatchManager} uses it to unpack the
     * binary patches it applies to Minecraft; point it at the Lazarus stand-in.
     */
    static ClassPatch pack200() {
        return new RemapPatch(
                "pack200",
                Set.of("net/minecraftforge/fml/common/patcher/ClassPatchManager"),
                Map.of("java/util/jar/Pack200", PACK200, "java/util/jar/Pack200$Unpacker", PACK200 + "$Unpacker"));
    }

    /**
     * ASM 7 and later reject an internal name passed to {@code Type.getType(String)}; ASM 5 accepted it. FML passes
     * internal names in its event and mod-discovery code.
     */
    static ClassPatch lenientAsmTypes() {
        return new RedirectPatch(
                "lenient-asm-types",
                Set.of(
                        "net/minecraftforge/fml/common/ModContainerFactory",
                        "net/minecraftforge/fml/common/asm/ASMTransformerWrapper",
                        "net/minecraftforge/fml/common/asm/transformers/EventSubscriptionTransformer",
                        "net/minecraftforge/fml/common/discovery/asm/ASMModParser",
                        "net/minecraftforge/fml/common/discovery/asm/ModAnnotation"),
                Opcodes.INVOKESTATIC,
                "org/objectweb/asm/Type",
                "getType",
                "(Ljava/lang/String;)Lorg/objectweb/asm/Type;",
                "lol/trq/lazarus/compat/LegacyAsm",
                "getType");
    }

    /**
     * Legacy ASM users build visitors at api level ASM4 or ASM5, which throw on Java 9+ class features under ASM 9.
     * Covers Forge and FML, Mixin 0.7, and OptiFine's transformer.
     */
    static ClassPatch asmApiLevel() {
        return new AsmApiPatch(Set.of("net/minecraftforge/", "org/spongepowered/", "optifine/"));
    }

    /** FML reads the super type of every class it scans; {@code module-info} has none. */
    static ClassPatch nullSafeSuperType() {
        return new RedirectPatch(
                "null-safe-super-type",
                Set.of("net/minecraftforge/fml/common/discovery/asm/ASMModParser"),
                Opcodes.INVOKEVIRTUAL,
                "org/objectweb/asm/Type",
                "equals",
                "(Ljava/lang/Object;)Z",
                "lol/trq/lazarus/compat/LegacyAsm",
                "typeEquals");
    }

    /**
     * FML scans every class path jar for mods unless its file name matches a hard-coded list of Minecraft's 2015
     * libraries. Extend the list with the libraries Lazarus adds.
     */
    static ClassPatch launcherLibraries() {
        return new ShortCircuitPatch(
                "launcher-libraries",
                "net/minecraftforge/fml/common/ModClassLoader",
                "isDefaultLibrary",
                "(Ljava/io/File;)Z",
                "lol/trq/lazarus/compat/Libraries",
                "isLibrary");
    }

    /**
     * Commons Lang 3.3.2, which Minecraft ships, cannot parse {@code java.specification.version} values without the
     * {@code 1.} prefix, so {@code SystemUtils.JAVA_SPECIFICATION_VERSION_AS_ENUM} is {@code null} and every {@code
     * isJavaVersionAtLeast} check throws. Report Java 9 and newer as the newest version it knows, 1.8.
     */
    static ClassPatch commonsLangJavaVersion() {
        return new NullFallbackPatch(
                "commons-lang-java-version",
                apacheCommons("lang3/JavaVersion"),
                "get",
                "(Ljava/lang/String;)L" + apacheCommons("lang3/JavaVersion") + ";",
                "lol/trq/lazarus/compat/LegacyJava",
                "isModern",
                "JAVA_1_8");
    }

    /**
     * Builds an {@code org/apache/commons/...} internal name at run time. The Lazarus jar relocates its own copies of
     * some Apache Commons libraries, and the relocation rewrites matching string constants; names of the game's
     * copies must not be rewritten.
     */
    static String apacheCommons(String rest) {
        return "org/apache/commons/".concat(rest);
    }

    static final String HOOKS = "lol/trq/lazarus/compat/ForgeHooks";
    static final String ENUM_HELPER = "net/minecraftforge/common/util/EnumHelper";

    /**
     * Forge fills {@code @ObjectHolder} and {@code @ItemStackHolder} fields, which are {@code static final}, through
     * {@code sun.reflect.ReflectionFactory} after clearing {@code Field.modifiers}. Neither works on Java 17.
     */
    static ClassPatch holderRefs(String owner) {
        return new ReplaceBodyPatch(
                "writable-holders", owner, "makeWritable", "(Ljava/lang/reflect/Field;)V", HOOKS, "makeWritable");
    }

    /** {@code EnumHelper} creates enum constants through {@code sun.reflect.ReflectionFactory}. */
    static ClassPatch enumHelperSetup() {
        return new ReplaceBodyPatch("enum-helper-setup", ENUM_HELPER, "setup", "()V", HOOKS, "setupEnumHelper");
    }

    /** {@code EnumHelper} writes {@code static final} fields by clearing {@code Field.modifiers}. */
    static ClassPatch enumHelperFieldWrites() {
        return new ReplaceBodyPatch(
                "enum-helper-field-writes",
                ENUM_HELPER,
                "setFailsafeFieldValue",
                "(Ljava/lang/reflect/Field;Ljava/lang/Object;Ljava/lang/Object;)V",
                HOOKS,
                "setFailsafeFieldValue");
    }

    /** {@code System.setSecurityManager} throws on Java 18 and newer unless enabled, and always on Java 24. */
    static ClassPatch securityManager() {
        return new RedirectPatch(
                "no-security-manager",
                Set.of("net/minecraftforge/fml/common/launcher/FMLTweaker"),
                Opcodes.INVOKESTATIC,
                "java/lang/System",
                "setSecurityManager",
                "(Ljava/lang/SecurityManager;)V",
                HOOKS,
                "setSecurityManager");
    }

    /**
     * FML adds a tweaker mod's jar to the system class loader with reflective {@code URLClassLoader.addURL}, which
     * fails on Java 9 and newer.
     */
    static ClassPatch systemClassPath() {
        return new RedirectPatch(
                "system-class-path",
                Set.of("net/minecraftforge/fml/relauncher/CoreModManager"),
                Opcodes.INVOKEVIRTUAL,
                "java/lang/reflect/Method",
                "invoke",
                "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
                HOOKS,
                "invoke");
    }

    /**
     * Mixin 0.7's LaunchWrapper service casts LaunchWrapper's class loader to {@code URLClassLoader} only to call
     * {@code getResourceAsStream}, which every {@code ClassLoader} has. Matched by suffix to cover relocated copies.
     */
    static ClassPatch mixinClassBytes() {
        String service = "org/spongepowered/asm/service/mojang/MixinServiceLaunchWrapper";
        return new SuffixPatch(
                new RemapPatch(
                        "mixin-class-bytes", Set.of(), Map.of("java/net/URLClassLoader", "java/lang/ClassLoader")),
                service);
    }

    /**
     * Mixin 0.7 reads classes with its own relocated ASM 5, which rejects Java 9+ class files such as the JDK's. Point
     * it at ASM 9. Runs before {@link #asmApiLevel()} so the rewritten visitors get their api level raised too.
     */
    static ClassPatch mixinModernAsm() {
        return new RelocatedAsmPatch("mixin-modern-asm", "org/spongepowered/", "org/spongepowered/asm/lib/");
    }

    /**
     * Log4Shell (CVE-2021-44228, CVE-2021-45046, CVE-2021-45105). Minecraft 1.8.9 ships Log4j 2.0-beta9, which expands
     * {@code ${...}} lookups inside logged messages, such as chat from a server. Stop interpolating messages, as Log4j
     * 2.15+ does.
     */
    static ClassPatch log4jMessageLookups() {
        return new RedirectPatch(
                "log4shell-message-lookups",
                Set.of("org/apache/logging/log4j/core/pattern/MessagePatternConverter"),
                Opcodes.INVOKEVIRTUAL,
                "org/apache/logging/log4j/core/lookup/StrSubstitutor",
                "replace",
                "(Lorg/apache/logging/log4j/core/LogEvent;Ljava/lang/String;)Ljava/lang/String;",
                "lol/trq/lazarus/compat/Log4jHooks",
                "messageWithoutLookups");
    }

    /** Log4Shell, defence in depth: the {@code jndi} lookup resolves to nothing anywhere it is used. */
    static ClassPatch log4jJndi() {
        return new NullReturnPatch("log4shell-jndi", "org/apache/logging/log4j/core/lookup/JndiLookup", "lookup");
    }
}
