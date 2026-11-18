/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;

class LaunchTest {
    @Test
    void parsesLaunchWrapperOptions() {
        LaunchArguments args = LaunchArguments.parse(new String[] {
            "--username",
            "a",
            "--version",
            "forge",
            "--gameDir=game",
            "--assetsDir",
            "assets",
            "--tweakClass",
            "one.Tweaker",
            "--tweakClass=two.Tweaker",
            "--accessToken",
            "0"
        });
        assertEquals("forge", args.profile());
        assertEquals(new File("game"), args.gameDir());
        assertEquals(new File("assets"), args.assetsDir());
        assertEquals(List.of("one.Tweaker", "two.Tweaker"), args.tweakClasses());
        assertEquals(List.of("--username", "a", "--accessToken", "0"), args.passthrough());
    }

    @Test
    void defaultsMatchLaunchWrapper() {
        LaunchArguments args = LaunchArguments.parse(new String[0]);
        assertEquals("UnknownTweaker", args.profile());
        assertEquals(new File("."), args.gameDir());
        assertNull(args.assetsDir());
        assertEquals(List.of(LaunchArguments.DEFAULT_TWEAKER), args.tweakClasses());
    }

    @Test
    void optionWithoutValueFails() {
        assertThrows(IllegalArgumentException.class, () -> LaunchArguments.parse(new String[] {"--tweakClass"}));
    }

    @Test
    void classpathPutsLazarusFirstAndDropsAsm5() {
        File self = new File("libs/lol.trq.lazarus-0.1.0.jar");
        List<String> fixed = Bootstrap.fixClasspath(
                List.of(
                        "libs/forge.jar",
                        "libs/asm-all-5.0.3.jar",
                        self.getPath(),
                        "libs/asm-debug-all-5.0.3.jar",
                        "mc.jar"),
                self);
        assertEquals(List.of(self.getAbsolutePath(), "libs/forge.jar", "mc.jar"), fixed);
    }

    @Test
    void safetyFlagsAlwaysWin() {
        assertTrue(Bootstrap.JVM_FLAGS.contains("-XX:-UseBASE64Intrinsics"));
        assertTrue(Bootstrap.overridesSafetyFlag("-XX:+UseBASE64Intrinsics"));
        assertTrue(Bootstrap.overridesSafetyFlag("-XX:-UseBASE64Intrinsics"));
        assertEquals(false, Bootstrap.overridesSafetyFlag("-Xmx2G"));
    }

    @Test
    void argfileQuotesAndEscapes() {
        String file = Bootstrap.toArgfile(List.of("-cp", "C:\\Users\\A B\\x.jar", "say \"hi\""));
        String nl = System.lineSeparator();
        assertEquals("\"-cp\"" + nl + "\"C:\\\\Users\\\\A B\\\\x.jar\"" + nl + "\"say \\\"hi\\\"\"" + nl, file);
    }

    @Test
    void platformExclusionsCoverJdkPackagesOutsideJava() {
        for (String pkg : List.of("javax.", "jdk.", "com.sun.", "lol.trq.lazarus.agent.")) {
            assertTrue(Lazarus.PLATFORM_EXCLUSIONS.contains(pkg), pkg);
        }
    }
}
