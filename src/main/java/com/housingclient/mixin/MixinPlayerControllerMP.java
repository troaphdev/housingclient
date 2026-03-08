package com.housingclient.mixin;

import com.housingclient.module.modules.combat.ReachModule;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "func_78764_a", at = @At("HEAD"), cancellable = true)
    private void onAttackEntity(EntityPlayer playerIn, Entity targetEntity, CallbackInfo ci) {
        if (com.housingclient.HousingClient.getInstance() != null
                && com.housingclient.HousingClient.getInstance().getModuleManager() != null) {
            ReachModule reach = (ReachModule) com.housingclient.HousingClient.getInstance().getModuleManager()
                    .getModule(ReachModule.class);
            if (reach != null && reach.isEnabled()) {
                if (!reach.isInAttackReach(targetEntity)) {
                    ci.cancel();
                }
            }
        }
    }
}
