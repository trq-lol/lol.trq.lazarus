# Core renderer

Minecraft 1.8.9 was written for legacy OpenGL: immediate mode, display lists, matrix stacks, fixed-function fog and
lighting, and GLSL 1.20. The core renderer runs it on an OpenGL 4.1 core profile instead, the modern OpenGL every
current driver supports.

On macOS this matters most. Apple gives legacy programs OpenGL 2.1 and GLSL 1.20, and nothing newer, so the game and
its shader packs are stuck there. On the core renderer they get OpenGL 4.1: OptiFine runs more than twice as fast on
an Apple M2, and shader packs compile in full, including ones the legacy path cannot load at all. On Windows and
Linux it works the same way, so one renderer covers every platform.

## Using it

The core renderer is the default on the LWJGL 3 profile (see [LWJGL 3](lwjgl3.md)); Minecraft, Forge, OptiFine and
mods run on it as they are. To use the legacy renderer instead, add this to the profile's JVM arguments:

```
-Dlazarus.gl=compat
```

The LWJGL 2 profile always uses the legacy renderer.

## Shader packs

OptiFine shader packs work on the core renderer. Turn off **Fast Render** and **Antialiasing** in OptiFine's video
settings, as OptiFine requires for shaders.

Supported packs:

- **BSL Shaders** 10.1.8
- **Complementary Reimagined** r5.10
- **Sildur's Vibrant Shaders** 2.02

Other packs usually work too; if one doesn't, open an issue with the pack's name and version and the output of a
run with `-Dlazarus.gl.debug=true`.

| Pack | Apple M2, core | Apple M2, legacy OpenGL 2.1 |
|------|---|---|
| BSL 10.1.8 | all programs, with shadows | shadow program does not compile |
| Sildur's Vibrant 2.02 Medium | all programs | one composite pass does not compile |
| Complementary Reimagined r5.10 | all programs | does not load |

BSL is also verified on Windows. Frame rates are in [benchmarks](benchmarks.md).

## How it works

- **Emulated fixed function.** The agent sends the game's fixed-function OpenGL calls (`glBegin`, `glVertexPointer`,
  `glPushMatrix`, `glFog`, `glCallList` and the rest) to an emulator. It keeps the legacy state itself and draws
  with one GLSL 3.30 shader covering transforms, both texture units, fog, the alpha test, the two directional lights
  and the damage tint. Every other OpenGL call goes straight to the driver.
- **Batching.** Consecutive draws with the same state are merged into one, so text and GUI cost a handful of draws
  per frame, and display lists are uploaded once into static buffers.
- **No driver queries.** The emulator tracks buffer, program, vertex array, texture and capability state itself.
  Queries stall the whole pipeline on some drivers, Apple's in particular.
- **Shader translation.** Shaders the game compiles (vanilla post-processing, mods and shader packs) are translated
  from legacy GLSL to core GLSL as they are uploaded. Fixed-function built-ins become inputs and uniforms the
  emulator supplies, legacy texture functions become their core equivalents, the alpha test is added back, and macros
  a shader tests without defining count as 0, so a pack compiles the same on every driver.
- **Extensions.** ARB and EXT extension calls that a core profile does not expose, such as `ARB_shader_objects` and
  `EXT_framebuffer_object`, are mapped to their core equivalents.

## Options

| Property | Effect |
|----------|--------|
| `lazarus.gl=compat` | Uses the legacy renderer on the LWJGL 3 profile instead of the core renderer |
| `lazarus.gl.debug=true` | Requests a debug context and prints every driver-reported GL error with the Java call site, names fixed-function calls the emulator does not implement, and writes shaders that fail to compile to `lazarus-shaders/` |
| `lazarus.gl.stream=subdata\|map` | How per-draw vertex data is uploaded. The default is `map` on Apple drivers and `subdata` elsewhere |

## For render libraries

Libraries that draw with their own shaders alongside the game should:

- bind state through `GL15.glBindBuffer`, `GL20.glUseProgram` and `GL30.glBindVertexArray`, which the emulator
  tracks, and draw through the `GL11C` and `GL32C` classes, which it leaves alone;
- read matrices and bindings from `lol.trq.lazarus.lwjgl3.gl.GlState` instead of querying the driver.
  `modelView`, `projection`, `program`, `vertexArray`, `arrayBuffer` and `viewport` answer from the emulator's own
  state, and a binding getter returns -1 when the state is unknown.

## Limitations

- The emulator covers the fixed-function features that Minecraft, Forge and OptiFine use. A mod that relies on
  others, such as texture coordinate generation, more than two lights or `glPolygonMode`, may draw incorrectly;
  `lazarus.gl.debug` names such calls.
- `glPushAttrib` saves the enables, the alpha test and the current colour, not fog parameters or the damage tint.
