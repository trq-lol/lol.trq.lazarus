# lol.trq.lazarus

Runs Minecraft Forge 1.8.9 on Java 17 and newer.

Forge 1.8.9 was built for Java 8. On a modern JVM it fails before the game window opens: LaunchWrapper expects the
system class loader to be a `URLClassLoader`, FML reflects into JDK internals that are now sealed, and Mixin 0.7 does
not understand the module system. Lazarus is a launch runtime that fixes those problems from the outside, without
shipping modified Forge or Minecraft files.

> **Status: pre-alpha.** The build, license and first launch utility are in place. Nothing boots the game yet.
> See [Roadmap](#roadmap).

## Goals

- Boot stock Forge 1.8.9 (11.15.1.x) on Java 17, 21 and 25.
- Keep OptiFine working. Most 1.8.9 players use it, so a runtime that loses it is not useful.
- Patch Forge and FML at runtime only. Users keep the stock Forge jar.
- Ship Mixin 0.8 with MixinExtras for mods that want a modern Mixin.
- Later, optionally: run on LWJGL 3 through an LWJGL 2 compatibility layer (macOS on Apple silicon, HiDPI).

## Roadmap

| Milestone | What it proves |
|-----------|----------------|
| M0 | Forge 1.8.9 + OptiFine reach the main menu on Java 21 with LWJGL 2 |
| M1 | Same on Java 25; FML reflection fixes; Mixin 0.8 + MixinExtras |
| M2 | Launcher integration contract: required JVM flags, class path layout, tweaker |
| M3 | Optional LWJGL 3 backend with an LWJGL 2 compatibility layer |

## Build

```
./gradlew spotlessApply build
```

Requires a JDK 25 toolchain (resolved automatically). Shipped bytecode targets Java 17.

## For AI agents

Read [CLAUDE.md](CLAUDE.md) for the mental model, the patterns that matter and the traps, and [llms.txt](llms.txt)
for a file index.

## Credits and prior art

Lazarus is written from scratch. These projects showed what breaks and why:

- [GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify): Forge 1.7.10 on Java 17+ with LWJGL 3
- [CleanroomMC/Cleanroom](https://github.com/CleanroomMC/Cleanroom): Forge 1.12.2 on Java 21
- [moehreag/legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) and
  [Zarzelcow/legacy-lwjgl3](https://github.com/Zarzelcow/legacy-lwjgl3): LWJGL 3 on legacy Fabric and Ornithe

See [NOTICE](NOTICE.md) for bundled third-party code and its licenses.

## License

[LGPL-3.0-or-later](COPYING.LESSER), which supplements the [GPL-3.0](COPYING). See [DISCLAIMER](DISCLAIMER.md).
