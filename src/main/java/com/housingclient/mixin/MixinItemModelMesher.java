package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Selects the replacement model for every standard item rendering path,
 * including GUI slots and dropped item entities.
 */
@Mixin(ItemModelMesher.class)
public abstract class MixinItemModelMesher {

    @ModifyVariable(method = "func_178089_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private ItemStack housingclient$replaceItemModelStack(ItemStack stack) {
        return ItemDisguiserModule.getVisualStack(stack);
    }
}
