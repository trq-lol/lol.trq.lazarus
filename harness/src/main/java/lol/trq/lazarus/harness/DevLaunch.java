/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.harness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Development launcher: downloads Minecraft 1.8.9 and Forge, builds the class path, and boots the game on a chosen
 * JDK, either the stock way (LaunchWrapper, Java 8) or through Lazarus.
 *
 * <p>Arguments are {@code key=value} pairs: {@code java} (path to a java executable, required), {@code mode} ({@code
 * lazarus} or {@code stock}), {@code runtime} (the Lazarus runtime jar), {@code extra} (extra class path jars,
 * separated by the path separator), {@code mods} (jars copied into the mods directory), {@code cache}, {@code
 * gameDir}, {@code timeout} (seconds).
 */
public final class DevLaunch {
    static final String MC = "1.8.9";
    static final String FORGE = "1.8.9-11.15.1.2318-1.8.9";
    static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    static final String FORGE_MAVEN = "https://maven.minecraftforge.net/";
    static final String MOJANG_LIBS = "https://libraries.minecraft.net/";

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();
    private final Path cache;

    DevLaunch(Path cache) {
        this.cache = cache;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> opts = new LinkedHashMap<>();
        for (String arg : args) {
            int eq = arg.indexOf('=');
            if (eq > 0) {
                opts.put(arg.substring(0, eq), arg.substring(eq + 1));
            }
        }
        String javaExe = require(opts, "java");
        String mode = opts.getOrDefault("mode", "lazarus");
        Path cache = Path.of(opts.getOrDefault("cache", "build/harness-cache")).toAbsolutePath();
        Path gameDir = Path.of(opts.getOrDefault("gameDir", "build/harness-run/" + mode))
                .toAbsolutePath();
        long timeout = Long.parseLong(opts.getOrDefault("timeout", "240"));

        DevLaunch launch = new DevLaunch(cache);
        List<Path> classpath = new ArrayList<>();
        Path natives = launch.vanilla(classpath);
        boolean lazarus = !mode.equals("stock");
        launch.forge(classpath, mode.equals("lazarus") ? lazarusLibraries(Path.of(require(opts, "runtime"))) : null);
        if (mode.equals("lazarus")) {
            classpath.add(0, Path.of(require(opts, "runtime")).toAbsolutePath());
        } else if (mode.equals("bare")) {
            // Worst case for the bootstrap: stock Forge class path with asm-all, Lazarus last, no agent, no flags.
            classpath.add(Path.of(require(opts, "runtime")).toAbsolutePath());
        }
        for (String extra : opts.getOrDefault("extra", "").split(File.pathSeparator)) {
            if (!extra.isBlank()) {
                classpath.add(Path.of(extra).toAbsolutePath());
            }
        }

        Files.createDirectories(gameDir.resolve("mods"));
        try (var old = Files.list(gameDir.resolve("mods"))) {
            for (Path p : old.toList()) {
                Files.deleteIfExists(p);
            }
        }
        for (String mod : opts.getOrDefault("mods", "").split(File.pathSeparator)) {
            if (!mod.isBlank()) {
                Path src = Path.of(mod);
                Files.copy(
                        src, gameDir.resolve("mods").resolve(src.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        writeMutedOptions(gameDir);

        List<String> command = new ArrayList<>();
        command.add(javaExe);
        command.add("-Xmx2G");
        command.add("-Djava.library.path=" + natives);
        command.add("-Dfml.ignoreInvalidMinecraftCertificates=true");
        command.add("-Dfml.ignorePatchDiscrepancies=true");
        if (mode.equals("lazarus")) {
            command.add("-javaagent:" + Path.of(require(opts, "runtime")).toAbsolutePath());
            command.addAll(List.of(
                    "-XX:+IgnoreUnrecognizedVMOptions",
                    "--enable-native-access=ALL-UNNAMED",
                    "--sun-misc-unsafe-memory-access=allow"));
            command.addAll(List.of(opts.getOrDefault("jvmArgs", "").split(" ")).stream()
                    .filter(s -> !s.isBlank())
                    .toList());
        }
        command.add("-cp");
        command.add(String.join(
                File.pathSeparator, classpath.stream().map(Path::toString).toList()));
        command.add(lazarus ? "lol.trq.lazarus.launch.Lazarus" : "net.minecraft.launchwrapper.Launch");
        command.addAll(List.of(
                "--username", "Lazarus",
                "--version", FORGE,
                "--gameDir", gameDir.toString(),
                "--assetsDir", cache.resolve("assets").toString(),
                "--assetIndex", "1.8",
                "--uuid", "00000000000000000000000000000000",
                "--accessToken", "0",
                "--userProperties", "{}",
                "--userType", "legacy",
                "--tweakClass", "net.minecraftforge.fml.common.launcher.FMLTweaker"));

        System.out.println("[harness] " + mode + " on " + javaExe);
        Process process = new ProcessBuilder(command)
                .directory(gameDir.toFile())
                .inheritIO()
                .start();
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            System.out.println("[harness] TIMEOUT after " + timeout + "s");
            System.exit(124);
        }
        System.out.println("[harness] exit " + process.exitValue());
        System.exit(process.exitValue());
    }

    private Path vanilla(List<Path> classpath) throws IOException, InterruptedException {
        JsonObject manifest = json(fetch(MANIFEST, cache.resolve("version_manifest_v2.json"), false));
        String url = null;
        for (JsonElement v : manifest.getAsJsonArray("versions")) {
            if (v.getAsJsonObject().get("id").getAsString().equals(MC)) {
                url = v.getAsJsonObject().get("url").getAsString();
            }
        }
        JsonObject version = json(fetch(url, cache.resolve("versions/" + MC + ".json"), true));
        Path natives = cache.resolve("natives/" + MC);
        Files.createDirectories(natives);
        for (JsonElement el : version.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            if (!allowed(lib)) {
                continue;
            }
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (downloads.has("artifact")) {
                JsonObject art = downloads.getAsJsonObject("artifact");
                classpath.add(fetch(
                        art.get("url").getAsString(),
                        cache.resolve("libraries/" + art.get("path").getAsString()),
                        true));
            }
            if (lib.has("natives") && lib.getAsJsonObject("natives").has(osKey())) {
                String classifier = lib.getAsJsonObject("natives")
                        .get(osKey())
                        .getAsString()
                        .replace("${arch}", "64");
                JsonObject art = downloads.getAsJsonObject("classifiers").getAsJsonObject(classifier);
                Path jar = fetch(
                        art.get("url").getAsString(),
                        cache.resolve("libraries/" + art.get("path").getAsString()),
                        true);
                extractNatives(jar, natives);
            }
        }
        JsonObject client = version.getAsJsonObject("downloads").getAsJsonObject("client");
        classpath.add(fetch(client.get("url").getAsString(), cache.resolve("versions/" + MC + ".jar"), true));
        return natives;
    }

    private void forge(List<Path> classpath, JsonObject lazarus) throws IOException, InterruptedException {
        List<String> removed = new ArrayList<>();
        if (lazarus != null) {
            lazarus.getAsJsonArray("remove").forEach(e -> removed.add(e.getAsString()));
        }
        String path = "net/minecraftforge/forge/" + FORGE + "/forge-" + FORGE + "-universal.jar";
        Path universal = fetch(FORGE_MAVEN + path, cache.resolve("libraries/" + path), true);
        JsonObject profile;
        try (ZipFile zip = new ZipFile(universal.toFile())) {
            try (InputStream in = zip.getInputStream(zip.getEntry("version.json"))) {
                profile = JsonParser.parseString(new String(in.readAllBytes())).getAsJsonObject();
            }
        }
        List<Path> forgeLibs = new ArrayList<>();
        for (JsonElement el : profile.getAsJsonArray("libraries")) {
            JsonObject lib = el.getAsJsonObject();
            String name = lib.get("name").getAsString();
            if (name.startsWith("net.minecraftforge:forge:")) {
                continue;
            }
            if (lib.has("clientreq") && !lib.get("clientreq").getAsBoolean()) {
                continue;
            }
            if (removed.stream().anyMatch(r -> name.startsWith(r + ":"))) {
                continue;
            }
            if (alreadyPresent(classpath, name)) {
                continue;
            }
            String base = lib.has("url") ? lib.get("url").getAsString() : MOJANG_LIBS;
            forgeLibs.add(mavenWithFallback(base, name));
        }
        if (lazarus != null) {
            JsonArray add = lazarus.has("add") ? lazarus.getAsJsonArray("add") : new JsonArray();
            for (JsonElement el : add) {
                JsonObject lib = el.getAsJsonObject();
                forgeLibs.add(
                        maven(lib.get("url").getAsString(), lib.get("name").getAsString()));
            }
        }
        classpath.addAll(0, forgeLibs);
        classpath.add(0, universal);
    }

    /** Reads {@code libraries.json} from the Lazarus runtime jar. */
    static JsonObject lazarusLibraries(Path runtime) throws IOException {
        try (ZipFile zip = new ZipFile(runtime.toFile())) {
            try (InputStream in = zip.getInputStream(zip.getEntry("lol/trq/lazarus/libraries.json"))) {
                return JsonParser.parseString(new String(in.readAllBytes())).getAsJsonObject();
            }
        }
    }

    private static boolean alreadyPresent(List<Path> classpath, String coordinate) {
        String[] parts = coordinate.split(":");
        String file = parts[1] + "-" + parts[2] + ".jar";
        return classpath.stream().anyMatch(p -> p.getFileName().toString().equals(file));
    }

    private Path mavenWithFallback(String base, String coordinate) throws IOException, InterruptedException {
        List<String> bases =
                new ArrayList<>(List.of(base, FORGE_MAVEN, MOJANG_LIBS, "https://repo1.maven.org/maven2/"));
        IOException last = null;
        for (String b : bases) {
            try {
                return maven(b, coordinate);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }

    private Path maven(String base, String coordinate) throws IOException, InterruptedException {
        String[] parts = coordinate.split(":");
        String path =
                parts[0].replace('.', '/') + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return fetch(base + path, cache.resolve("libraries/" + path), true);
    }

    private Path fetch(String url, Path target, boolean cached) throws IOException, InterruptedException {
        if (cached && Files.isRegularFile(target) && Files.size(target) > 0) {
            return target;
        }
        Files.createDirectories(target.getParent());
        HttpResponse<Path> response = http.send(
                HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "lazarus-harness")
                        .build(),
                HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(target);
            throw new IOException("GET " + url + " -> " + response.statusCode());
        }
        System.out.println("[harness] fetched " + url);
        return target;
    }

    private static boolean allowed(JsonObject lib) {
        if (!lib.has("rules")) {
            return true;
        }
        boolean allow = false;
        for (JsonElement el : lib.getAsJsonArray("rules")) {
            JsonObject rule = el.getAsJsonObject();
            boolean matches = !rule.has("os")
                    || rule.getAsJsonObject("os").get("name").getAsString().equals(osKey());
            if (matches) {
                allow = rule.get("action").getAsString().equals("allow");
            }
        }
        return allow;
    }

    static String osKey() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win") ? "windows" : os.contains("mac") ? "osx" : "linux";
    }

    private static void extractNatives(Path jar, Path dir) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry e : java.util.Collections.list(zip.entries())) {
                if (e.isDirectory() || e.getName().startsWith("META-INF")) {
                    continue;
                }
                Path out = dir.resolve(e.getName()).normalize();
                if (!out.startsWith(dir)) {
                    continue;
                }
                Files.createDirectories(out.getParent());
                try (InputStream in = zip.getInputStream(e)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void writeMutedOptions(Path gameDir) throws IOException {
        Path options = gameDir.resolve("options.txt");
        if (!Files.exists(options)) {
            Files.writeString(options, "soundCategory_master:0.0\nmusicVolume:0.0\nfboEnable:true\n");
        }
    }

    private static JsonObject json(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static String require(Map<String, String> opts, String key) {
        String v = opts.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("missing " + key + "=...");
        }
        return v;
    }
}
