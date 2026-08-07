package com.housingclient.mixin;

import com.housingclient.module.modules.visuals.NickHiderModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class MixinAbstractClientPlayer {

    @Inject(method = "func_110306_p", at = @At("HEAD"), cancellable = true)
    private void useSteveSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (NickHiderModule.shouldHideSkin(player.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementSkin());
        }
    }

    @Inject(method = "func_110303_q", at = @At("HEAD"), cancellable = true)
    private void useReplacementCape(CallbackInfoReturnable<ResourceLocation> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (NickHiderModule.shouldHideSkin(player.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementCape());
        }
    }

    @Inject(method = "func_175154_l", at = @At("HEAD"), cancellable = true)
    private void useSteveModel(CallbackInfoReturnable<String> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (NickHiderModule.shouldHideSkin(player.getGameProfile())) {
            cir.setReturnValue(NickHiderModule.getReplacementSkinModel());
        }
    }
}
