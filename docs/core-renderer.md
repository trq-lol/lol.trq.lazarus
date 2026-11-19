# Core renderer

`-Dlazarus.gl=core` runs Minecraft 1.8.9 on an OpenGL 4.1 core profile instead of the legacy context it was
written for. It needs the LWJGL 3 profile. macOS only offers OpenGL 2.1 to legacy programs, so this is the way to a
modern OpenGL there; it works the same on Windows and Linux.

It is opt-in while it is being tested.

## How it works

A core profile has no fixed-function pipeline: no immediate mode, display lists, matrix stacks, fog, alpha test or
fixed lighting, and no GLSL 1.20. Lazarus emulates the parts Minecraft, Forge and OptiFine use:

- The agent redirects the fixed-function calls of game classes (`glBegin`, `glVertexPointer`, `glPushMatrix`,
  `glFog`, `glCallList`, ...) to an emulator in the LWJGL 3 jar. Every other OpenGL call goes to the driver, after
  the emulator flushes any batched draws.
- The emulator keeps the old state itself and draws with one GLSL 3.30 shader that implements transforms, two
  texture units (the second is the lightmap), fog, the alpha test, two directional lights and the damage tint.
- Immediate-mode and small vertex-array draws with the same state are merged into one draw. Display lists are
  recorded once into static buffers.
- The emulator tracks buffer, program, vertex array, texture and capability state itself, so it never queries the
  driver. Queries stall the whole pipeline on some drivers, Apple's in particular.
- Shaders the game compiles (vanilla's post-processing, mods, OptiFine) are translated from legacy GLSL to GLSL 3.30
  core when their source is uploaded, and receive the fixed-function matrices they read as uniforms.

## Options

| Property | Effect |
|----------|--------|
| `lazarus.gl=core` | Turns the core renderer on |
| `lazarus.gl.debug=true` | Asks for a debug context, prints every driver-reported GL error with the Java call site, and names fixed-function calls the emulator does not implement |
| `lazarus.gl.stream=subdata\|map` | How per-draw vertex data is uploaded. Defaults: `map` on Apple drivers, `subdata` elsewhere |
| `lazarus.swapInterval=<n>` | Re-applies a swap interval before every buffer swap, for diagnosing vsync |

## Status

Tested: vanilla and OptiFine HD U M5 into a world on Windows (NVIDIA), Linux (Mesa, in CI) and macOS (Apple M2),
with no driver errors; vanilla post-processing shaders; the damage tint; the BSL shader pack on Windows and macOS.

On macOS the core renderer runs shader packs that the legacy OpenGL 2.1 path cannot, because Apple's 2.1 driver only
accepts GLSL 1.20. Programs that compile on an Apple M2:

| Pack | Core | Legacy GL 2.1 |
|------|-----:|--------------:|
| BSL 10.1.8 | 26 / 26 | 24 / 26 (no shadows) |
| Sildur's Vibrant 2.02 Medium | 15 / 15 | 14 / 15 |
| Complementary Reimagined r5.10 | 26 / 26 | 0 / 26 |

The translator also makes packs portable across drivers: a macro that is not defined where an `#if` tests it counts as
0 (as on NVIDIA and Mesa), even though Apple's compiler rejects such lines.

Known gaps:

- OptiFine shader packs: BSL 10.1.8, Complementary Reimagined r5.10 and Sildur's Vibrant 2.02 are verified; other
  packs are untested. Shader packs need VBOs on and Fast
  Render and antialiasing off, as OptiFine requires.
- Only the fixed-function features the game uses are emulated. A mod that relies on others (texture coordinate
  generation, more than two lights, `glPolygonMode`) may draw incorrectly; `lazarus.gl.debug`
  names such calls.
- `glPushAttrib` saves only the enables, alpha test and colour, not fog parameters or the damage tint.
- The emulator's state is shared by the client thread and Forge's splash thread, which Forge keeps from drawing at
  the same time.

[benchmarks.md](benchmarks.md) has the numbers.

## For render libraries

Libraries that draw with their own shaders on the core renderer should:

- bind state through `GL15.glBindBuffer`, `GL20.glUseProgram` and `GL30.glBindVertexArray`, which the emulator
  tracks, and draw through the `GL11C`/`GL32C` classes, which it leaves alone;
- read matrices and bindings from `lol.trq.lazarus.lwjgl3.gl.GlState` instead of querying the driver.
  `GlState.modelView`, `projection`, `program`, `vertexArray`, `arrayBuffer` and `viewport` answer from the
  emulator's own state; a binding getter returns -1 when the state is unknown.
