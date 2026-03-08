package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.visuals.FancyTextModule;
import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Intercepts outgoing chat messages to revert fancy text back to normal
 * when sending commands (messages starting with /).
 */
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {

    @ModifyVariable(method = "func_71165_d", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String revertFancyCommands(String message) {
        try {
            FancyTextModule fancyText = HousingClient.getInstance().getModuleManager()
                    .getModule(FancyTextModule.class);
            if (fancyText != null && fancyText.isEnabled() && fancyText.isRevertCommands()) {
                if (FancyTextModule.isCommand(message)) {
                    return FancyTextModule.revertToNormal(message);
                }
            }
        } catch (Exception e) {
            // Fail silently
        }
        return message;
    }
}
