# Compatibility

## Tested

Each row was booted with the development harness: real Minecraft 1.8.9 and Forge 11.15.1.2318, downloaded from
Mojang and Forge, launched through Lazarus.

| Java | Forge only | + Mixin 0.7.11 mod | + OptiFine HD U M5 | World | Where |
|------|-----------|--------------------|--------------------|-------|-------|
| 17 (Temurin 17.0.19) | ✔ | ✔ | ✔ | ✔ | Windows 10 |
| 21 (Corretto 21.0.9) | ✔ | ✔ | ✔ | ✔ | Windows 10 |
| 25 (Corretto 25.0.4) | ✔ | ✔ | ✔ | ✔ | Windows 10 |
| 17, 21, 25 (Temurin) | ✔ | ✔ | not run (OptiFine cannot be redistributed) | | Linux, CI, Xvfb with software OpenGL |

"Forge only" means the main menu is reached with FML, Forge and a test mod loaded. "Mixin" means a mod that bundles
Mixin 0.7.11 and registers through `MixinTweaker`, the way most 1.8.9 mods do, injected into `GuiMainMenu`.
"World" means a new singleplayer world was created and played for five seconds on the integrated server.

Also tested:

- **Stock class path, no agent, no flags** (`runBare21`, `runBare25`): the bootstrap relaunches and the game boots.
- **An installed launcher profile** (`runProfile`): the version and profile the installer writes, launched the way
  the official launcher launches legacy versions, from a directory whose path contains a space.

## Expected to work

- Other Forge 1.8.9 builds that share 11.15.1.2318's internals.
- Mods that only use Forge APIs.
- Mods that bundle Mixin 0.7.x, relocated or not.
- Coremods and `ITweaker` mods, including ones in the `mods` folder.

## Known limitations

- **Mods that reach into JDK internals themselves**, beyond what Forge does, may still fail. The agent opens the JDK
  to the game's class loaders, which covers plain reflection; removed APIs are only replaced where Forge, FML,
  Mixin 0.7 or OptiFine used them. Report anything else as an issue with the stack trace.
- **Nashorn** (the `javax.script` JavaScript engine) left the JDK in Java 15. Mods that script with it need the
  standalone `org.openjdk.nashorn` library, which uses a different package.
- **LWJGL 2.9.4 by default**, which runs on Java 17 to 25 on Windows and Linux x64. The optional LWJGL 3 install
  ([lwjgl3.md](lwjgl3.md)) runs on Apple silicon (tested on an M2 with macOS 26.5).
- **Minecraft and Forge versions other than 1.8.9** are not supported.
- **Servers** are not covered yet. Lazarus targets the client.
