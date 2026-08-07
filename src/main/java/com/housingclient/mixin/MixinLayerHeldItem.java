package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Replaces the stack before the third-person held-item layer chooses
 * block-specific translations and scaling (notably chests).
 */
@Mixin(LayerHeldItem.class)
public abstract class MixinLayerHeldItem {

    @ModifyVariable(method = "func_177141_a", at = @At("STORE"), ordinal = 0)
    private ItemStack housingclient$useVisualHeldItemTransform(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
