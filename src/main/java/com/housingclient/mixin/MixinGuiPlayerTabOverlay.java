package com.housingclient.mixin;

import com.housingclient.utils.HousingClientUserManager;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Redirect(method = "func_175249_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;func_175063_a(Ljava/lang/String;FFI)I"))
    public int onDrawStringWithShadow(FontRenderer instance, String text, float x, float y, int color) {
        // Check if the text being drawn (player name) belongs to a HC user
        if (HousingClientUserManager.getInstance().isRainbowName(text)) {
            // Draw rainbow string instead
            return RenderUtils.drawRainbowString(text, x, y, true);
        }

        // Default rendering
        return instance.drawStringWithShadow(text, x, y, color);
    }
}
