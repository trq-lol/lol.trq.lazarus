/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.install;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;
import lol.trq.lazarus.launch.Bootstrap;

/**
 * Installs Lazarus into the official Minecraft Launcher's directory on top of an installed Forge 1.8.9.
 *
 * <p>It copies the Lazarus jar into {@code libraries/}, writes a version that inherits the Forge version and swaps in
 * the Lazarus main class and a modern Java runtime, and adds a launcher profile that passes the agent and JVM flags.
 * Existing files are only ever added to; {@code launcher_profiles.json} is backed up before it is changed.
 */
public final class VanillaInstall {
    /** The Mojang Java runtime the version asks the launcher for. */
    static final String RUNTIME_COMPONENT = "java-runtime-delta";

    static final int RUNTIME_MAJOR = 21;
    static final String MAIN_CLASS = "lol.trq.lazarus.launch.Lazarus";

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path minecraftDir;
    private final Path selfJar;
    private final String version;

    /**
     * Creates an installer.
     *
     * @param minecraftDir the launcher's game directory, such as {@code %APPDATA%\.minecraft}
     * @param selfJar the Lazarus jar to install
     * @param version the Lazarus version
     */
    public VanillaInstall(Path minecraftDir, Path selfJar, String version) {
        this.minecraftDir = minecraftDir;
        this.selfJar = selfJar;
        this.version = version;
    }

