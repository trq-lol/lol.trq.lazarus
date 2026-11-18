/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lol.trq.lazarus.agent.LazarusAgent;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entrypoint that replaces {@code net.minecraft.launchwrapper.Launch} on Java 9 and newer.
 *
 * <p>LaunchWrapper's own entrypoint casts the system class loader to {@code URLClassLoader}, which fails on any
 * modern JVM. This class builds the same {@link LaunchClassLoader} from {@code java.class.path}, fills the same
 * {@link Launch} statics and blackboard keys, and runs the tweaker chain with the same contract, so FML, Mixin and
 * OptiFine see an unchanged environment.
 */
public final class Lazarus {
    private static final Logger LOG = LogManager.getLogger("Lazarus");

    /**
     * Packages the launch class loader leaves to its parent. LaunchWrapper's defaults cover {@code java.} and {@code
     * sun.}; on Java 9 and newer the JDK also defines classes in these packages through the platform class loader,
     * which the launch class loader cannot reach on its own. Lazarus's runtime packages load once, from the
     * application class loader, because patched FML code on both sides of that boundary calls into them.
     */
    static final List<String> PLATFORM_EXCLUSIONS = List.of(
            "javax.",
            "jdk.",
            "com.sun.",
            "org.w3c.dom.",
            "org.xml.sax.",
            "org.ietf.jgss.",
            "lol.trq.lazarus.agent.",
            "lol.trq.lazarus.compat.",
            "lol.trq.lazarus.launch.",
            "lol.trq.lazarus.patch.",
            "lol.trq.lazarus.shadow.");

    private Lazarus() {}

    /**
     * Boots the game.
     *
     * @param args the LaunchWrapper command line
     * @throws Throwable whatever the tweaker chain or the game's main method throws
     */
    public static void main(String[] args) throws Throwable {
        Optional<Integer> child = Bootstrap.relaunchIfNeeded(args);
        if (child.isPresent()) {
            System.exit(child.get());
            return;
        }
        if (!LazarusAgent.installed()) {
            throw new IllegalStateException("the Lazarus agent is not installed; add -javaagent:<lazarus jar>");
        }
        LaunchArguments parsed = LaunchArguments.parse(args);
        LaunchClassLoader loader = createClassLoader(Classpath.current());
        Launch.classLoader = loader;
        Launch.minecraftHome = parsed.gameDir();
        Launch.assetsDir = parsed.assetsDir();
        Launch.blackboard = new HashMap<>();
        Thread.currentThread().setContextClassLoader(loader);
        LOG.info(
                "Lazarus booting {} on Java {} ({})",
                parsed.profile(),
                Runtime.version(),
                System.getProperty("java.vendor"));
        Method target = runTweakers(loader, Launch.blackboard, parsed);
        @SuppressWarnings("unchecked")
        List<String> gameArgs = (List<String>) Launch.blackboard.get("ArgumentList");
        LOG.info("Launching wrapped Minecraft {{}}", target.getDeclaringClass().getName());
        try {
            target.invoke(null, (Object) gameArgs.toArray(String[]::new));
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    static LaunchClassLoader createClassLoader(List<URL> classpath) {
        LaunchClassLoader loader = new LaunchClassLoader(classpath.toArray(URL[]::new));
        PLATFORM_EXCLUSIONS.forEach(loader::addClassLoaderExclusion);
        if (LazarusAgent.installed()) {
            LazarusAgent.openJdkTo(loader.getUnnamedModule());
        }
        return loader;
    }

    /**
     * Runs the tweaker chain and resolves the launch target.
     *
     * <p>Tweakers may queue further tweakers during {@code injectIntoClassLoader} by adding to the {@code
     * TweakClasses} list on the blackboard, so the chain repeats until that list stays empty. The first tweaker
     * constructed is the primary tweaker and names the launch target. On return, the blackboard's {@code
     * ArgumentList} holds the game's arguments.
     *
     * @return the launch target's {@code main(String[])} method
     */
    static Method runTweakers(ClassLoader loader, Map<String, Object> blackboard, LaunchArguments parsed)
            throws ReflectiveOperationException {
        List<String> tweakClassNames = new ArrayList<>(parsed.tweakClasses());
        List<String> argumentList = new ArrayList<>();
        List<ITweaker> tweakers = new ArrayList<>();
        List<ITweaker> allTweakers = new ArrayList<>();
        blackboard.put("TweakClasses", tweakClassNames);
        blackboard.put("ArgumentList", argumentList);
        blackboard.put("Tweaks", tweakers);

        Set<String> seen = new HashSet<>();
        ITweaker primary = null;
        do {
            for (String name : List.copyOf(tweakClassNames)) {
                tweakClassNames.remove(name);
                if (!seen.add(name)) {
                    LOG.warn("Tweak class {} is already queued, skipping", name);
                    continue;
                }
                int dot = name.lastIndexOf('.');
                if (dot > 0 && loader instanceof LaunchClassLoader launch) {
                    launch.addClassLoaderExclusion(name.substring(0, dot));
                }
                LOG.info("Loading tweak class {}", name);
                ITweaker tweaker = (ITweaker)
                        Class.forName(name, true, loader).getConstructor().newInstance();
                tweakers.add(tweaker);
                if (primary == null) {
                    LOG.info("Using primary tweak class {}", name);
                    primary = tweaker;
                }
            }
            for (ITweaker tweaker : List.copyOf(tweakers)) {
                tweakers.remove(tweaker);
                LOG.info("Calling tweak class {}", tweaker.getClass().getName());
                tweaker.acceptOptions(
                        new ArrayList<>(parsed.passthrough()), parsed.gameDir(), parsed.assetsDir(), parsed.profile());
                if (loader instanceof LaunchClassLoader launch) {
                    tweaker.injectIntoClassLoader(launch);
                }
                allTweakers.add(tweaker);
            }
        } while (!tweakClassNames.isEmpty());

        for (ITweaker tweaker : allTweakers) {
            argumentList.addAll(List.of(tweaker.getLaunchArguments()));
        }
        if (primary == null) {
            throw new IllegalStateException("no tweak class could be loaded");
        }
        return Class.forName(primary.getLaunchTarget(), false, loader).getMethod("main", String[].class);
    }
}
