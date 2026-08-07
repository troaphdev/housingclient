package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * GuiContainerCreative overrides GuiScreen's tooltip method for the Search
 * Items tab, so it needs its own visual-stack substitution.
 */
@Mixin(GuiContainerCreative.class)
public abstract class MixinGuiContainerCreative {

    @ModifyVariable(method = "func_146285_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceCreativeSearchTooltipStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
