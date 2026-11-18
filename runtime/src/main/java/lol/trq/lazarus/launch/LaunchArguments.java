/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The command line LaunchWrapper understands, split into the options it consumes and the arguments it passes on.
 *
 * <p>{@code --version}, {@code --gameDir}, {@code --assetsDir} and {@code --tweakClass} are consumed; tweakers
 * receive the first three through {@code acceptOptions} and add them back to the game's arguments themselves.
 * Everything else is passed to the tweakers unchanged.
 *
 * @param profile the {@code --version} value, or {@code "UnknownTweaker"} when absent
 * @param gameDir the {@code --gameDir} value, or the working directory
 * @param assetsDir the {@code --assetsDir} value, or {@code null}
 * @param tweakClasses every {@code --tweakClass} value, in order
 * @param passthrough the arguments tweakers receive, in order
 */
public record LaunchArguments(
        String profile, File gameDir, File assetsDir, List<String> tweakClasses, List<String> passthrough) {

    /** The primary tweaker when none is given, matching LaunchWrapper. */
    public static final String DEFAULT_TWEAKER = "net.minecraft.launchwrapper.VanillaTweaker";

    /**
     * Parses a LaunchWrapper command line.
     *
     * @param args the raw arguments
     * @return the parsed arguments
     * @throws IllegalArgumentException if an option that takes a value has none
     */
    public static LaunchArguments parse(String[] args) {
        String profile = null;
        File gameDir = null;
        File assetsDir = null;
        List<String> tweaks = new ArrayList<>();
        List<String> rest = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String name = arg;
            String value = null;
            int eq = arg.indexOf('=');
            if (arg.startsWith("--") && eq > 0) {
                name = arg.substring(0, eq);
                value = arg.substring(eq + 1);
            }
            if (!isOption(name)) {
                rest.add(arg);
                continue;
            }
            if (value == null) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException(name + " needs a value");
                }
                value = args[++i];
            }
            switch (name) {
                case "--version" -> profile = value;
                case "--gameDir" -> gameDir = new File(value);
                case "--assetsDir" -> assetsDir = new File(value);
                default -> tweaks.add(value);
            }
        }
        if (tweaks.isEmpty()) {
            tweaks.add(DEFAULT_TWEAKER);
        }
        return new LaunchArguments(
                profile == null ? "UnknownTweaker" : profile,
                gameDir == null ? new File(".") : gameDir,
                assetsDir,
                List.copyOf(tweaks),
                List.copyOf(rest));
    }

    private static boolean isOption(String name) {
        return switch (name) {
            case "--version", "--gameDir", "--assetsDir", "--tweakClass" -> true;
            default -> false;
        };
    }
}
