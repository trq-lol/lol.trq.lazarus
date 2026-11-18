# CLAUDE.md: working in lol.trq.lazarus

A launch runtime that boots stock Minecraft Forge 1.8.9 on Java 17 to 25. For the file index, see
[llms.txt](llms.txt).

## The 30-second mental model

```
launcher -> java 17..25 -> Lazarus entrypoint (replaces net.minecraft.launchwrapper.Launch)
    -> own LaunchClassLoader built from Classpath.current()   (no URLClassLoader cast)
    -> FML tweaker -> Lazarus coremod patches FML/Forge at class-load time
    -> Mixin 0.8 bootstrap -> Minecraft.main
```

## The patterns that matter

- **Fix from outside.** Patch Forge, FML and LaunchWrapper behaviour at runtime (entrypoint, coremod, class
  transformers). Never ship a modified copy of their files.
- **OptiFine compatibility is the acceptance test.** A change that boots Forge but breaks OptiFine is not done.
- **Bytecode targets Java 17.** That is the oldest JVM Lazarus supports.
- **Write it, do not copy it.** Prior art is credited in the README; copied code must be recorded in `NOTICE.md`.

## Pitfalls to avoid

- FML 1.8.9 bundles ASM 5. It cannot read class files newer than Java 8, so classes FML scans (anything in a mod jar)
  must stay Java 8 bytecode.
- LaunchWrapper always loads `org.lwjgl.` from the parent class loader. Swap LWJGL by changing the class path, not
  with a transformer.
- Do not launch the game client from an agent without the owner's go-ahead.

## Conventions

- Commits use Conventional Commits. **You own every commit, including code an AI wrote. Never add Co-Authored-By or
  any other AI-attribution trailer.**
- Never name a consuming application in code, docs, comments or logs.
