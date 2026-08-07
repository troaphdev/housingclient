package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.NickHiderModule;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkPlayerInfo.class)
public class MixinNetworkPlayerInfo {

    @Inject(method = "func_178837_g", at = @At("HEAD"), cancellable = true)
    private void useSteveSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        NetworkPlayerInfo playerInfo = (NetworkPlayerInfo) (Object) this;
        if (NickHiderModule.shouldHideSkin(playerInfo.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementSkin());
        }
    }

    @Inject(method = "func_178861_h", at = @At("HEAD"), cancellable = true)
    private void useReplacementCape(CallbackInfoReturnable<ResourceLocation> cir) {
        NetworkPlayerInfo playerInfo = (NetworkPlayerInfo) (Object) this;
        if (NickHiderModule.shouldHideSkin(playerInfo.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementCape());
        }
    }

    @Inject(method = "func_178851_f", at = @At("HEAD"), cancellable = true)
    private void useSteveModel(CallbackInfoReturnable<String> cir) {
        NetworkPlayerInfo playerInfo = (NetworkPlayerInfo) (Object) this;
        if (NickHiderModule.shouldHideSkin(playerInfo.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementSkinModel());
        }
    }
}
