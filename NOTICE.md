# Notice

lol.trq.lazarus is licensed under the GNU Lesser General Public License v3.0 or later.

## Bundled third-party code

| Component | Where | License |
|-----------|-------|---------|
| [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) 1.28.0, with Commons IO and Commons Lang 3 | relocated under `lol.trq.lazarus.shadow` | Apache-2.0 |
| ASM 6.2.1 `Remapping*Adapter` classes (removed in ASM 7) | `runtime/src/main/java/org/objectweb/asm/commons` | BSD-3-Clause (header kept in each file) |

Commons Compress supplies the Pack200 unpacker that Java 14 removed. The ASM adapters are ported to ASM 9 so FML's
deobfuscation transformer, written against ASM 5, keeps loading.

## Studied, not copied

The projects credited in the README were read for design. No code from them is included. A change that copies
code from any project must add it here with its license and keep its notices.
