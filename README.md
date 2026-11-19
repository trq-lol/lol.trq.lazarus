<div align="center">

# lol.trq.lazarus

A runtime for Minecraft Forge 1.8.9 on modern Java, with optional LWJGL 3 and security patches.

[![CI](https://github.com/trq-lol/lol.trq.lazarus/actions/workflows/ci.yml/badge.svg)](https://github.com/trq-lol/lol.trq.lazarus/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/trq-lol/lol.trq.lazarus)](https://github.com/trq-lol/lol.trq.lazarus/releases)
[![Java](https://img.shields.io/badge/java-17%20%7C%2021%20%7C%2025-informational)](docs/compatibility.md)
[![License](https://img.shields.io/badge/license-LGPL--3.0--or--later-blue)](COPYING.LESSER)

</div>

Forge 1.8.9 was written for Java 8, and on anything newer it crashes before the window opens. Lazarus is a single
jar that runs it on Java 17, 21 and 25 without changing Minecraft, Forge or your mods. OptiFine and Mixin 0.7 mods
keep working.

- **A modern JVM.** Current garbage collectors, TLS and security updates, with the same game and mods.
- **LWJGL 3.** Native Apple silicon, GLFW windowing and input, and correct high-DPI scaling.
- **Modern OpenGL on macOS.** An OpenGL 4.1 core renderer that runs OptiFine shader packs Apple's legacy OpenGL
  cannot, and runs them faster.
- **Security fixes.** Log4Shell in the game's bundled Log4j and Java4Shell in modern JVMs are both closed.

| | Java 17 | Java 21 | Java 25 |
|---|:---:|:---:|:---:|
| Forge 1.8.9 to the main menu | ✔ | ✔ | ✔ |
| With OptiFine HD U M5 and a Mixin 0.7 mod | ✔ | ✔ | ✔ |
| Singleplayer world | ✔ | ✔ | ✔ |
| JVM warnings | none | none | none |

Every push boots real Forge 1.8.9 in CI on Java 17, 21 and 25, on LWJGL 2, LWJGL 3 and the core renderer. See
[compatibility](docs/compatibility.md) for the full matrix.

## Install

Install Forge 1.8.9 (11.15.1.2318) first, with Forge's own installer.

### Official Minecraft Launcher

1. Download `lol.trq.lazarus-<version>.jar` from [releases](https://github.com/trq-lol/lol.trq.lazarus/releases).
2. Open it (double-click, or `java -jar lol.trq.lazarus-<version>.jar`). Any Java 17 or newer works.
3. Click OK. The installer finds your `.minecraft` folder and your Forge 1.8.9 version.
4. In the launcher, pick **Forge 1.8.9 (Lazarus)** and play. The launcher downloads the Java runtime it needs.

The installer only adds files: one library, one version that inherits from your Forge version, and one profile. It
backs up `launcher_profiles.json` first, and your plain Forge profile keeps working on Java 8.

From a terminal: `java -jar lol.trq.lazarus-<version>.jar install [--dir <.minecraft>] [--forge <id>] [--lwjgl3]`

### Prism Launcher and MultiMC

1. Create a Minecraft 1.8.9 instance with Forge 11.15.1.2318.
2. Put `lol.trq.lazarus-<version>.jar` in the instance's `libraries` folder.
3. Save the output of `java -jar lol.trq.lazarus-<version>.jar prism` as `lol.trq.lazarus.json` and add it with
   **Edit instance → Version → Import components**.
4. Under **Settings → Java**, pick Java 17 or newer and turn on **Skip Java compatibility checks**.

Other launchers, and what a launcher needs to support Lazarus: [launchers](docs/launchers.md).

## LWJGL 3

Tick **LWJGL 3** in the installer (or pass `--lwjgl3`) for a second profile that runs on LWJGL 3 instead of 1.8.9's
LWJGL 2. It is the default on Apple silicon Macs. See [LWJGL 3](docs/lwjgl3.md).

## Core renderer and shader packs

With LWJGL 3, add `-Dlazarus.gl=core` to the JVM arguments to run the game on an OpenGL 4.1 core profile. Lazarus
emulates the fixed-function pipeline the game was written for and translates its GLSL 1.20 shaders, so vanilla,
OptiFine and shader packs all work. On macOS, where the legacy path is stuck on OpenGL 2.1:

| On an Apple M2 | Legacy OpenGL 2.1 | Core renderer |
|---|---|---|
| OptiFine | 223 fps | 528 fps |
| BSL shaders | 26 fps, no shadows | 81 fps, with shadows |
| Complementary Reimagined | does not load | runs in full |

See [core renderer](docs/core-renderer.md) and [benchmarks](docs/benchmarks.md).

## How it works

**Entrypoint.** `lol.trq.lazarus.launch.Lazarus` replaces LaunchWrapper's `Launch`, which casts the system class
loader to `URLClassLoader` and fails on Java 9 and later. Lazarus builds the same `LaunchClassLoader` and runs the
tweaker chain under LaunchWrapper's contract, so FML, Mixin and OptiFine can't tell the difference.

**Agent.** A Java agent opens the JDK to the class path and the game's class loader, giving the game Java 8's
reflection rules, and patches classes as they load: Pack200, ASM 5 behaviour on ASM 9, `static final` writes and
enum creation without `sun.reflect`, no `SecurityManager`, system class path appends, and Mixin 0.7's private ASM 5.

**Bootstrap.** If a launcher starts Lazarus without the agent, or puts Forge's ASM 5 ahead of ASM 9, Lazarus
relaunches the JVM once with the right setup. A launcher only needs to set the main class and add the jar.

Every patch and the JDK change behind it: [how it works](docs/how-it-works.md). The jar bundles ASM 9 and relocated
copies of Apache Commons Compress and Gson; see [NOTICE](NOTICE.md).

## Security

**Log4Shell.** Minecraft 1.8.9 ships Log4j 2.0-beta9. Lazarus stops it interpolating log messages and makes JNDI
lookups resolve to nothing. Every CI run logs a real exploit string and checks that nothing happens.

**Java4Shell** (CVE-2026-114938) is a bug in modern JVMs that Java 8 never had. Lazarus always turns off the
affected Base64 intrinsic.

Details: [security](docs/security.md).

## Troubleshooting

See [troubleshooting](docs/troubleshooting.md). `-Dlazarus.debug=true` logs every patch Lazarus applies.

## Building

```
./gradlew spotlessApply build
```

Gradle downloads the JDKs it needs.

| Project | What it is |
|---------|------------|
| `runtime` | The jar you install: entrypoint, agent, patches, installer |
| `lwjgl3` | The LWJGL 2 API on LWJGL 3 and the core renderer, bundled inside the runtime jar |
| `harness` | A development launcher that downloads Minecraft 1.8.9 and Forge and boots them on any JDK |
| `smoke` | A test mod that checks the menu and a world, takes screenshots and runs benchmarks |
| `smoke-mixin` | A test mod that bundles Mixin 0.7.11 and injects into the main menu |

Boot the game through Lazarus (the window starts muted and closes itself):

```
./gradlew :harness:runLazarus25 -Pmixin                        # Forge and a Mixin 0.7 mod on Java 25
./gradlew :harness:runLazarus21 -Poptifine=/path/OptiFine.jar  # with OptiFine (bring your own copy)
./gradlew :harness:runLwjgl3_25 -PjvmArgs=-Dlazarus.gl=core     # LWJGL 3 on the core renderer
./gradlew :harness:runBare25                                   # no agent, stock class path
./gradlew :harness:runStock8                                   # stock LaunchWrapper on Java 8, for comparison
```

`-PjvmArgs="-Dlazarus.smoke.world=true -Dlazarus.smoke.screenshot=true"` also loads a world and saves a screenshot to
`harness/build/run/<task>/screenshots`. `-PshaderPack=<zip>` installs and selects a shader pack.

## For AI agents

[CLAUDE.md](CLAUDE.md) has the mental model, the patterns that matter and the traps. [llms.txt](llms.txt) indexes the
files.

## Credits and prior art

Lazarus is written from scratch. These projects worked through the same problems first and showed what breaks and
why:

- [GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify): Forge 1.7.10 on Java 17+ with LWJGL 3
- [CleanroomMC/Cleanroom](https://github.com/CleanroomMC/Cleanroom): Forge 1.12.2 on Java 21
- [moehreag/legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) and
  [Zarzelcow/legacy-lwjgl3](https://github.com/Zarzelcow/legacy-lwjgl3): LWJGL 3 on legacy Fabric and Ornithe
- [gudenau/MC-LWJGL3](https://github.com/gudenau/MC-LWJGL3): a 2017 Forge 1.12 coremod that put LWJGL 3 under the
  game by remapping every `org.lwjgl` reference

## License

[LGPL-3.0-or-later](COPYING.LESSER), which supplements the [GPL-3.0](COPYING). See [DISCLAIMER](DISCLAIMER.md) and
[NOTICE](NOTICE.md).
