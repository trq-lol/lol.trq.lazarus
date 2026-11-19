# Contributing

- Conventional Commits, imperative, lowercase subject, no trailing period. No AI-attribution trailers.
- `./gradlew spotlessApply` then `./gradlew spotlessCheck build` before pushing.
- Every source file starts with `/* SPDX-License-Identifier: LGPL-3.0-or-later */` (Spotless adds it).
- A new patch comes with a test in `runtime/src/test` that applies it to synthetic bytecode, and a row in
  `docs/how-it-works.md`.
- A behaviour change is also booted in the game before it is merged:

  ```sh
  ./gradlew :harness:runLazarus17 -Pmixin
  ./gradlew :harness:runLazarus25 -Pmixin -PjvmArgs=-Dlazarus.smoke.world=true
  ```

  and, if you have an OptiFine 1.8.9 jar, again with `-Poptifine=<path>`. Never commit OptiFine.
- Never ship modified Forge, FML, Minecraft or OptiFine files. Patch at runtime.
- Do not copy code from other projects without recording it and its license in `NOTICE.md`.
- No machine paths or secrets in commits.
