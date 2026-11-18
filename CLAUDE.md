# CLAUDE.md: working in lol.trq.lazarus

One jar that boots stock Minecraft Forge 1.8.9 on Java 17 to 25: a replacement launch entrypoint, a Java agent that
patches classes as they load, a bootstrap that repairs the JVM setup, and an installer. For the file index, see
[llms.txt](llms.txt); for the full design, [docs/how-it-works.md](docs/how-it-works.md).

## The 30-second mental model

```
java [-javaagent:lazarus.jar] -cp lazarus.jar:<forge libs>:<mc> lol.trq.lazarus.launch.Lazarus <launchwrapper args>
  LazarusAgent.premain        open the JDK to the app loader's unnamed module; register PatchingTransformer
  Lazarus.main
    Bootstrap                 no agent, or asm-all ahead of ASM 9?  -> relaunch once via @argfile, exit with child
    LinuxNatives              weaken liblwjgl64.so's SUNWprivate_1.1 requirement (copy, never in place)
    LaunchClassLoader         from java.class.path; JDK opened to it too
    tweaker chain             LaunchWrapper's exact contract: FMLTweaker -> coremods -> mod-dir tweakers
    launch target             net.minecraft.client.main.Main
```

Patches (`runtime/src/main/java/lol/trq/lazarus/patch/Patches.java`) call static hooks in `lol.trq.lazarus.compat`.

## The patterns that matter

- **Fix from outside.** Patch at load time through the agent. Never ship modified Forge, FML, LaunchWrapper,
  Minecraft or OptiFine files, and never modify the launcher's files (natives are copied, not patched in place).
- **One patch per JDK or library change, scoped to the classes that need it.** Add it to `Patches.all()` with a
  javadoc naming the change, a row in `docs/how-it-works.md`, and a test in `PatchesTest` on synthetic bytecode.
- **Hooks live in `compat` and take only JDK types.** Patched Forge code calls them across the
  `LaunchClassLoader` boundary; Lazarus's packages are excluded from that loader so they load once.
- **Owner names under `org/apache/commons/` go through `Patches.apacheCommons`.** The shadow relocation rewrites
  matching string constants, which would retarget a patch at Lazarus's own copy.
- **Prove it in the game.** Unit tests are not enough for a behaviour change: run the harness
  (`./gradlew :harness:runLazarus25 -Pmixin`, add `-Poptifine=<jar>` and
  `-PjvmArgs=-Dlazarus.smoke.world=true`) and check the `LAZARUS-SMOKE OK` and `LAZARUS-MIXIN OK` lines.
- **OptiFine compatibility is the acceptance test.** OptiFine cannot be redistributed, so it is tested locally only.

## Pitfalls to avoid

- FML loads its launch classes through the application class loader, where LaunchWrapper transformers never look;
  only the agent reaches them.
- FML scans every class path jar for mods. A jar Lazarus adds must match `compat.Libraries.PREFIXES`.
- Mixin 0.7 bundles ASM 5 under `org/spongepowered/asm/lib`; mods may relocate it again. Match by path fragment.
- The official launcher splits `javaArgs` on spaces. Never write a path with whitespace into it.
- An unknown `--` JVM flag stops older JDKs from starting; pair new flags with `-XX:+IgnoreUnrecognizedVMOptions`.
- Running the game opens a window. The harness mutes it and exits on its own; do not launch the game from an agent
  without the owner's go-ahead.

## Conventions

- Commits use Conventional Commits. **You own every commit, including code an AI wrote. Never add Co-Authored-By or
  any other AI-attribution trailer.**
- Never name a consuming application in code, docs, comments or logs.
- Copied code is recorded in `NOTICE.md` with its license.
