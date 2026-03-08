package com.housingclient.mixin;

import com.housingclient.module.modules.combat.ReachModule;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    /**
     * @author HousingClient
     * @reason Redirect getBlockReachDistance to allow extended reach.
     *         Target: PlayerControllerMP.getBlockReachDistance() called in
     *         EntityRenderer.getMouseOver()
     */
    @Redirect(method = "func_78473_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;func_78757_d()F"))
    private float redirectGetBlockReachDistance(PlayerControllerMP controller) {
        if (com.housingclient.HousingClient.getInstance() != null
                && com.housingclient.HousingClient.getInstance().getModuleManager() != null) {
            ReachModule reach = (ReachModule) com.housingclient.HousingClient.getInstance().getModuleManager()
                    .getModule(ReachModule.class);
            if (reach != null && reach.isEnabled()) {
                // Return Max of L/R because we limit L separately in MixinPlayerControllerMP
                return (float) Math.max(reach.getLeftReach(), reach.getRightReach());
            }
        }
        return controller.getBlockReachDistance();
    }
}
