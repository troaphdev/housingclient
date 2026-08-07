package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Uses replacement display data for item tooltips and chat item hover text.
 */
@Mixin(GuiScreen.class)
public abstract class MixinItemHiderGuiScreen {

    @ModifyVariable(method = "func_146285_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceTooltipStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
