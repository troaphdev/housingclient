package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import com.housingclient.module.modules.visuals.NickHiderModule;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    /**
     * Covers every vanilla text draw after layout has been calculated, including
     * chat, scoreboards, tab lists, nametags, menus, and the debug overlay.
     */
    @ModifyVariable(method = "func_180455_b", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String hideAccountName(String text) {
        return NickHiderModule.replaceOwnName(ItemDisguiserModule.replaceHeldItemName(text));
    }

    /**
     * Uses the replacement name for layout measurements as well as drawing so
     * nametag backgrounds and other dynamically sized UI match the hidden name.
     */
    @ModifyVariable(method = "func_78256_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String measureHiddenAccountName(String text) {
        return NickHiderModule.replaceOwnName(ItemDisguiserModule.replaceHeldItemName(text));
    }
}
