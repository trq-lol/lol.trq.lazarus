/* SPDX-License-Identifier: LGPL-3.0-or-later */
package lol.trq.lazarus.smokemixin.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Proves Mixin applied: prints a report line when the main menu initializes. Targets SRG names, no refmap. */
@Mixin(targets = "net.minecraft.client.gui.GuiMainMenu", remap = false)
public abstract class MainMenuMixin {
    @Inject(method = "func_73866_w_", at = @At("HEAD"), remap = false)
    private void lazarus$initGui(CallbackInfo ci) {
        System.out.println("LAZARUS-MIXIN OK version="
                + org.spongepowered.asm.launch.MixinBootstrap.VERSION
                + " java=" + System.getProperty("java.version"));
    }
}
