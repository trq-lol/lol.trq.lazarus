/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.harness;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Launches a profile from a launcher directory the way the official Minecraft Launcher does for a legacy version:
 * libraries child first, the child's main class, the inherited {@code minecraftArguments}, and the profile's {@code
 * javaArgs} split on spaces. Used to check what the installer writes.
 *
 * <p>Arguments: {@code java}, {@code mcDir}, {@code profile} (key in {@code launcher_profiles.json}), {@code cache},
 * {@code gameDir}, {@code mods}, {@code timeout}.
 */
public final class ProfileLaunch {
    private ProfileLaunch() {}

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new LinkedHashMap<>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq > 0) {
                opts.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }
        Path mcDir = Path.of(opts.get("mcDir"));
        Path gameDir = Path.of(opts.get("gameDir")).toAbsolutePath();
        Path cache = Path.of(opts.get("cache")).toAbsolutePath();
        JsonObject profile = json(mcDir.resolve("launcher_profiles.json"))
                .getAsJsonObject("profiles")
                .getAsJsonObject(opts.get("profile"));
        String versionId = profile.get("lastVersionId").getAsString();

        List<JsonObject> chain = new ArrayList<>();
        String id = versionId;
        while (id != null && !id.equals(DevLaunch.MC)) {
            JsonObject v = json(mcDir.resolve("versions").resolve(id).resolve(id + ".json"));
            chain.add(v);
            id = v.has("inheritsFrom") ? v.get("inheritsFrom").getAsString() : null;
        }

        List<Path> classpath = new ArrayList<>();
        String mainClass = null;
        String minecraftArguments = null;
        for (JsonObject v : chain) {
            if (mainClass == null && v.has("mainClass")) {
                mainClass = v.get("mainClass").getAsString();
            }
            if (minecraftArguments == null && v.has("minecraftArguments")) {
                minecraftArguments = v.get("minecraftArguments").getAsString();
            }
            for (JsonElement el : v.getAsJsonArray("libraries")) {
                JsonObject lib = el.getAsJsonObject();
                if (lib.has("clientreq") && !lib.get("clientreq").getAsBoolean()) {
                    continue;
                }
                if (!DevLaunch.allowed(lib)) {
                    continue;
                }
                String path = lib.has("downloads")
                        ? lib.getAsJsonObject("downloads")
                                .getAsJsonObject("artifact")
                                .get("path")
                                .getAsString()
                        : mavenPath(lib.get("name").getAsString());
                Path local = mcDir.resolve("libraries").resolve(path);
                classpath.add(
                        Files.exists(local) ? local : cache.resolve("libraries").resolve(path));
            }
        }
        DevLaunch vanilla = new DevLaunch(cache);
        Path natives = vanilla.vanilla(classpath);

        Files.createDirectories(gameDir.resolve("mods"));
        for (String mod : opts.getOrDefault("mods", "").split(File.pathSeparator)) {
            if (!mod.isBlank()) {
                Path src = Path.of(mod);
                Files.copy(
                        src,
                        gameDir.resolve("mods").resolve(src.getFileName()),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!Files.exists(gameDir.resolve("options.txt"))) {
            Files.writeString(gameDir.resolve("options.txt"), "soundCategory_master:0.0\nmusicVolume:0.0\n");
        }

        List<String> command = new ArrayList<>();
        command.add(opts.get("java"));
        for (String a : profile.get("javaArgs").getAsString().split(" ")) {
            if (!a.isBlank()) {
                command.add(a);
            }
        }
        command.add("-Djava.library.path=" + natives);
        for (String a : opts.getOrDefault("jvmArgs", "").split(" ")) {
            if (!a.isBlank()) {
                command.add(a);
            }
        }
        command.add("-cp");
        command.add(String.join(
                File.pathSeparator, classpath.stream().map(Path::toString).toList()));
        command.add(mainClass);
        String substituted = minecraftArguments
                .replace("${auth_player_name}", "Lazarus")
                .replace("${version_name}", versionId)
                .replace("${game_directory}", gameDir.toString())
                .replace("${assets_root}", cache.resolve("assets").toString())
                .replace("${assets_index_name}", "1.8")
                .replace("${auth_uuid}", "00000000000000000000000000000000")
                .replace("${auth_access_token}", "0")
                .replace("${user_properties}", "{}")
                .replace("${user_type}", "legacy");
        command.addAll(List.of(substituted.split(" ")));

        System.out.println("[profile] " + versionId + " main=" + mainClass + " javaArgs=" + profile.get("javaArgs"));
        Process process = new ProcessBuilder(command)
                .directory(mcDir.toFile())
                .inheritIO()
                .start();
        long timeout = Long.parseLong(opts.getOrDefault("timeout", "240"));
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            System.out.println("[profile] TIMEOUT");
            System.exit(124);
        }
        System.out.println("[profile] exit " + process.exitValue());
        System.exit(process.exitValue());
    }

    static String mavenPath(String coordinate) {
        String[] p = coordinate.split(":");
        return p[0].replace('.', '/') + "/" + p[1] + "/" + p[2] + "/" + p[1] + "-" + p[2] + ".jar";
    }

    private static JsonObject json(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }
}
