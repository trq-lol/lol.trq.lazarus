/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.smoke;

import java.io.File;
import java.lang.reflect.Method;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Test mod for the harness. When the main menu has been up for a while it prints a report line; with {@code
 * -Dlazarus.smoke.world=true} it then creates a singleplayer world, plays in it for a while and reports again. It
 * exits the game at the end unless {@code -Dlazarus.smoke.stay=true}.
 *
 * <p>The mod is compiled against Forge only. Minecraft is reached by reflection with SRG names, the names Forge uses
 * at run time.
 */
@Mod(modid = "lazarus_smoke", name = "Lazarus Smoke", version = "1", acceptableRemoteVersions = "*")
public class SmokeMod {
    private static final String MAIN_MENU = "net.minecraft.client.gui.GuiMainMenu";
    private static final int MENU_TICKS = Integer.getInteger("lazarus.smoke.ticks", 40);
    private static final int WORLD_TICKS = Integer.getInteger("lazarus.smoke.worldTicks", 100);
    private static final boolean WORLD = Boolean.getBoolean("lazarus.smoke.world");
    private static final boolean SCREENSHOT = Boolean.getBoolean("lazarus.smoke.screenshot");

    private enum Stage {
        BOOTING,
        MENU,
        LOADING_WORLD,
        IN_WORLD,
        DONE
    }

    private Stage stage = Stage.BOOTING;
    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) throws ReflectiveOperationException {
        Object gui = event.getClass().getField("gui").get(event);
        if (stage == Stage.BOOTING && gui != null && gui.getClass().getName().equals(MAIN_MENU)) {
            stage = Stage.MENU;
            ticks = 0;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) throws ReflectiveOperationException {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        switch (stage) {
            case MENU:
                if (++ticks >= MENU_TICKS) {
                    log4shellProbe();
                    report("menu");
                    if (WORLD) {
                        stage = Stage.LOADING_WORLD;
                        ticks = 0;
                        launchWorld();
                    } else {
                        finish();
                    }
                }
                break;
            case LOADING_WORLD:
                if (field(minecraft(), "field_71439_g") != null) {
                    stage = Stage.IN_WORLD;
                    ticks = 0;
                }
                break;
            case IN_WORLD:
                if (++ticks >= WORLD_TICKS) {
                    report("world");
                    finish();
                }
                break;
            default:
                break;
        }
    }

    private void finish() {
        stage = Stage.DONE;
        if (SCREENSHOT) {
            screenshot();
        }
        if (!Boolean.getBoolean("lazarus.smoke.stay")) {
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    /**
     * Logs a Log4Shell payload that points at a local listener and reports whether Log4j connected to it, and whether
     * a harmless {@code sys} lookup was expanded in the message.
     */
    private static void log4shellProbe() {
        try (java.net.ServerSocket listener =
                new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            listener.setSoTimeout(3000);
            org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger("LazarusProbe");
            log.info("LAZARUS-PROBE sys=${sys:java.vendor} jndi=${jndi:ldap://127.0.0.1:" + listener.getLocalPort()
                    + "/x}");
            boolean connected;
            try (java.net.Socket ignored = listener.accept()) {
                connected = true;
            } catch (java.net.SocketTimeoutException e) {
                connected = false;
            }
            System.out.println("LAZARUS-LOG4SHELL jndiConnected=" + connected);
        } catch (java.io.IOException e) {
            System.out.println("LAZARUS-LOG4SHELL probe failed: " + e);
        }
    }

    private static void report(String where) {
        StringBuilder mods = new StringBuilder();
        Loader.instance()
                .getActiveModList()
                .forEach(m -> mods.append(m.getModId()).append(' '));
        System.out.println("LAZARUS-SMOKE OK at=" + where + " java=" + System.getProperty("java.version") + " optifine="
                + optifineVersion() + " mods=[" + mods.toString().trim() + "]");
    }

    /** Creates and enters a default world named {@code lazarus-smoke} with a fixed seed. */
    private static void launchWorld() throws ReflectiveOperationException {
        ClassLoader loader = SmokeMod.class.getClassLoader();
        Class<?> gameType = Class.forName("net.minecraft.world.WorldSettings$GameType", true, loader);
        Object survival = gameType.getField("SURVIVAL").get(null);
        Class<?> worldType = Class.forName("net.minecraft.world.WorldType", true, loader);
        Object defaultType = worldType.getField("field_77137_b").get(null);
        Class<?> settingsClass = Class.forName("net.minecraft.world.WorldSettings", true, loader);
        Object settings = settingsClass
                .getConstructor(long.class, gameType, boolean.class, boolean.class, worldType)
                .newInstance(1891L, survival, true, false, defaultType);
        String folder = "lazarus-smoke-" + System.nanoTime();
        Object mc = minecraft();
        mc.getClass()
                .getMethod("func_71371_a", String.class, String.class, settingsClass)
                .invoke(mc, folder, "lazarus-smoke", settings);
    }

    /** Saves a screenshot to {@code screenshots/} through Minecraft's own helper. */
    private static void screenshot() {
        try {
            Object mc = minecraft();
            Class<?> type = mc.getClass();
            File dir = (File) type.getField("field_71412_D").get(mc);
            int width = type.getField("field_71443_c").getInt(mc);
            int height = type.getField("field_71440_d").getInt(mc);
            Object framebuffer = type.getMethod("func_147110_a").invoke(mc);
            Class<?> helper =
                    Class.forName("net.minecraft.util.ScreenShotHelper", true, SmokeMod.class.getClassLoader());
            for (Method m : helper.getMethods()) {
                if (m.getName().equals("func_148260_a") && m.getParameterTypes().length == 4) {
                    System.out.println("LAZARUS-SMOKE screenshot " + m.invoke(null, dir, width, height, framebuffer));
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            System.out.println("LAZARUS-SMOKE screenshot failed: " + e);
        }
    }

    private static Object minecraft() throws ReflectiveOperationException {
        Class<?> type = Class.forName("net.minecraft.client.Minecraft", true, SmokeMod.class.getClassLoader());
        return type.getMethod("func_71410_x").invoke(null);
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        return owner.getClass().getField(name).get(owner);
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
