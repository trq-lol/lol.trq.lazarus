# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/) and the
project uses [Semantic Versioning](https://semver.org/).

## [0.4.1] - 2026-11-19

### Fixed

- The core renderer no longer raises GL errors while Forge's splash screen is up. The splash thread draws on the
  window's context while the main thread loads on a shared one, and the two shared one set of cached bindings and one
  stream buffer; each thread now has its own.

## [0.4.0] - 2026-11-19

### Changed

- The core renderer is now the default on the LWJGL 3 profile. `-Dlazarus.gl=compat` selects the legacy renderer.
  The LWJGL 2 profile is unchanged.

## [0.3.0] - 2026-11-19

### Added

- OptiFine shader packs on the core renderer, verified with BSL 10.1.8, Complementary Reimagined r5.10 and
  Sildur's Vibrant 2.02 on macOS, and BSL on Windows. On macOS this is faster
  than the legacy path and runs programs the legacy path cannot compile, shadows included. ARB and EXT extension
  calls that a core profile does not expose are mapped to their core equivalents, OptiFine's extra vertex
  attributes are emulated, and translated shaders get the compatibility alpha test.
- `-PshaderPack=<zip>` in the development harness, and shader failure dumps with `lazarus.gl.debug`.

### Fixed

- The smoke mod no longer calls `System.exit` inside a tick, which could deadlock with Minecraft's shutdown hook.
- Harness screenshots and benchmarks no longer land on the pause menu when the window loses focus.

## [0.2.0] - 2026-11-19

### Added

- An opt-in OpenGL 4.1 core renderer, `-Dlazarus.gl=core` (LWJGL 3 only), the way to modern OpenGL on macOS. It
  emulates the fixed-function pipeline Minecraft, Forge and OptiFine use and translates their GLSL 1.20 shaders to
  GLSL 3.30 core. See [docs/core-renderer.md](docs/core-renderer.md).
- `lol.trq.lazarus.lwjgl3.gl.GlState`, read-only access to the core renderer's matrices and bindings for render
  libraries, answered without driver queries.
- A frame time benchmark in the smoke mod, `-Dlazarus.smoke.bench=<seconds>`. See
  [docs/benchmarks.md](docs/benchmarks.md).
- A shutdown watchdog: if the JVM is still shutting down 20 seconds after it began, Lazarus prints every thread's
  stack and halts it. `-Dlazarus.shutdownGrace=<seconds>` changes the wait.

### Fixed

- The macOS relaunch now also recognises a renamed copy of the LWJGL 3 compatibility jar.
- The relaunch no longer writes the game's program arguments, which include the launcher's session token, to the
  temporary argfile.
- The installer keeps the first `launcher_profiles.json` backup, writes its JSON atomically and rejects version ids
  that are not plain names.

## [0.1.0] - 2026-11-18

First release. Minecraft Forge 1.8.9 (11.15.1.2318) runs on Java 17, 21 and 25, with OptiFine HD U M5 and with mods
that bundle Mixin 0.7.

### Added

- `lol.trq.lazarus.launch.Lazarus`, a replacement for LaunchWrapper's `Launch` that works on Java 9 and newer and
  keeps LaunchWrapper's tweaker contract.
- A Java agent that opens the JDK to the game's class loaders and patches classes as they load:
  - Pack200 for FML's binary patches, through Apache Commons Compress.
  - ASM 5 behaviour on ASM 9: visitor api level, lenient `Type.getType`, null-safe super types, and the
    `Remapping*Adapter` classes ASM 7 removed.
  - `static final` field writes and enum constant creation for `ObjectHolderRef`, `ItemStackHolderRef` and
    `EnumHelper`, without `sun.reflect`.
  - No `SecurityManager` in FML.
  - Tweaker jars appended to the system class path through `Instrumentation`.
  - Commons Lang 3.3.2's `JavaVersion` on Java 9+.
  - Mixin 0.7: its bundled ASM 5 replaced by ASM 9, and its `URLClassLoader` cast removed, also in relocated copies.
- Log4Shell protection for the Log4j 2.0-beta9 that Minecraft 1.8.9 ships: logged messages are no longer
  interpolated (CVE-2021-44228, CVE-2021-45046, CVE-2021-45105) and the `jndi` lookup resolves to nothing.
- Java4Shell (CVE-2026-114938) stopgap: Lazarus always runs with `-XX:-UseBASE64Intrinsics`, relaunching when the
  running JVM has the Base64 intrinsics on.
- A bootstrap that relaunches the JVM once, with the agent, a corrected class path and the JVM flags, when a launcher
  starts Lazarus without them.
- LWJGL 2 Linux natives load on JDKs that no longer version `libjawt.so` symbols.
- An installer for the official Minecraft Launcher (`java -jar`, window or command line), and a Prism Launcher /
  MultiMC component (`java -jar … prism`).
- Optional LWJGL 3: an LWJGL 2 API layer on GLFW, OpenGL and OpenAL (`lol.trq.lazarus-lwjgl3`, bundled), LWJGL 2
  method aliases added to LWJGL 3 classes by the agent, bootstrap support (LWJGL 2 removed from the class path,
  `-XstartOnFirstThread` and headless AWT on macOS), Minecraft's clipboard on GLFW, and `install --lwjgl3`. Runs
  on Apple silicon. See `docs/lwjgl3.md`.
- `-Dlazarus.debug.input=true` traces mouse grabs, focus and movement.
- A development harness that boots real Forge 1.8.9 on any JDK, smoke and Mixin test mods, and CI that boots the
  game on Java 17, 21 and 25.
