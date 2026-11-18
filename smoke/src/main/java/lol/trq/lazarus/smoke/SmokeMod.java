/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.smoke;

import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Test mod for the harness: a couple of seconds after the main menu opens, prints a report line and exits the game.
 *
 * <p>The report names the Java version, the loaded mods, and OptiFine's version when OptiFine is installed.
 */
@Mod(modid = "lazarus_smoke", name = "Lazarus Smoke", version = "1", acceptableRemoteVersions = "*")
public class SmokeMod {
    private static final String MAIN_MENU = "net.minecraft.client.gui.GuiMainMenu";

    /** Ticks to keep the main menu up before exiting, so it initializes and renders. */
    private static final int MENU_TICKS = Integer.getInteger("lazarus.smoke.ticks", 40);

    private int menuTicks = -1;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) throws ReflectiveOperationException {
        Object gui = event.getClass().getField("gui").get(event);
        if (menuTicks < 0 && gui != null && gui.getClass().getName().equals(MAIN_MENU)) {
            menuTicks = 0;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || menuTicks < 0 || ++menuTicks < MENU_TICKS) {
            return;
        }
        menuTicks = Integer.MIN_VALUE;
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
