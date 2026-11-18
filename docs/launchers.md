# Using Lazarus from a launcher

## What a launcher has to do

The minimum:

1. Start a Java 17 or newer JVM.
2. Put `lol.trq.lazarus-<version>.jar` on the class path, next to the usual Forge 1.8.9 libraries and the Minecraft
   1.8.9 client jar.
3. Use `lol.trq.lazarus.launch.Lazarus` as the main class instead of `net.minecraft.launchwrapper.Launch`. The
   program arguments do not change; keep `--tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker`.

That is enough: on first start Lazarus relaunches the JVM once with the agent and a corrected class path.

To skip the relaunch, also:

4. Add `-javaagent:<path to lol.trq.lazarus jar>` to the JVM arguments.
5. Put the Lazarus jar before Forge's `asm-all-5.0.3.jar` on the class path, or leave `asm-all` out. Lazarus bundles
   ASM 9.
6. Add `-XX:+IgnoreUnrecognizedVMOptions --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow`
   to the JVM arguments, so Java 22+ does not print warnings.

Natives, `-Djava.library.path`, assets and authentication are handled as for stock Forge.

## Official Minecraft Launcher

Run the jar and click OK, or run `java -jar lol.trq.lazarus-<version>.jar install`. Options:

| Option | Default |
|--------|---------|
| `--dir <path>` | `%APPDATA%\.minecraft`, `~/Library/Application Support/minecraft` or `~/.minecraft` |
| `--forge <id>` | the newest installed `1.8.9-forge…` version |
| `--memory <MB>` | 2048 |

It writes:

- `libraries/lol/trq/lazarus/lol.trq.lazarus/<version>/lol.trq.lazarus-<version>.jar`
- `versions/<forge id>-lazarus/<forge id>-lazarus.json`: inherits the Forge version, sets the Lazarus main class,
  adds the jar, and asks for the `java-runtime-delta` (Java 21) runtime, which the launcher downloads.
- a profile in `launcher_profiles.json` (backed up to `launcher_profiles.json.lazarus-backup` first) with the JVM
  flags, and `-javaagent` when the jar's path has no spaces. The launcher splits JVM arguments on spaces, so with a
  space in the path the agent is left to the bootstrap.

To uninstall, delete the version folder and the profile in the launcher.

## Prism Launcher and MultiMC

1. Create a Minecraft 1.8.9 instance and add Forge 11.15.1.2318.
2. Open the instance folder and put `lol.trq.lazarus-<version>.jar` into its `libraries` folder (create it).
3. Save the component:

   ```
   java -jar lol.trq.lazarus-<version>.jar prism > lol.trq.lazarus.json
   ```

   then add it with **Edit instance → Version → Import components** (or copy it to the instance's `patches` folder
   and add `{"uid": "lol.trq.lazarus"}` to `mmc-pack.json` after Forge).
4. **Settings → Java**: choose Java 17 or newer and enable **Skip Java compatibility checks**, since Minecraft 1.8.9
   declares Java 8.
5. Optionally add the JVM flags from step 6 above under **Settings → Java → JVM arguments**.

The component only sets the main class and adds the jar; the bootstrap does the rest. The Prism and MultiMC route
follows their documented component format but has not yet been run end to end by the maintainers. Reports welcome.

## Custom launchers

Read `lol/trq/lazarus/libraries.json` from the jar. It states the main class, the libraries to remove from a Forge
1.8.9 profile, and the JVM arguments, and it is what the harness and installer use.
