package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.NickHiderModule;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiUtilRenderComponents.class)
public class MixinGuiUtilRenderComponents {

    /**
     * Replaces the name before chat/sign line wrapping so a username split across
     * two rendered lines cannot bypass the final FontRenderer hook.
     */
    @ModifyVariable(method = "func_178908_a", at = @At(value = "STORE"), index = 11)
    private static String hideAccountNameBeforeWrapping(String text) {
        return NickHiderModule.replaceOwnName(text);
    }
}
