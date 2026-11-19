<div align="center">

# lol.trq.lazarus

A runtime for Minecraft Forge 1.8.9 on modern Java, with optional LWJGL 3 and security patches.

[![CI](https://github.com/trq-lol/lol.trq.lazarus/actions/workflows/ci.yml/badge.svg)](https://github.com/trq-lol/lol.trq.lazarus/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/trq-lol/lol.trq.lazarus)](https://github.com/trq-lol/lol.trq.lazarus/releases)
[![Java](https://img.shields.io/badge/java-17%20%7C%2021%20%7C%2025-informational)](docs/compatibility.md)
[![License](https://img.shields.io/badge/license-LGPL--3.0--or--later-blue)](COPYING.LESSER)

</div>

Forge 1.8.9 was written for Java 8. On anything newer it crashes before the window even opens. Lazarus is one jar
that fixes this from the outside. Minecraft, Forge and your mods stay exactly as they are, and OptiFine still works.

You get a current JVM with its garbage collectors, TLS and security updates, an optional LWJGL 3 backend that runs
natively on Apple silicon, and fixes for two security holes: one in the game's old Log4j, one in modern JVMs.

| | Java 17 | Java 21 | Java 25 |
|---|:---:|:---:|:---:|
| Forge 1.8.9 to the main menu | ✔ | ✔ | ✔ |
| With OptiFine HD U M5 and a Mixin 0.7 mod | ✔ | ✔ | ✔ |
| Singleplayer world | ✔ | ✔ | ✔ |
| JVM warnings | none | none | none |

CI boots real Forge 1.8.9 on every push: Java 17, 21 and 25, on LWJGL 2 and LWJGL 3.
[docs/compatibility.md](docs/compatibility.md) lists exactly what is tested and what isn't.

## Install

Install Forge 1.8.9 (11.15.1.2318) first, with Forge's own installer.

### Official Minecraft Launcher

1. Download `lol.trq.lazarus-<version>.jar` from [releases](https://github.com/trq-lol/lol.trq.lazarus/releases).
2. Open it (double-click, or `java -jar lol.trq.lazarus-<version>.jar`). Any Java 17 or newer will do.
3. Click OK. The installer finds your `.minecraft` folder and your Forge 1.8.9 version on its own.
4. In the launcher, pick **Forge 1.8.9 (Lazarus)** and play. The launcher downloads the Java runtime it needs.

The installer only adds files: one library, one version that inherits from your Forge version, and one profile. It
backs up `launcher_profiles.json` before touching it, and your plain Forge profile keeps working on Java 8.

Prefer a terminal? `java -jar lol.trq.lazarus-<version>.jar install [--dir <.minecraft>] [--forge <id>] [--lwjgl3]`

### Prism Launcher and MultiMC

1. Create a Minecraft 1.8.9 instance with Forge 11.15.1.2318.
2. Put `lol.trq.lazarus-<version>.jar` in the instance's `libraries` folder (create it if it's missing).
3. Save the output of `java -jar lol.trq.lazarus-<version>.jar prism` as `lol.trq.lazarus.json`, then add it with
   **Edit instance → Version → Import components**.
4. Under **Settings → Java**, pick Java 17 or newer and turn on **Skip Java compatibility checks**.

Other launchers: [docs/launchers.md](docs/launchers.md) has the details, including what a launcher needs to do to
support Lazarus.

## LWJGL 3

Tick the box in the installer (or pass `--lwjgl3`) to get a second profile that runs on LWJGL 3 instead of 1.8.9's
LWJGL 2. That means GLFW for the window and input, correct high-DPI scaling, and native Apple silicon. On Apple
silicon Macs this profile is selected by default. [docs/lwjgl3.md](docs/lwjgl3.md) covers what changes.

## How it works

The jar has three parts.

**Entrypoint.** `lol.trq.lazarus.launch.Lazarus` replaces LaunchWrapper's `Launch`, which casts the system class
loader to `URLClassLoader` and so fails on Java 9 and later. Lazarus builds the same `LaunchClassLoader` and runs the
tweaker chain under LaunchWrapper's contract, so FML, Mixin and OptiFine can't tell the difference.

**Agent.** It opens the JDK to the game's class loaders, so the game (and nothing else) gets Java 8's reflection
rules. It also patches classes as they load: Pack200, ASM 5 behaviour on ASM 9, `static final` writes and enum
creation without `sun.reflect`, no `SecurityManager`, appends to the system class path, and Mixin 0.7's private copy
of ASM 5. [docs/how-it-works.md](docs/how-it-works.md) lists every patch and the JDK change behind it.

**Bootstrap.** If a launcher starts Lazarus without the agent, or puts Forge's ASM 5 ahead of ASM 9, Lazarus
relaunches the JVM once with the right setup. All a launcher has to do is set the main class and add the jar.

The jar bundles ASM 9 plus relocated copies of Apache Commons Compress and Gson. See [NOTICE](NOTICE.md).

## Security

**Log4Shell.** Minecraft 1.8.9 ships Log4j 2.0-beta9. Lazarus stops it interpolating messages and makes JNDI lookups
resolve to nothing. Every CI run logs a real exploit string and checks that nothing happens.

**Java4Shell** (CVE-2026-114938) is a bug in modern JVMs that Java 8 never had. Lazarus always turns off the
affected Base64 intrinsic.

Details in [docs/security.md](docs/security.md).

## Troubleshooting

Start with [docs/troubleshooting.md](docs/troubleshooting.md). Add `-Dlazarus.debug=true` to the JVM arguments to log
every patch Lazarus applies.

## Building

```
./gradlew spotlessApply build
```

Gradle downloads the JDKs it needs.

| Project | What it is |
|---------|------------|
| `runtime` | The jar you install: entrypoint, agent, patches, installer |
| `lwjgl3` | The LWJGL 2 API on top of LWJGL 3, bundled inside the runtime jar |
| `harness` | A dev launcher that downloads Minecraft 1.8.9 and Forge and boots them on any JDK |
| `smoke` | A test mod that reports from the main menu (and optionally a world), then exits |
| `smoke-mixin` | A test mod that bundles Mixin 0.7.11 and injects into the main menu |

To boot the game through Lazarus (the window starts muted and closes itself):

```
./gradlew :harness:runLazarus25 -Pmixin                        # Forge and a Mixin 0.7 mod on Java 25
./gradlew :harness:runLazarus21 -Poptifine=/path/OptiFine.jar  # with OptiFine (bring your own copy)
./gradlew :harness:runBare25                                   # worst case: no agent, stock class path
./gradlew :harness:runStock8                                   # stock LaunchWrapper on Java 8, for comparison
./gradlew :harness:runLwjgl3_25 -Pmixin                        # on LWJGL 3
./gradlew :harness:runBareLwjgl3_25                            # LWJGL 3, no agent, stock class path
```

Add `-PjvmArgs="-Dlazarus.smoke.world=true -Dlazarus.smoke.screenshot=true"` to also load a world and save a
screenshot to `harness/build/run/<task>/screenshots`.

## For AI agents

[CLAUDE.md](CLAUDE.md) has the mental model, the patterns that matter and the traps. [llms.txt](llms.txt) indexes
the files.

## Credits and prior art

Lazarus is written from scratch, but these projects worked through the same problems first and showed what breaks
and why:

- [GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify): Forge 1.7.10 on Java 17+ with LWJGL 3
- [CleanroomMC/Cleanroom](https://github.com/CleanroomMC/Cleanroom): Forge 1.12.2 on Java 21
- [moehreag/legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) and
  [Zarzelcow/legacy-lwjgl3](https://github.com/Zarzelcow/legacy-lwjgl3): LWJGL 3 on legacy Fabric and Ornithe
- [gudenau/MC-LWJGL3](https://github.com/gudenau/MC-LWJGL3): an early (2017) Forge 1.12 coremod that put LWJGL 3
  under the game by remapping every `org.lwjgl` reference to its own compatibility classes

## License

[LGPL-3.0-or-later](COPYING.LESSER), which supplements the [GPL-3.0](COPYING). See [DISCLAIMER](DISCLAIMER.md) and
[NOTICE](NOTICE.md).
