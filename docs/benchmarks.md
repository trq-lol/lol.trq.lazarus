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

The core renderer (`-Dlazarus.gl=core`) is the macOS path and is still unoptimized: it uploads every uniform and
streams vertex data on every draw. It runs at roughly a third of the driver's speed on vanilla and half with
OptiFine. Closing that gap is the next piece of work on it. The LWJGL 2 vanilla row is unexpectedly slow next to
LWJGL 3 and has not been investigated yet.
