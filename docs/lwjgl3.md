# LWJGL 3

Minecraft 1.8.9 is written against LWJGL 2.9.4. Lazarus can run it on LWJGL 3 instead, for native Apple silicon
support, GLFW windowing and input, and correct high-DPI scaling. It is optional: the default install keeps LWJGL 2.

## Why not just swap the jars

LWJGL 2 and LWJGL 3 share the `org.lwjgl` package and the JNI symbol names of their native libraries, so they cannot
run side by side, and LWJGL 3 cannot be relocated. LWJGL 3 also dropped or reshaped much of the API the game uses.
The switch has to cover the whole game: Minecraft, Forge, OptiFine and every mod see the same `org.lwjgl`.

## What Lazarus provides

The LWJGL 2 API surface used by Minecraft 1.8.9, Forge 11.15.1.2318, OptiFine HD U M5 and paulscode's OpenAL library
was measured class by class against LWJGL 3.4.3:

| | References |
|---|---|
| Unchanged in LWJGL 3 | 183 |
| Renamed or reshaped methods (for example `glGetFloat` → `glGetFloatv`) | 41 |
| In 19 classes LWJGL 3 does not have | about 234 |

- **The missing classes** are in `lol.trq.lazarus-lwjgl3.jar`, written for Lazarus: `Display`, `DisplayMode`,
  `PixelFormat`, `ContextAttribs`, `Drawable`, `SharedDrawable`, `GLContext`, `ContextCapabilities`, `Keyboard`,
  `Mouse`, `Sys`, `LWJGLUtil`, `LWJGLException`, `OpenGLException`, `ARBDebugOutputCallback`.
  `ContextCapabilities` is generated from LWJGL 2.9.4's 378 capability fields and filled from LWJGL 3's
  `GLCapabilities` by name.
- **The renamed methods** are added to LWJGL 3's own classes by the agent at load time, with the LWJGL 2 name and
  descriptor, delegating to the LWJGL 3 call. A method that already exists is never touched, so these patches do
  nothing on an LWJGL 2 launch.
- **OpenAL**: LWJGL 2's `AL.create()` opened a device and context; LWJGL 3 has no such method. paulscode's calls are
  redirected to an equivalent lifecycle.
- **`lwjgl_util`** (vectors, matrices, GLU) is plain Java and stays as it is.

### Behaviour kept from LWJGL 2

- Key codes are LWJGL 2's (DirectInput scan codes), so saved key bindings keep working. Key names match
  `Keyboard.getKeyName`.
- A typed character arrives with the key press that produced it, as in LWJGL 2.
- Mouse coordinates are framebuffer pixels with y from the bottom; wheel steps are 120 per notch.
- A grabbed cursor uses raw, unaccelerated motion where the platform supports it.
- `Display.getWidth`/`getHeight` are framebuffer pixels, so viewports are right on high-DPI screens.
- Forge's splash screen draws on another thread with a `SharedDrawable`; `Display.update()` there swaps buffers and
  leaves event polling to the main thread, which GLFW requires.

## Launch requirements

The bootstrap handles these when it finds the compatibility jar on the class path:

- LWJGL 2's core and natives jars are removed from the class path (Minecraft's version lists them, and a launcher
  cannot remove inherited libraries).
- On macOS the JVM is relaunched with `-XstartOnFirstThread`, and AWT is made headless
  (`-Djava.awt.headless=true`). Otherwise AWT, started off the main thread, hands its `[NSApplication run]` loop to
  the main thread through GLFW's event pump and the game freezes. Headless AWT still decodes images; Minecraft's
  clipboard is moved from AWT to GLFW on every LWJGL 3 launch. Mods that open AWT windows or dialogs throw
  `HeadlessException` on macOS with LWJGL 3.

LWJGL 3 extracts its own natives from its natives jars; LWJGL 2's natives on `java.library.path` do not interfere.

## Tested

| | Java 17 | Java 21 | Java 25 |
|---|:---:|:---:|:---:|
| Windows: menu, Mixin 0.7 mod | ✔ | ✔ | ✔ |
| Windows: OptiFine, world, splash screen | | | ✔ |
| Windows: installed launcher profile, stock class path, no agent | | ✔ | ✔ |
| Linux CI (Xvfb, software OpenGL): menu, Mixin 0.7 mod, world | ✔ | ✔ | ✔ |
| macOS 26.5, Apple M2: menu, Mixin 0.7 mod | ✔ | ✔ | ✔ |
| macOS: world, Retina screenshot, stock class path with no agent | | | ✔ |

Played by hand on Windows (typing, menus, inventory, mouse look) and on macOS with a trackpad (typing, copy and
paste, menus, mouse look, Cmd-Tab). macOS is tested by hand because GitHub's hosted macOS runners have no usable
OpenGL. LWJGL 2 does not start on Apple silicon (`liblwjgl.dylib` has no arm64 build), which is what the LWJGL 3
install is for.
