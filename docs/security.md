# Security

Running a 2015 game on a 2026 JVM changes its attack surface in both directions. Lazarus closes the known holes it
can reach; this page lists them and what it does about each.

## Log4Shell (CVE-2021-44228, CVE-2021-45046, CVE-2021-45105)

Minecraft 1.8.9 ships Log4j **2.0-beta9**, the first version affected. Log4j expands `${...}` lookups inside logged
messages, so any text that reaches the log (chat from a server, a player name, a sign) is evaluated. That enabled
remote code execution through `${jndi:ldap://...}` and a recursive-lookup crash.

Lazarus patches Log4j as it loads:

- messages are written without lookups, as Log4j 2.15+ does (`log4shell-message-lookups`);
- the `jndi` lookup resolves to nothing everywhere (`log4shell-jndi`).

Every CI run logs a JNDI payload aimed at a local listener plus a `${sys:...}` lookup, and fails if either is
expanded or anything connects. On a stock Forge 1.8.9 launch the `${sys:...}` lookup does expand.

## Java4Shell (CVE-2026-114938)

Disclosed 2026-11-17. On x86-64 CPUs with AVX-512 VBMI (Intel Ice Lake and later, AMD Zen 4 and later), HotSpot's
Base64 decode intrinsic in **Java 17, 21, 25 and 27** writes past its buffer for crafted input, which can lead to
remote code execution in any code that decodes attacker-controlled Base64 with `java.util.Base64`. Java 8, the
game's original runtime, is not affected, so moving the game to modern Java would add this exposure.

Lazarus always runs with `-XX:-UseBASE64Intrinsics`, the advised stopgap:

- the installer and `libraries.json` include it;
- if the running JVM has the intrinsics on (the flag is missing, or a launcher turned it back on), the bootstrap
  relaunches with it, placed after the launcher's own arguments so it wins.

The flag only switches off a fast path; decoding stays correct. It is safe on JVMs without AVX-512 and on JDKs that
already contain the fix. Still update to a fixed JDK when your distribution ships one (for example Corretto
25.0.5.1.2, 21.0.13.1.2 or 17.0.21.1.2, or Oracle 25.0.5.1); the same release fixes nine other JDK CVEs.

## Not affected

- **Spring4Shell** (CVE-2022-22965): needs the Spring Framework, which Minecraft, Forge and Lazarus do not contain.
- **Text4Shell** (CVE-2022-42889): needs Apache Commons Text, which none of them contain. The Commons Lang that
  Minecraft ships and the relocated Commons libraries in Lazarus are different projects.

## What Lazarus does not change

- Mods run with full access to the JVM, as on Java 8. Lazarus opens the JDK only to the game's own class loaders,
  but anything loaded there is trusted.
- Lazarus does not update Minecraft's other bundled libraries (Netty 4.0.23, Guava 17, Commons IO 2.4). Their known
  CVEs concern server-side HTTP and file handling rather than the game client's network protocol.

Report vulnerabilities privately; see [SECURITY.md](../SECURITY.md).
