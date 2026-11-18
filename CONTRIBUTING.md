# Contributing

- Conventional Commits, imperative, lowercase subject, no trailing period. No AI-attribution trailers.
- `./gradlew spotlessApply` then `./gradlew spotlessCheck build` before pushing.
- Every source file starts with `/* SPDX-License-Identifier: LGPL-3.0-or-later */` (Spotless adds it).
- Tests live in `src/test`. Behaviour changes that can be tested off the game thread ship with a test.
- Never ship modified Forge, FML, Minecraft or OptiFine files. Patch at runtime.
- Do not copy code from other projects without recording it and its license in `NOTICE.md`.
- No machine paths or secrets in commits.
