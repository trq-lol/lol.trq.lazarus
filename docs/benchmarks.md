# Benchmarks

The smoke mod has a benchmark mode. It creates a world from a fixed seed, waits 15 seconds (300 ticks) for chunks
to load, turns the frame cap and vsync off, holds the player still and turns the camera one full circle over the
run. Then it prints one line:

```
LAZARUS-BENCH fps=… avg_ms=… p50_ms=… p99_ms=… p999_ms=… max_ms=… low1_fps=… frames=… gc=… gc_ms=… heap_mb=…
              java=… gl=… lwjgl=… optifine=…
```

`low1_fps` is the average frame rate over the slowest 1% of frames. `gc` and `gc_ms` count collections during the
run only.

## Running it

```
./gradlew :harness:runLazarus25 -PjvmArgs="-Dlazarus.smoke.bench=60"
./gradlew :harness:runLwjgl3_25 -PjvmArgs="-Dlazarus.smoke.bench=60"
./gradlew :harness:runLwjgl3_25 -PjvmArgs="-Dlazarus.smoke.bench=60 -Dlazarus.gl=core"
```

Add `-Poptifine=/path/OptiFine.jar` to include OptiFine. `-Dlazarus.smoke.benchSettleTicks=<n>` changes the
settle time. Close other GPU-heavy programs, and compare runs from the same machine only.

## Results

Windows 10, RTX 4070, Java 25.0.4, 1280x720 window, render distance 8, default video settings. One 60 second run
per row, so treat differences under about 10% as noise.

| Backend | Renderer | OptiFine | fps | p99 ms | 1% low fps |
|---------|----------|:--------:|----:|-------:|-----------:|
| LWJGL 2 | driver | no | 491 | 4.23 | 130 |
| LWJGL 3 | driver | no | 1239 | 1.60 | 415 |
| LWJGL 3 | core (emulated) | no | 436 | 4.51 | 134 |
| LWJGL 2 | driver | M5 | 1475 | 1.85 | 320 |
| LWJGL 3 | driver | M5 | 1382 | 2.00 | 316 |
| LWJGL 3 | core (emulated) | M5 | 663 | 3.44 | 216 |

After the first round of core renderer work (uniform caching, tracked bindings, a sub-data stream ring, per-mesh
vertex arrays), the same scene gives:

| Backend | Renderer | OptiFine | fps | p99 ms | 1% low fps |
|---------|----------|:--------:|----:|-------:|-----------:|
| LWJGL 3 | core (emulated) | no | 529 | 3.62 | 144 |
| LWJGL 3 | core (emulated) | M5 | 1323 | 1.50 | 489 |

With OptiFine the core renderer is now within about 5% of the driver. On vanilla the frame is mostly the game's own
CPU work (sky colour blending, chunk visibility), which a profile shows outside the emulator.

## Run-to-run noise

Repeated runs of the same configuration differ by up to 25% on this machine, so compare several runs. For reference,
stock Forge on Java 8 (LWJGL 2, vanilla) measured 666 fps, in the same range as Lazarus on Java 25.

## Garbage collectors

Two 40 second runs each, Java 25, LWJGL 3, vanilla:

| Collector | fps | p99 ms | p99.9 ms | 1% low fps |
|-----------|-----|--------|----------|------------|
| G1 (default) | 802, 748 | 3.07, 2.68 | 14.2, 6.7 | 145, 222 |
| Parallel | 810, 1036 | 2.42, 1.81 | 5.5, 4.1 | 266, 374 |
| ZGC | 690, 772 | 2.88, 2.45 | 8.1, 6.1 | 191, 260 |
| Shenandoah | 771, 702 | 2.41, 2.78 | 8.2, 4.2 | 219, 274 |

Parallel was best on every measure in both runs here, and is what Java 8 used by default. Two runs are not enough to
change the default; this needs more runs and other machines first.
