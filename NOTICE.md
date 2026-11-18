# Notice

lol.trq.lazarus is licensed under the GNU Lesser General Public License v3.0 or later.

## Bundled third-party code

The Lazarus jar contains:

| Component | How | License |
|-----------|-----|---------|
| [ASM](https://asm.ow2.io/) 9.11.1 (`asm`, `asm-tree`, `asm-commons`, `asm-util`, `asm-analysis`) | unrelocated, `org.objectweb.asm` | BSD-3-Clause |
| ASM 6.2.1 `Remapping*Adapter` classes, removed in ASM 7 | source in `runtime/src/main/java/org/objectweb/asm/commons`, ported to ASM 9 | BSD-3-Clause (header kept in each file) |
| [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) 1.28.0, with Commons IO and Commons Lang 3 | relocated under `lol.trq.lazarus.shadow` | Apache-2.0 |
| [Gson](https://github.com/google/gson) 2.15.0 | relocated under `lol.trq.lazarus.shadow.gson` | Apache-2.0 |

- ASM 9 replaces Forge's ASM 5 so FML, Mixin 0.7 and OptiFine can read Java 9+ class files.
- The ASM 6.2.1 adapters keep FML's deobfuscation transformer, written against ASM 5, loading.
- Commons Compress supplies the Pack200 unpacker that Java 14 removed.
- Gson is used by the installer to edit launcher files.

`javax.annotation.Resource` in the jar is written from scratch to the JSR-250 signature; it left the JDK in Java 11
and Mixin 0.7 reads it.

## Studied, not copied

The projects credited in the README were read for design. No code from them is included. A change that copies code
from any project must add it here with its license and keep its notices.

## Not included

Lazarus does not include or download Minecraft, Forge, LWJGL or OptiFine for players. The development harness
downloads Minecraft and Forge from their official servers for testing.
