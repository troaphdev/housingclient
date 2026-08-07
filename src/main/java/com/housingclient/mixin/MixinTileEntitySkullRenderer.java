package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.NickHiderModule;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TileEntitySkullRenderer.class)
public class MixinTileEntitySkullRenderer {

    /**
     * Substitutes only player-head profiles whose embedded skin texture matches
     * the active account's real skin. The ItemStack/NBT remains untouched.
     */
    @ModifyVariable(method = "func_180543_a", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private GameProfile hideMatchingPlayerHead(GameProfile profile) {
        return NickHiderModule.replaceMatchingSkullProfile(profile);
    }
}
