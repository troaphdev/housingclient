package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.visuals.HideEntitiesModule;
import com.housingclient.module.modules.visuals.ItemDisguiserModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.RenderItemFrame;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides a frame-only rendering path that never reads the displayed ItemStack.
 * This keeps unsafe stacks out of model, map, compass, name, and item render code.
 */
@Mixin(RenderItemFrame.class)
public abstract class MixinRenderItemFrame {

    private static final ModelResourceLocation HOUSINGCLIENT_EMPTY_FRAME_MODEL =
            new ModelResourceLocation("item_frame", "normal");

    @Inject(method = "func_76986_a", at = @At("HEAD"), cancellable = true)
    private void housingclient$renderSafeFrame(EntityItemFrame entity, double x, double y, double z,
            float entityYaw, float partialTicks, CallbackInfo ci) {
        HideEntitiesModule hideEntities = housingclient$getHideEntities();
        if (hideEntities == null || !hideEntities.isEnabled()
                || !hideEntities.shouldHideItemFrameContents()) {
            return;
        }

        /* Hide Item Frames suppresses both the frame and its contents immediately. */
        if (hideEntities.isHideItemFramesEnabled()) {
            ci.cancel();
            return;
        }

        /*
         * Draw only the empty frame. Do not call getDisplayedItem anywhere in this
         * path: even inspecting an unsafe stack can enter custom model code.
         */
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        BlockPos blockPos = entity.getHangingPosition();
        double renderX = (double) blockPos.getX() - entity.posX + x;
        double renderY = (double) blockPos.getY() - entity.posY + y;
        double renderZ = (double) blockPos.getZ() - entity.posZ + z;
        GlStateManager.translate(renderX + 0.5D, renderY + 0.5D, renderZ + 0.5D);
        GlStateManager.rotate(180.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

        BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
        ModelManager modelManager = dispatcher.getBlockModelShapes().getModelManager();
        IBakedModel frameModel = modelManager.getModel(HOUSINGCLIENT_EMPTY_FRAME_MODEL);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        dispatcher.getBlockModelRenderer().renderModelBrightnessColor(
                frameModel, 1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
        ci.cancel();
    }

    /** Preserve Item Disguiser behavior whenever frame contents are visible. */
    @ModifyVariable(method = "func_82402_b", at = @At("STORE"), ordinal = 0)
    private ItemStack housingclient$getVisualFramedItem(ItemStack stack) {
        HideEntitiesModule hideEntities = housingclient$getHideEntities();
        if (hideEntities != null && hideEntities.isEnabled()
                && hideEntities.shouldHideItemFrameContents()) {
            return null;
        }
        return ItemDisguiserModule.getVisualStack(stack);
    }

    private static HideEntitiesModule housingclient$getHideEntities() {
        if (HousingClient.getInstance() == null || HousingClient.getInstance().getModuleManager() == null) {
            return null;
        }
        return HousingClient.getInstance().getModuleManager().getModule(HideEntitiesModule.class);
    }
}
