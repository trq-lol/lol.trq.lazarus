/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lol.trq.lazarus.agent.LazarusAgent;

/**
 * Makes sure the JVM is set up for Lazarus, relaunching it once with a corrected command line when it is not.
 *
 * <p>A launcher only has to start {@link Lazarus} with the Lazarus jar on the class path. If the agent is missing,
 * if Forge's ASM 5 comes before the bundled ASM 9 on the class path, or if the JVM flags that silence Java 22+
 * warnings are missing, the bootstrap starts a child JVM with the agent, a reordered class path and the flags, and
 * exits with its status.
 */
final class Bootstrap {
    static final String RELAUNCHED = "lazarus.relaunched";

    /** JVM flags every Lazarus launch wants; ignored by JDKs that do not know them. */
    static final List<String> JVM_FLAGS = List.of(
            "-XX:+IgnoreUnrecognizedVMOptions",
            "--enable-native-access=ALL-UNNAMED",
            "--sun-misc-unsafe-memory-access=allow",
            // Java4Shell (CVE-2026-114938): the AVX-512 Base64 decode intrinsic corrupts the heap on Java 17+.
            "-XX:-UseBASE64Intrinsics");

    private Bootstrap() {}

    /**
     * Relaunches the JVM if it needs fixing.
     *
     * @param args the program arguments
     * @return the child JVM's exit status if one ran and this JVM should exit with it; empty to boot in this JVM
     * @throws Exception if the child JVM cannot be started
     */
    static Optional<Integer> relaunchIfNeeded(String[] args) throws Exception {
        if (Boolean.getBoolean(RELAUNCHED) || Boolean.getBoolean("lazarus.noRelaunch")) {
            return Optional.empty();
        }
        File self = selfJar();
        List<String> classpath = classpath();
        List<String> fixedClasspath = fixClasspath(classpath, self);
        boolean agent = LazarusAgent.installed();
        boolean base64Safe = !Security.base64IntrinsicsEnabled();
        if (agent && fixedClasspath.equals(classpath) && base64Safe) {
            return Optional.empty();
        }
        List<String> command = new ArrayList<>();
        for (String arg : inputArguments()) {
            if (!arg.startsWith("-Djava.class.path=") && !isOurAgent(arg) && !overridesSafetyFlag(arg)) {
                command.add(arg);
            }
        }
        // Lazarus's flags go last so they win over anything the launcher passed.
        command.addAll(JVM_FLAGS);
        command.add("-javaagent:" + self.getAbsolutePath());
        command.add("-D" + RELAUNCHED + "=true");
        command.add("-cp");
        command.add(String.join(File.pathSeparator, fixedClasspath));
        command.add(Lazarus.class.getName());
        command.addAll(List.of(args));

        Path argfile = Files.createTempFile("lazarus-", ".args");
        argfile.toFile().deleteOnExit();
        Files.writeString(argfile, toArgfile(command), StandardCharsets.UTF_8);
        System.err.println("[Lazarus] relaunching: "
                + (agent ? "" : "agent not installed; ")
                + (fixedClasspath.equals(classpath) ? "" : "class path reordered for ASM 9; ")
                + (base64Safe ? "" : "Base64 intrinsics disabled (CVE-2026-114938)"));
        Process child =
                new ProcessBuilder(javaExecutable(), "@" + argfile).inheritIO().start();
        Runtime.getRuntime().addShutdownHook(new Thread(child::destroy, "lazarus-relaunch-reaper"));
        return Optional.of(child.waitFor());
    }

    /**
     * Puts the Lazarus jar first and drops Forge's ASM 5, so the bundled ASM 9 is the one every loader sees.
     *
     * @param classpath the class path entries
     * @param self the Lazarus jar
     * @return the corrected entries
     */
    static List<String> fixClasspath(List<String> classpath, File self) {
        String selfPath = self.getAbsolutePath();
        List<String> fixed = new ArrayList<>();
        fixed.add(selfPath);
        for (String entry : classpath) {
            String name = new File(entry).getName();
            if (entry.isBlank()
                    || new File(entry).getAbsolutePath().equals(selfPath)
                    || name.startsWith("asm-all-")
                    || name.startsWith("asm-debug-all-")) {
                continue;
            }
            fixed.add(entry);
        }
        return fixed;
    }

    /**
     * Encodes arguments for a Java {@code @argfile}: each one quoted, with backslashes and quotes escaped.
     *
     * @param args the arguments
     * @return the file contents
     */
    static String toArgfile(List<String> args) {
        StringBuilder out = new StringBuilder();
        for (String arg : args) {
            out.append('"')
                    .append(arg.replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"')
                    .append(System.lineSeparator());
        }
        return out.toString();
    }

    /** Whether a JVM argument sets the Base64 intrinsics, which Lazarus always turns off (CVE-2026-114938). */
    static boolean overridesSafetyFlag(String arg) {
        return arg.matches("-XX:[+-]UseBASE64Intrinsics");
    }

    private static boolean isOurAgent(String arg) {
        return arg.startsWith("-javaagent:")
                && new File(arg.substring("-javaagent:".length())).getName().startsWith("lol.trq.lazarus");
    }

    private static List<String> classpath() {
        List<String> entries = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static List<String> inputArguments() {
        try {
            return ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (LinkageError | SecurityException e) {
            System.err.println("[Lazarus] cannot read the JVM's arguments; relaunching without them");
            return List.of();
        }
    }

    private static String javaExecutable() {
        return ProcessHandle.current()
                .info()
                .command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    }

    static File selfJar() {
        try {
            return new File(Lazarus.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
