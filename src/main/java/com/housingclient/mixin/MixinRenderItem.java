package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Supplies replacement stack data for tinting, enchantment glint, durability
 * overlays, stack labels, and entity-specific item models.
 */
@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @ModifyVariable(method = "func_180454_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceRenderedStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }

    @ModifyVariable(method = "func_180453_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceOverlayStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }

    @ModifyVariable(method = "func_175049_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceEntityModelStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
