# lol.trq.lazarus

Runs Minecraft Forge 1.8.9 on Java 17, 21 and 25.

Forge 1.8.9 was built for Java 8 and fails before its window opens on anything newer. Lazarus is a single jar that
fixes that from the outside: a replacement launch entrypoint and a Java agent that patch Forge, FML, Mixin 0.7 and
a few 2015-era libraries as they load. Minecraft, Forge and your mods stay stock. OptiFine keeps working.

| | Java 17 | Java 21 | Java 25 |
|---|---|---|---|
| Forge 1.8.9 to the main menu | ✔ | ✔ | ✔ |
| + OptiFine HD U M5 + a Mixin 0.7 mod | ✔ | ✔ | ✔ |
| Singleplayer world | ✔ | ✔ | ✔ |
| JVM warnings | none | none | none |

Every push boots real Forge 1.8.9 with a Mixin 0.7 mod on Java 17, 21 and 25 in CI. See
[docs/compatibility.md](docs/compatibility.md) for exactly what is tested and what is not.

## Install

You need Forge 1.8.9 (11.15.1.2318) installed first, with its own installer.

### Official Minecraft Launcher

1. Download `lol.trq.lazarus-<version>.jar` from the [releases](https://github.com/trq-lol/lol.trq.lazarus/releases).
2. Run it: double-click it, or `java -jar lol.trq.lazarus-<version>.jar`. Any Java 17 or newer works.
3. Click OK. The installer finds your `.minecraft` and your Forge 1.8.9 version.
4. In the launcher, pick the **Forge 1.8.9 (Lazarus)** installation and play. The launcher downloads the Java 21
   runtime it asks for.

From a terminal: `java -jar lol.trq.lazarus-<version>.jar install [--dir <.minecraft>] [--forge <id>]`.

The installer only adds files: a library, a new version that inherits your Forge version, and a profile. It backs up
`launcher_profiles.json` first. Your plain Forge installation keeps working on Java 8.

### Prism Launcher and MultiMC

1. Create a Minecraft 1.8.9 instance with Forge 11.15.1.2318.
2. Put `lol.trq.lazarus-<version>.jar` into the instance's `libraries` folder (create it if needed).
3. Save the output of `java -jar lol.trq.lazarus-<version>.jar prism` as `lol.trq.lazarus.json` and add it with
   **Edit instance → Version → Import components**.
4. In **Settings → Java**, pick Java 17 or newer and enable **Skip Java compatibility checks**.

[docs/launchers.md](docs/launchers.md) covers these steps in detail, and what any other launcher needs to do.

## How it works

Three pieces, all in the one jar:

- **Entrypoint** (`lol.trq.lazarus.launch.Lazarus`). Replaces LaunchWrapper's `Launch`, which casts the system class
  loader to `URLClassLoader` and dies on Java 9+. It builds the same `LaunchClassLoader` and runs the tweaker chain
  with LaunchWrapper's contract, so FML, Mixin and OptiFine see nothing different.
- **Agent.** Opens the JDK to the game's own class loaders, giving the game (and only the game) Java 8's reflection
  rules, and patches classes as they load: Pack200, ASM 5 behaviour on ASM 9, `static final` writes and enum
  creation without `sun.reflect`, no `SecurityManager`, system class path appends, and Mixin 0.7's private ASM 5.
  [docs/how-it-works.md](docs/how-it-works.md) lists every patch with the JDK change behind it.
- **Bootstrap.** If a launcher starts Lazarus without the agent, or with Forge's ASM 5 ahead of ASM 9 on the class
  path, Lazarus relaunches the JVM once with everything corrected. A launcher only has to set the main class and add
  the jar.

Lazarus also closes **Log4Shell** in the Log4j 2.0-beta9 that Minecraft 1.8.9 ships: messages are never interpolated
and JNDI lookups resolve to nothing. The smoke test logs a real exploit string on every CI run and checks nothing
happens.

The jar bundles ASM 9 and relocated copies of Apache Commons Compress and Gson; see [NOTICE](NOTICE.md).

## Troubleshooting

See [docs/troubleshooting.md](docs/troubleshooting.md). Start the game with `-Dlazarus.debug=true` to log every patch
Lazarus applies.

## Build

```
./gradlew spotlessApply build
```

Gradle provisions the JDKs it needs. The build has four projects:

| Project | What it is |
|---------|------------|
| `runtime` | The shipped jar: entrypoint, agent, patches, installer |
| `harness` | A development launcher that downloads Minecraft 1.8.9 and Forge and boots them on any JDK |
| `smoke` | A test mod that reports at the main menu (and optionally in a world), then exits |
| `smoke-mixin` | A test mod that bundles Mixin 0.7.11 and injects into the main menu |

Boot the game through Lazarus. The window opens muted and closes itself.

```
./gradlew :harness:runLazarus25 -Pmixin                        # Forge + a Mixin 0.7 mod on Java 25
./gradlew :harness:runLazarus21 -Poptifine=/path/OptiFine.jar  # with OptiFine (bring your own copy)
./gradlew :harness:runBare25                                   # worst case: no agent, stock class path
./gradlew :harness:runStock8                                   # stock LaunchWrapper on Java 8, for comparison
```

Add `-PjvmArgs="-Dlazarus.smoke.world=true -Dlazarus.smoke.screenshot=true"` to also play in a world and save a
screenshot under `harness/build/run/<task>/screenshots`.

## For AI agents

Read [CLAUDE.md](CLAUDE.md) for the mental model, the patterns that matter and the traps, and [llms.txt](llms.txt)
for a file index.

## Credits and prior art

Lazarus is written from scratch. These projects showed what breaks and why:

- [GTNewHorizons/lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify): Forge 1.7.10 on Java 17+ with LWJGL 3
- [CleanroomMC/Cleanroom](https://github.com/CleanroomMC/Cleanroom): Forge 1.12.2 on Java 21
- [moehreag/legacy-lwjgl3](https://github.com/moehreag/legacy-lwjgl3) and
  [Zarzelcow/legacy-lwjgl3](https://github.com/Zarzelcow/legacy-lwjgl3): LWJGL 3 on legacy Fabric and Ornithe

## License

[LGPL-3.0-or-later](COPYING.LESSER), which supplements the [GPL-3.0](COPYING). See [DISCLAIMER](DISCLAIMER.md) and
[NOTICE](NOTICE.md).
