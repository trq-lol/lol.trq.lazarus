# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/) and the
project uses [Semantic Versioning](https://semver.org/).

## [0.1.0] - Unreleased

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
- A development harness that boots real Forge 1.8.9 on any JDK, smoke and Mixin test mods, and CI that boots the
  game on Java 17, 21 and 25.
