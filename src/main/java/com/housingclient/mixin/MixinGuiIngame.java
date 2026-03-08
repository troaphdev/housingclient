package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.modules.visuals.ScoreboardModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for GuiIngame to allow the Scoreboard module to cancel vanilla
 * rendering.
 * 
 * When the Scoreboard module is enabled, we cancel vanilla scoreboard rendering
 * so the module can render its own custom scoreboard at the HudDesigner
 * position.
 */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame {

    /**
     * Inject at the start of renderScoreboard to cancel it when Scoreboard module
     * is enabled.
     * 
     * Uses SRG name func_180475_a for production compatibility.
     */
    @Inject(method = "func_180475_a", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(ScoreObjective objective, ScaledResolution sr, CallbackInfo ci) {
        // Check if Scoreboard module is enabled
        if (HousingClient.getInstance() == null ||
                HousingClient.getInstance().getModuleManager() == null) {
            return;
        }

        ScoreboardModule scoreboard = HousingClient.getInstance()
                .getModuleManager()
                .getModule(ScoreboardModule.class);

        if (scoreboard != null && scoreboard.isEnabled()) {
            // Cancel vanilla rendering - the module renders its own
            ci.cancel();
        }
    }
}
