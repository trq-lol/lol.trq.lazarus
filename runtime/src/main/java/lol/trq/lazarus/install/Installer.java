/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.install;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * Entrypoint of {@code java -jar lol.trq.lazarus.jar}: installs Lazarus into the official Minecraft Launcher.
 *
 * <p>With no arguments and a display, it shows a small dialog. Otherwise it is a command line tool:
 *
 * <pre>
 * java -jar lol.trq.lazarus.jar install [--dir &lt;.minecraft&gt;] [--forge &lt;version id&gt;] [--memory &lt;MB&gt;]
 * java -jar lol.trq.lazarus.jar prism
 * java -jar lol.trq.lazarus.jar version
 * </pre>
 */
public final class Installer {
    static final String PROFILE_NAME = "Forge 1.8.9 (Lazarus)";
    static final String PROFILE_NAME_LWJGL3 = "Forge 1.8.9 (Lazarus, LWJGL 3)";

    private Installer() {}

    /**
     * Runs the installer.
     *
     * @param args the command line
     * @throws Exception if the install fails
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 && !GraphicsEnvironment.isHeadless()) {
            gui();
            return;
        }
        if (args.length == 0 || args[0].equals("help") || args[0].equals("--help")) {
            System.out.println(usage());
            return;
        }
        if (args[0].equals("version")) {
            System.out.println(version());
            return;
        }
        if (args[0].equals("prism")) {
            System.out.println(prismComponent(version()));
            return;
        }
        if (!args[0].equals("install")) {
            System.err.println("unknown command: " + args[0] + "\n" + usage());
            System.exit(2);
        }
        Path dir = VanillaInstall.defaultMinecraftDir();
        String forge = null;
        int memory = 2048;
        boolean lwjgl3 = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--dir" -> dir = Path.of(args[++i]);
                case "--forge" -> forge = args[++i];
                case "--memory" -> memory = Integer.parseInt(args[++i]);
                case "--lwjgl3" -> lwjgl3 = true;
                default -> {
                    System.err.println("unknown option: " + args[i] + "\n" + usage());
                    System.exit(2);
                }
            }
        }
        VanillaInstall install = new VanillaInstall(dir, selfJar(), version());
        if (forge == null) {
            List<String> found = install.forgeVersions();
            if (found.isEmpty()) {
                System.err.println("No Forge 1.8.9 installation in " + dir
                        + ". Install Forge 1.8.9 (11.15.1.2318) with its installer, then run this again.");
                System.exit(1);
            }
            forge = found.get(0);
        }
        String name = lwjgl3 ? PROFILE_NAME_LWJGL3 : PROFILE_NAME;
        System.out.println(install.install(forge, name, memory, lwjgl3).describe());
    }

    private static void gui() throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        String title = "Lazarus " + version();
        JTextField dirField =
                new JTextField(VanillaInstall.defaultMinecraftDir().toString(), 36);
        JComboBox<String> forgeBox = new JComboBox<>();
        refresh(dirField, forgeBox);
        dirField.addActionListener(e -> refresh(dirField, forgeBox));
        JPanel panel = new JPanel(new java.awt.GridLayout(0, 1, 4, 4));
        panel.add(new JLabel("Runs Minecraft Forge 1.8.9 on Java 17 and newer."));
        panel.add(new JLabel("Minecraft directory:"));
        panel.add(dirField);
        panel.add(new JLabel("Forge 1.8.9 installation:"));
        panel.add(forgeBox);
        javax.swing.JCheckBox lwjgl3Box = new javax.swing.JCheckBox(
                "Use LWJGL 3 (needed on Apple silicon Macs)",
                System.getProperty("os.arch", "").contains("aarch64")
                        && System.getProperty("os.name", "").toLowerCase().startsWith("mac"));
        panel.add(lwjgl3Box);
        int choice = JOptionPane.showConfirmDialog(
                null, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
            return;
        }
        refresh(dirField, forgeBox);
        Object forge = forgeBox.getSelectedItem();
        if (forge == null) {
            JOptionPane.showMessageDialog(
                    null,
                    "No Forge 1.8.9 installation found.\nInstall Forge 1.8.9 (11.15.1.2318) with its installer first.",
                    title,
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            VanillaInstall install =
                    new VanillaInstall(Path.of(dirField.getText().trim()), selfJar(), version());
            boolean lwjgl3 = lwjgl3Box.isSelected();
            String result = install.install(forge.toString(), lwjgl3 ? PROFILE_NAME_LWJGL3 : PROFILE_NAME, 2048, lwjgl3)
                    .describe();
            JOptionPane.showMessageDialog(null, result, title, JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Install failed:\n" + e.getMessage(), title, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void refresh(JTextField dirField, JComboBox<String> forgeBox) {
        forgeBox.removeAllItems();
        try {
            for (String id : new VanillaInstall(Path.of(dirField.getText().trim()), null, version()).forgeVersions()) {
                forgeBox.addItem(id);
            }
        } catch (Exception unreadable) {
            forgeBox.removeAllItems();
        }
    }

    static String usage() {
        return """
                Lazarus %s: runs Minecraft Forge 1.8.9 on Java 17 and newer.

                usage:
                  java -jar lol.trq.lazarus.jar                 open the installer window
                  java -jar lol.trq.lazarus.jar install [options]
                      --dir <path>      the launcher's game directory (default: %s)
                      --forge <id>      the Forge version to build on (default: newest 1.8.9 Forge found)
                      --memory <MB>     maximum heap for the profile (default: 2048)
                      --lwjgl3          run on LWJGL 3 (needed on Apple silicon); a separate profile
                  java -jar lol.trq.lazarus.jar prism         print a Prism Launcher / MultiMC component
                  java -jar lol.trq.lazarus.jar version
                """.formatted(version(), VanillaInstall.defaultMinecraftDir());
    }

    static String version() {
        String v = Installer.class.getPackage().getImplementationVersion();
        return v == null ? "dev" : v;
    }

    static Path selfJar() {
        try {
            return new File(Installer.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Builds a Prism Launcher / MultiMC component that adds Lazarus to a Forge 1.8.9 instance. The jar is looked up
     * in the instance's {@code libraries} folder.
     *
     * @param version the Lazarus version
     * @return the component JSON
     */
    static String prismComponent(String version) {
        return """
                {
                  "formatVersion": 1,
                  "name": "Lazarus",
                  "uid": "lol.trq.lazarus",
                  "version": "%1$s",
                  "mainClass": "lol.trq.lazarus.launch.Lazarus",
                  "libraries": [
                    {"name": "lol.trq.lazarus:lol.trq.lazarus:%1$s", "MMC-hint": "local"}
                  ],
                  "requires": [{"uid": "net.minecraftforge"}]
                }""".formatted(version);
    }
}
