# How Lazarus works

Forge 1.8.9 assumes Java 8 in three ways: it reaches into JDK internals, it relies on JDK APIs that were later
removed, and it relies on ASM 5 behaviour. Lazarus answers each at the point where it breaks, without changing any
file on disk.

## Launch sequence

```
java -javaagent:lol.trq.lazarus.jar -cp lol.trq.lazarus.jar:<forge libs>:<minecraft> lol.trq.lazarus.launch.Lazarus <args>

LazarusAgent.premain
  open every JDK package to the application class loader's unnamed module
  register the patching ClassFileTransformer
Lazarus.main
  Bootstrap: agent missing or ASM 5 first on the class path?  -> relaunch once, corrected, and exit with its status
  build LaunchClassLoader from java.class.path; open the JDK to its unnamed module too
  run the tweaker chain (FMLTweaker -> coremods -> mod-dir tweakers such as OptiFine and Mixin)
  invoke the primary tweaker's launch target (net.minecraft.client.main.Main)
```

## The entrypoint

LaunchWrapper's `net.minecraft.launchwrapper.Launch` starts with
`(URLClassLoader) getClass().getClassLoader()`. Since Java 9 the application class loader is not a
`URLClassLoader`, so it fails immediately.

`lol.trq.lazarus.launch.Lazarus` reads the class path from `java.class.path`, builds the same `LaunchClassLoader`,
and fills the same `Launch.classLoader`, `Launch.minecraftHome`, `Launch.assetsDir` and `Launch.blackboard`
(`TweakClasses`, `ArgumentList`, `Tweaks`). The tweaker loop matches LaunchWrapper: tweakers may queue further
tweakers during `injectIntoClassLoader`, the first tweaker constructed is the primary one, and each tweak class's
package is excluded from transformation.

It also keeps some packages out of `LaunchClassLoader`:

| Package | Why |
|---------|-----|
| `javax.`, `jdk.`, `com.sun.`, `org.w3c.dom.`, `org.xml.sax.`, `org.ietf.jgss.` | On Java 9+ the JDK defines these through the platform class loader, which `LaunchClassLoader` cannot reach on its own |
| `lol.trq.lazarus.{agent,compat,launch,patch,shadow}.` | Patched FML code on both sides of the loader boundary calls into them; they must load once |

## Opening the JDK

Minecraft 1.8.9, Forge and their libraries reflect into JDK internals. Instead of a long list of `--add-opens`
flags, the agent calls `Instrumentation.redefineModule` to open every package of every boot-layer module to the
game's two unnamed modules: the application class loader's and `LaunchClassLoader`'s. Code in named modules is
unaffected.

## Patches

Patches run in the agent's `ClassFileTransformer`, so they reach classes from every loader. That matters because
FML loads its own launch classes through the application class loader, where a LaunchWrapper transformer never
looks. Each patch only touches the classes it names.

| Patch | Classes | JDK or library change | What Lazarus does |
|-------|---------|-----------------------|-------------------|
| `mixin-modern-asm` | Mixin 0.7 (any relocation) | Mixin 0.7 bundles ASM 5, which rejects Java 9+ class files, including the JDK's | Rewrite references from its relocated ASM to ASM 9 |
| `asm-api-level` | `net/minecraftforge/`, `org/spongepowered/`, `optifine/` | ASM 7+ throws when a visitor built for ASM 4/5 meets modules, nest mates, records | Raise the api constant passed to visitor constructors to ASM9 |
| `pack200` | `ClassPatchManager` | `java.util.jar.Pack200` removed in Java 14 | Use a stand-in backed by Apache Commons Compress |
| `lenient-asm-types` | five FML classes | ASM 7+ rejects an internal name passed to `Type.getType` | Accept it, as ASM 5 did |
| `null-safe-super-type` | `ASMModParser` | `module-info` has no superclass | Null-safe comparison |
| `launcher-libraries` | `ModClassLoader.isDefaultLibrary` | FML scans every class path jar not on a hard-coded 2015 list | Treat Lazarus's own libraries as libraries |
| `commons-lang-java-version` | `commons-lang3` 3.3.2 `JavaVersion` | Java 9 dropped the `1.` prefix; lang 3.3.2 cannot parse `17` | Report Java 9+ as 1.8, the newest it knows |
| `writable-holders` | `ObjectHolderRef`, `ItemStackHolderRef` | `sun.reflect.ReflectionFactory` sealed; `Field.modifiers` hidden since 12 | Install a stand-in reflection factory |
| `enum-helper-setup`, `enum-helper-field-writes` | `EnumHelper` | same | Same stand-in; `static final` writes through `Unsafe`, enum constants through `MethodHandles` |
| `no-security-manager` | `FMLTweaker` | `System.setSecurityManager` off by default since 18, removed in 24 | Skip it; FML's exit transformer still stops mods from calling `System.exit` |
| `system-class-path` | `CoreModManager` | the application class loader is not a `URLClassLoader` | Append tweaker jars with `Instrumentation.appendToSystemClassLoaderSearch` |
| `mixin-class-bytes` | Mixin 0.7's `MixinServiceLaunchWrapper` (any relocation) | same | Use `ClassLoader.getResourceAsStream`, which it was all along |

### The reflection stand-in

Forge never links against `sun.reflect.ReflectionFactory`. It looks the factory and a few of its methods up
reflectively, keeps them in static fields, and calls them with `Method.invoke`. Lazarus fills those fields with
`LegacyReflection` and its methods, which have the same shapes:

- `newFieldAccessor(Field, boolean)` returns an accessor whose `set(Object, Object)` writes through `Unsafe`, final
  or not.
- `newConstructorAccessor(Constructor)` returns an accessor whose `newInstance(Object[])` calls a method handle.
  Unlike `Constructor.newInstance`, a method handle may construct an enum constant.

Forge's own code then runs unchanged.

## Libraries in the jar

| Library | How | Why |
|---------|-----|-----|
| ASM 9 | unrelocated | FML, Mixin and OptiFine link against `org.objectweb.asm`; ASM 9 replaces Forge's `asm-all` 5 |
| ASM 6.2.1 `Remapping*Adapter` | source, ported to ASM 9 | FML's deobfuscation transformer extends classes that ASM 7 removed |
| `javax.annotation.Resource` | written from scratch | Mixin 0.7 reads it; it left the JDK in 11 |
| Apache Commons Compress | relocated | Pack200 unpacker |
| Gson | relocated | the installer edits launcher JSON |

## The bootstrap

A launcher that only sets the main class gets a JVM without the agent and with Forge's `asm-all` 5 on the class
path, possibly ahead of ASM 9. `Bootstrap` notices either, and relaunches once with:

- the JVM's own input arguments,
- `-javaagent:<lazarus jar>`,
- the warning flags below,
- the class path with the Lazarus jar first and `asm-all` removed,

all written to a Java `@argfile`, so Windows command line limits do not apply. The parent waits and exits with the
child's status. Set `-Dlazarus.noRelaunch=true` to turn it off.

### JVM flags

```
-XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow
```

LWJGL 2 loads native libraries and uses `sun.misc.Unsafe`, which Java 24+ warns about. The first flag lets Java 17
and 21 ignore the option they do not know.
