package com.housingclient.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public class MixinGuiMultiplayer extends GuiScreen {

    @Inject(method = "func_73866_w_", at = @At("RETURN"))
    public void initGui(CallbackInfo ci) {
        // Add button to bottom left
        // Width 100, Height 20. Position: x=5, y=height-25 (just above bottom)
        this.buttonList.add(new GuiButton(69420, 5, this.height - 25, 100, 20, "Cookie Login"));
    }

    @Inject(method = "func_146284_a", at = @At("HEAD"))
    public void actionPerformed(GuiButton button, CallbackInfo ci) {
        if (button.id == 69420) {
            mc.displayGuiScreen(new com.housingclient.gui.alt.GuiAltManager(this));
        }
    }
}
