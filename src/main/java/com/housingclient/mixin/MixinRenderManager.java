package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.visuals.HideEntitiesModule;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {

    @Inject(method = "func_147939_a", at = @At("HEAD"), cancellable = true)
    private void onDoRenderEntity(Entity entity, double x, double y, double z, float entityYaw,
            float partialTicks, boolean hideDebugBox, CallbackInfoReturnable<Boolean> cir) {
        if (HousingClient.getInstance() == null || HousingClient.getInstance().getModuleManager() == null)
            return;
        HideEntitiesModule hideEntities = HousingClient.getInstance().getModuleManager()
                .getModule(HideEntitiesModule.class);
        if (hideEntities != null && hideEntities.isEnabled() && hideEntities.isEntityHidden(entity.getEntityId())) {
            cir.setReturnValue(false);
        }
    }
}
