/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.smoke;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Test mod for the harness: when the main menu opens, prints a report line and exits the game.
 *
 * <p>The report names the Java version, the loaded mods, and OptiFine's version when OptiFine is installed.
 */
@Mod(modid = "lazarus_smoke", name = "Lazarus Smoke", version = "1", acceptableRemoteVersions = "*")
public class SmokeMod {
    private static final String MAIN_MENU = "net.minecraft.client.gui.GuiMainMenu";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) throws ReflectiveOperationException {
        Object gui = event.getClass().getField("gui").get(event);
        if (gui == null || !gui.getClass().getName().equals(MAIN_MENU)) {
            return;
        }
        StringBuilder mods = new StringBuilder();
        Loader.instance()
                .getActiveModList()
                .forEach(m -> mods.append(m.getModId()).append(' '));
        System.out.println("LAZARUS-SMOKE OK java=" + System.getProperty("java.version") + " optifine="
                + optifineVersion() + " mods=[" + mods.toString().trim() + "]");
        if (!Boolean.getBoolean("lazarus.smoke.stay")) {
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    private static String optifineVersion() {
        try {
            Class<?> config = Class.forName("Config", false, SmokeMod.class.getClassLoader());
            return String.valueOf(config.getMethod("getVersion").invoke(null));
        } catch (ReflectiveOperationException | LinkageError e) {
            return "absent";
        }
    }
}
