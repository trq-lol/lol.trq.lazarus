# Benchmarks

## Method

The smoke mod has a benchmark mode. It creates a world from a fixed seed, waits 15 seconds for chunks to load, turns
the frame cap and vsync off, holds the player still and turns the camera one full circle over the run, then prints:

```text
LAZARUS-BENCH fps=… avg_ms=… p50_ms=… p99_ms=… p999_ms=… max_ms=… low1_fps=… frames=… gc=… gc_ms=… heap_mb=…
              java=… gl=… lwjgl=… optifine=…
```

`low1_fps` is the average frame rate over the slowest 1% of frames; `gc` and `gc_ms` count collections during the
run only.

```sh
./gradlew :harness:runLazarus25 -PjvmArgs="-Dlazarus.smoke.bench=60"                       # LWJGL 2
./gradlew :harness:runLwjgl3_25 -PjvmArgs="-Dlazarus.smoke.bench=60"                       # LWJGL 3, core renderer
./gradlew :harness:runLwjgl3_25 -PjvmArgs="-Dlazarus.smoke.bench=60 -Dlazarus.gl=compat"   # LWJGL 3, legacy renderer
```

Add `-Poptifine=/path/OptiFine.jar` for OptiFine and `-PshaderPack=/path/pack.zip` for a shader pack.
`-Dlazarus.smoke.benchDebug=true` keeps the F3 screen open. Runs on one machine vary by up to 25%, so compare several
runs of each configuration and only compare runs from the same machine.

## macOS

MacBook Air M2, macOS 26.5, Java 25.0.5, 30 second runs. On macOS the legacy renderer gets OpenGL 2.1; the core
renderer gets OpenGL 4.1.

| | Legacy OpenGL 2.1 | Core renderer |
|---|---:|---:|
| Vanilla | 160 fps | 364 fps |
| OptiFine HD U M5 | 223 fps | 528 fps |
| OptiFine, F3 open | | 507 fps |

With shader packs, OptiFine HD U M5:

| Pack | Legacy OpenGL 2.1 | Core renderer |
|---|---|---|
| BSL 10.1.8 | 26 fps; shadow program does not compile | 81 fps, all programs |
| Sildur's Vibrant 2.02 Medium | 120 fps; one composite pass does not compile | 107 fps, all programs |
| Complementary Reimagined r5.10 | does not load | 76 fps, all programs |

Apple's OpenGL 2.1 only accepts GLSL 1.20, which is why packs lose programs on the legacy renderer. The core renderer
compiles all of them, so on Sildur's it does more work than the legacy renderer and the two are not directly
comparable.

## Windows

Windows 10, NVIDIA RTX 4070, Java 25.0.4, 1280×720, render distance 8.

| | Vanilla | OptiFine HD U M5 |
|---|---:|---:|
| LWJGL 2 | 491 fps | 1475 fps |
| LWJGL 3 | 1239 fps | 1382 fps |
| LWJGL 3, core renderer | 740 fps | 2270 fps |

On a driver with a fast legacy OpenGL path, the core renderer is slower on vanilla, where the frame is mostly the
game's own CPU work, and faster with OptiFine. Stock Forge 1.8.9 on Java 8 measures in the same range as Lazarus on
Java 25 (666 fps vanilla on LWJGL 2).

## Garbage collectors

Java 25, vanilla, two runs per collector, mean frame rate:

| Collector | Windows, RTX 4070, LWJGL 3 legacy renderer | macOS, Apple M2, core renderer |
|-----------|---:|---:|
| G1 (default) | 775 fps | 356 fps |
| Parallel | 923 fps | 375 fps |
| ZGC | 731 fps | 348 fps |
| Shenandoah | 737 fps | 349 fps |

The differences are within run-to-run noise on the M2, and with OptiFine G1 and Parallel measure the same (525 and
521 fps). ZGC uses two to three times the heap of the others. Lazarus keeps the JVM's default, G1;
`-XX:+UseParallelGC` is a reasonable alternative on machines where it measures better.
