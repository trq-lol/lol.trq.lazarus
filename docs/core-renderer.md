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
| `lazarus.gl.debug=true` | Asks for a debug context and prints every driver-reported GL error with the Java call site |
| `lazarus.gl.stream=subdata\|orphan\|frames\|map` | How per-draw vertex data is uploaded; see `StreamBuffer` |
| `lazarus.swapInterval=<n>` | Re-applies a swap interval before every buffer swap, for diagnosing vsync |

## Status

Tested: vanilla and OptiFine HD U M5 into a world on Windows (NVIDIA), Linux (Mesa, in CI) and macOS (Apple M2),
with no driver errors; vanilla post-processing shaders; the damage tint.

Known gaps:

- OptiFine shader packs are not supported yet.
- On macOS the frame rate is currently held to the display's refresh steps (60 or 30 fps); this is being worked on.
- Only the fixed-function features the game uses are emulated. A mod that relies on others (texture coordinate
  generation, more than two lights, `glPolygonMode`) may draw incorrectly.

[benchmarks.md](benchmarks.md) has the numbers.
