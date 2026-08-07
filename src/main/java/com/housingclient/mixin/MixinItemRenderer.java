package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes held-item transforms and special first-person rendering use the visual
 * stack while keeping ItemRenderer's real cached stack intact between frames.
 */
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow(aliases = "field_78453_b")
    private ItemStack itemToRender;

    @Unique
    private ItemStack housingclient$realFirstPersonStack;

    @Unique
    private boolean housingclient$firstPersonStackReplaced;

    @Inject(method = "func_78440_a", at = @At("HEAD"))
    private void housingclient$useVisualFirstPersonStack(float partialTicks, CallbackInfo ci) {
        ItemStack visual = ItemDisguiserModule.getVisualStack(itemToRender);
        if (visual != itemToRender) {
            housingclient$realFirstPersonStack = itemToRender;
            housingclient$firstPersonStackReplaced = true;
            itemToRender = visual;
        }
    }

    @Inject(method = "func_78440_a", at = @At("RETURN"))
    private void housingclient$restoreRealFirstPersonStack(float partialTicks, CallbackInfo ci) {
        if (housingclient$firstPersonStackReplaced) {
            itemToRender = housingclient$realFirstPersonStack;
            housingclient$realFirstPersonStack = null;
            housingclient$firstPersonStackReplaced = false;
        }
    }

    @ModifyVariable(method = "func_178099_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceHeldItemStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