    /** {@return the default game directory for this operating system} */
    public static Path defaultMinecraftDir() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, ".minecraft");
        }
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "minecraft");
        }
        return Path.of(home, ".minecraft");
    }

    /** {@return the installed Forge 1.8.9 version ids, newest build first} */
    public List<String> forgeVersions() throws IOException {
        Path versions = minecraftDir.resolve("versions");
        if (!Files.isDirectory(versions)) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        try (Stream<Path> dirs = Files.list(versions)) {
            for (Path dir : dirs.toList()) {
                String id = dir.getFileName().toString();
                if (id.startsWith("1.8.9-forge")
                        && !id.endsWith("-lazarus")
                        && Files.isRegularFile(dir.resolve(id + ".json"))) {
                    found.add(id);
                }
            }
        }
        found.sort((a, b) -> b.compareTo(a));
        return found;
    }

    /**
     * Installs Lazarus on top of a Forge version.
     *
     * @param forgeId the Forge version id, such as {@code 1.8.9-forge1.8.9-11.15.1.2318-1.8.9}
     * @param profileName the launcher profile's display name
     * @param maxMemoryMb the profile's {@code -Xmx}, in megabytes
     * @return what was installed
     * @throws IOException if a file cannot be read or written
     */
    public Result install(String forgeId, String profileName, int maxMemoryMb) throws IOException {
        return install(forgeId, profileName, maxMemoryMb, false);
    }

    /**
     * Installs Lazarus on top of a Forge version, optionally on LWJGL 3.
     *
     * <p>The LWJGL 3 install is a separate version and profile; the LWJGL 2 one keeps working beside it. The launcher
     * downloads LWJGL 3 from Maven Central, verified by SHA-1. Minecraft's own LWJGL 2 stays in the inherited library
     * list, which a launcher cannot remove; Lazarus drops it from the class path when it starts.
     *
     * @param forgeId the Forge version id
     * @param profileName the launcher profile's display name
     * @param maxMemoryMb the profile's {@code -Xmx}, in megabytes
     * @param lwjgl3 whether to run on LWJGL 3
     * @return what was installed
     * @throws IOException if a file cannot be read or written
     */
    public Result install(String forgeId, String profileName, int maxMemoryMb, boolean lwjgl3) throws IOException {
        if (!forgeId.matches("[A-Za-z0-9._-]+") || forgeId.contains("..")) {
            throw new IOException("Not a version id: " + forgeId);
        }
        Path forgeJson = minecraftDir.resolve("versions").resolve(forgeId).resolve(forgeId + ".json");
        if (!Files.isRegularFile(forgeJson)) {
            throw new IOException("Forge version " + forgeId + " is not installed in " + minecraftDir
                    + ". Install Forge 1.8.9 with its installer first.");
        }

        String libraryPath = "lol/trq/lazarus/lol.trq.lazarus/" + version + "/lol.trq.lazarus-" + version + ".jar";
        Path library = minecraftDir.resolve("libraries").resolve(libraryPath);
        Files.createDirectories(library.getParent());
        if (!selfJar.toAbsolutePath().equals(library.toAbsolutePath())) {
            Files.copy(selfJar, library, StandardCopyOption.REPLACE_EXISTING);
        }

        String versionId = forgeId + (lwjgl3 ? "-lazarus-lwjgl3" : "-lazarus");
        Path versionJson = minecraftDir.resolve("versions").resolve(versionId).resolve(versionId + ".json");
        Files.createDirectories(versionJson.getParent());
        JsonObject json = versionJson(versionId, forgeId, libraryPath, library);
        if (lwjgl3) {
            addLwjgl3(json.getAsJsonArray("libraries"));
        }
        writeAtomically(versionJson, GSON.toJson(json));

        Path profiles = minecraftDir.resolve("launcher_profiles.json");
        JsonObject root = Files.isRegularFile(profiles)
                ? JsonParser.parseString(Files.readString(profiles)).getAsJsonObject()
                : new JsonObject();
        Path backup = profiles.resolveSibling("launcher_profiles.json.lazarus-backup");
        if (Files.isRegularFile(profiles) && !Files.exists(backup)) {
            Files.copy(profiles, backup);
        }
        if (!root.has("profiles")) {
            root.add("profiles", new JsonObject());
        }
        String profileKey = (lwjgl3 ? "lazarus-lwjgl3-" : "lazarus-") + forgeId;
        root.getAsJsonObject("profiles").add(profileKey, profile(profileName, versionId, library, maxMemoryMb));
        writeAtomically(profiles, GSON.toJson(root));
        return new Result(versionId, profileKey, library, versionJson);
    }

    /** Writes through a temporary file in the same directory, so a crash never leaves a half-written file. */
    private static void writeAtomically(Path target, String content) throws IOException {
        Path temp =
                Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            Files.writeString(temp, content);
            if (Files.exists(target)
                    && temp.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(temp, Files.getPosixFilePermissions(target));
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    JsonObject versionJson(String versionId, String forgeId, String libraryPath, Path library) throws IOException {
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        JsonObject json = new JsonObject();
        json.addProperty("id", versionId);
        json.addProperty("inheritsFrom", forgeId);
        json.addProperty("type", "release");
        json.addProperty("time", now);
        json.addProperty("releaseTime", now);
        json.addProperty("mainClass", MAIN_CLASS);

        JsonObject artifact = new JsonObject();
        artifact.addProperty("path", libraryPath);
        artifact.addProperty("sha1", sha1(library));
        artifact.addProperty("size", Files.size(library));
        artifact.addProperty("url", "");
        JsonObject downloads = new JsonObject();
        downloads.add("artifact", artifact);
        JsonObject lib = new JsonObject();
        lib.addProperty("name", "lol.trq.lazarus:lol.trq.lazarus:" + version);
        lib.add("downloads", downloads);
        JsonArray libraries = new JsonArray();
        libraries.add(lib);
        json.add("libraries", libraries);

        JsonObject java = new JsonObject();
        java.addProperty("component", RUNTIME_COMPONENT);
        java.addProperty("majorVersion", RUNTIME_MAJOR);
        json.add("javaVersion", java);
        return json;
    }

    /** Adds the bundled LWJGL 3 compatibility jar and the LWJGL 3 artifacts, natives limited to their OS. */
    private void addLwjgl3(JsonArray libraries) throws IOException {
        String compatPath =
                "lol/trq/lazarus/lol.trq.lazarus-lwjgl3/" + version + "/lol.trq.lazarus-lwjgl3-" + version + ".jar";
        Path compat = minecraftDir.resolve("libraries").resolve(compatPath);
        Files.createDirectories(compat.getParent());
        try (var in = VanillaInstall.class.getResourceAsStream("/META-INF/lazarus/lwjgl3.jar")) {
            if (in == null) {
                throw new IOException("this Lazarus jar does not contain the LWJGL 3 compatibility layer");
            }
            Files.copy(in, compat, StandardCopyOption.REPLACE_EXISTING);
        }
        libraries.add(library(
                "lol.trq.lazarus:lol.trq.lazarus-lwjgl3:" + version,
                compatPath,
                "",
                sha1(compat),
                Files.size(compat),
                null));
        JsonObject manifest;
        try (var in = VanillaInstall.class.getResourceAsStream("/META-INF/lazarus/lwjgl3-libraries.json")) {
            if (in == null) {
                throw new IOException("this Lazarus jar does not contain the LWJGL 3 library list");
            }
            manifest = JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        for (var el : manifest.getAsJsonArray("libraries")) {
            JsonObject l = el.getAsJsonObject();
            String os = l.has("os") && !l.get("os").isJsonNull() ? l.get("os").getAsString() : null;
            libraries.add(library(
                    l.get("name").getAsString(),
                    l.get("path").getAsString(),
                    l.get("url").getAsString(),
                    l.get("sha1").getAsString(),
                    l.get("size").getAsLong(),
                    os));
        }
    }

    private static JsonObject library(String name, String path, String url, String sha1, long size, String os) {
        JsonObject artifact = new JsonObject();
        artifact.addProperty("path", path);
        artifact.addProperty("sha1", sha1);
        artifact.addProperty("size", size);
        artifact.addProperty("url", url);
        JsonObject downloads = new JsonObject();
        downloads.add("artifact", artifact);
        JsonObject lib = new JsonObject();
        lib.addProperty("name", name);
        lib.add("downloads", downloads);
        if (os != null) {
            JsonObject osRule = new JsonObject();
            osRule.addProperty("name", os);
            JsonObject rule = new JsonObject();
            rule.addProperty("action", "allow");
            rule.add("os", osRule);
            JsonArray rules = new JsonArray();
            rules.add(rule);
            lib.add("rules", rules);
        }
        return lib;
    }

    static JsonObject profile(String name, String versionId, Path library, int maxMemoryMb) {
        String now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        List<String> args = new ArrayList<>();
        args.add("-Xmx" + maxMemoryMb + "M");
        // The launcher splits javaArgs on spaces, and a broken -javaagent path stops the JVM. Leave the agent out when
        // the path has whitespace; Lazarus then relaunches itself with the agent.
        String agentPath = library.toAbsolutePath().toString();
        if (agentPath.chars().noneMatch(Character::isWhitespace)) {
            args.add("-javaagent:" + agentPath);
        }
        args.addAll(Bootstrap.JVM_FLAGS);
        JsonObject profile = new JsonObject();
        profile.addProperty("name", name);
        profile.addProperty("type", "custom");
        profile.addProperty("lastVersionId", versionId);
        profile.addProperty("icon", "Furnace");
        profile.addProperty("javaArgs", String.join(" ", args));
        profile.addProperty("created", now);
        profile.addProperty("lastUsed", now);
        return profile;
    }

    static String sha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * What an install wrote.
     *
     * @param versionId the new version's id
     * @param profileKey the launcher profile's key in {@code launcher_profiles.json}
     * @param library the installed Lazarus jar
     * @param versionJson the new version's JSON file
     */
    public record Result(String versionId, String profileKey, Path library, Path versionJson) {
        String describe() {
            return "Installed " + versionId + "\n  library: " + library + "\n  version: " + versionJson
                    + "\n  profile: " + profileKey + "\nOpen the Minecraft Launcher and pick the \"" + versionId
                    + "\" installation.";
        }
    }
}
