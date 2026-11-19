# Troubleshooting

Start with `-Dlazarus.debug=true` in the JVM arguments. Lazarus then logs every class it patches, with the patch
name, to standard error.

## `UnsupportedClassVersionError: lol/trq/lazarus/...`

The game started on Java 8 to 16. Lazarus needs Java 17 or newer. In the official launcher, the installed version asks
for Java 21; check that the profile does not override the Java executable.

## `the Lazarus agent is not installed`

The bootstrap's relaunch was turned off with `-Dlazarus.noRelaunch=true`, or the relaunch itself started without
the agent. Add `-javaagent:<path to the Lazarus jar>` to the JVM arguments.

## The game starts, then a second Java process appears

That is the bootstrap's one-time relaunch: the launcher started Lazarus without the agent or with Forge's ASM 5
first on the class path. It is harmless. To avoid it, follow steps 4 to 6 in [launchers.md](launchers.md).

## `Error opening zip file or JAR manifest missing`

The `-javaagent` path in the JVM arguments is wrong, often because it contains a space and the launcher split it.
Remove the `-javaagent` argument; the bootstrap adds it correctly.

## `NoSuchMethodError` or `NoClassDefFoundError` naming `org.objectweb.asm`

Something put an ASM older than 9 ahead of the Lazarus jar on the class path. Lazarus removes Forge's `asm-all`
when it relaunches; another library may bundle its own ASM unrelocated. Put the Lazarus jar first.

## A mod fails with `InaccessibleObjectException`, `NoSuchFieldException: modifiers` or `sun.reflect`

The mod reaches into JDK internals that Lazarus does not patch for it. Open an issue with the mod name, its version
and the full stack trace.

## `WARNING: A terminally deprecated method in sun.misc.Unsafe has been called`

Java 24+ warns about LWJGL 2's use of `Unsafe`. Add `--sun-misc-unsafe-memory-access=allow` (and, for Java 17 and 21
compatibility, `-XX:+IgnoreUnrecognizedVMOptions`) to the JVM arguments. The installer does this.

## The game freezes while quitting

Lazarus halts a JVM whose shutdown is still running 20 seconds after it began, and first prints every thread's
stack to the log under `[Lazarus] shutdown has not finished`. If you see that block, please open an issue with it.
`-Dlazarus.shutdownGrace=<seconds>` changes the wait; `0` turns the watchdog off.
