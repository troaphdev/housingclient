package com.housingclient.mixin;

import com.housingclient.utils.HousingClientUserManager;
import com.housingclient.utils.MinecraftFormatting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(GuiPlayerTabOverlay.class)
public class MixinGuiPlayerTabOverlay {

    @Inject(method = "func_175243_a", at = @At("RETURN"), cancellable = true)
    public void onGetPlayerName(NetworkPlayerInfo networkPlayerInfoIn, CallbackInfoReturnable<String> cir) {
        if (networkPlayerInfoIn == null || networkPlayerInfoIn.getGameProfile() == null) return;
        String name = networkPlayerInfoIn.getGameProfile().getName();
        try {
            if (com.housingclient.HousingClient.instance != null && com.housingclient.HousingClient.instance.getModuleManager() != null) {
                com.housingclient.module.modules.moderation.NickDetectorModule nickMod = 
                    (com.housingclient.module.modules.moderation.NickDetectorModule) com.housingclient.HousingClient.instance.getModuleManager().getModule(com.housingclient.module.modules.moderation.NickDetectorModule.class);
                if (nickMod != null && nickMod.isEnabled() && nickMod.isNicked(name)) {
                    cir.setReturnValue(MinecraftFormatting.appendPreservingFormatting(
                            cir.getReturnValue(), "\u00A7c~"));
                }
            }
        } catch (Exception e) {}
    }

    @Redirect(method = "func_175249_a", at = @At(value = "INVOKE", target = "Ljava/util/List;subList(II)Ljava/util/List;", remap = false))
    public List<NetworkPlayerInfo> onSublist(List<NetworkPlayerInfo> instance, int fromIndex, int toIndex) {
        // Don't truncate; optionally pin only the local profile to the top.
        return com.housingclient.module.modules.visuals.NickHiderModule.reorderTabList(instance);
    }

    @Redirect(method = "func_175249_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;func_175063_a(Ljava/lang/String;FFI)I"))
    public int onDrawStringWithShadow(FontRenderer instance, String text, float x, float y, int color) {
        // Check if the text being drawn (player name) belongs to a HC user
        if (HousingClientUserManager.getInstance().isRainbowName(text)) {
            // Draw rainbow string instead
            return RenderUtils.drawRainbowString(text, x, y, true);
        }

        // Default rendering
        return instance.drawStringWithShadow(text, x, y, color);
    }
}
