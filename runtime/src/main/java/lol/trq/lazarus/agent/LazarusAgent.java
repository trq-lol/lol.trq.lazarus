/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.agent;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import lol.trq.lazarus.patch.Patches;

/**
 * Java agent that patches Forge, FML and their libraries as they load.
 *
 * <p>FML keeps its launch classes out of the launch class loader, so a LaunchWrapper transformer never sees them. An
 * agent's {@link java.lang.instrument.ClassFileTransformer} sees every class from every loader.
 */
public final class LazarusAgent {
    private static volatile Instrumentation instrumentation;

    private LazarusAgent() {}

    /**
     * Agent entrypoint for {@code -javaagent}.
     *
     * @param args the agent argument string, unused
     * @param inst the instrumentation instance
     */
    public static void premain(String args, Instrumentation inst) {
        install(inst);
    }

    /**
     * Agent entrypoint for dynamic attach.
     *
     * @param args the agent argument string, unused
     * @param inst the instrumentation instance
     */
    public static void agentmain(String args, Instrumentation inst) {
        install(inst);
    }

    /** {@return whether the agent is installed in this JVM} */
    public static boolean installed() {
        return instrumentation != null;
    }

    /**
     * Opens every package of every JDK module to {@code target}, restoring Java 8's reflection rules for it.
     *
     * <p>Minecraft 1.8.9, Forge and their libraries reflect into JDK internals freely. Opening the JDK only to the
     * game's own unnamed modules keeps that working without {@code --add-opens} flags.
     *
     * @param target the module to open the JDK to, usually a class loader's unnamed module
     * @throws IllegalStateException if the agent is not installed
     */
    public static void openJdkTo(Module target) {
        Instrumentation inst = instrumentation;
        if (inst == null) {
            throw new IllegalStateException("the Lazarus agent is not installed; add -javaagent:<lazarus jar>");
        }
        Set<Module> to = Set.of(target);
        for (Module module : ModuleLayer.boot().modules()) {
            Map<String, Set<Module>> opens = new HashMap<>();
            for (String pkg : module.getPackages()) {
                if (!module.isOpen(pkg, target)) {
                    opens.put(pkg, to);
                }
            }
            if (!opens.isEmpty()) {
                inst.redefineModule(module, Set.of(), opens, opens, Set.of(), Map.of());
            }
        }
    }

    /**
     * Appends a jar to the application class loader's search path, the Java 9+ replacement for calling {@code
     * URLClassLoader.addURL} on the system class loader.
     *
     * @param jar the jar
     * @throws IOException if the jar cannot be opened
     * @throws IllegalStateException if the agent is not installed
     */
    public static void appendToSystemClassPath(File jar) throws IOException {
        Instrumentation inst = instrumentation;
        if (inst == null) {
            throw new IllegalStateException("the Lazarus agent is not installed; add -javaagent:<lazarus jar>");
        }
        inst.appendToSystemClassLoaderSearch(new JarFile(jar));
    }

    private static synchronized void install(Instrumentation inst) {
        if (instrumentation != null) {
            return;
        }
        instrumentation = inst;
        openJdkTo(ClassLoader.getSystemClassLoader().getUnnamedModule());
        inst.addTransformer(new PatchingTransformer(Patches.all()));
    }
}
