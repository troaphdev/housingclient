package com.housingclient.event;

import com.housingclient.HousingClient;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EventManager {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (HousingClient.getMinecraft().thePlayer == null)
            return;
        if (HousingClient.getMinecraft().theWorld == null)
            return;

        HousingClient.getInstance().getModuleManager().onTick();
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (HousingClient.getMinecraft().thePlayer == null)
            return;
        if (HousingClient.getMinecraft().theWorld == null)
            return;

        HousingClient.getInstance().getModuleManager().onRender3D(event.partialTicks);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.TEXT)
            return;
        if (HousingClient.getMinecraft().thePlayer == null)
            return;

        HousingClient.getInstance().getModuleManager().onRender();
    }

    @SubscribeEvent
    public void onCameraSetup(net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup event) {
        com.housingclient.module.modules.visuals.FreeCamModule freeCam = HousingClient.getInstance().getModuleManager()
                .getModule(com.housingclient.module.modules.visuals.FreeCamModule.class);

        if (freeCam != null && freeCam.isEnabled()) {
            // The camera will render from the FreeCam's stored position
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent event) {
        if (event.entity.worldObj.isRemote && event.entity.equals(HousingClient.getMinecraft().thePlayer)) {
            if (event.entity instanceof net.minecraft.entity.EntityLivingBase) {
                HousingClient.getInstance().getModuleManager()
                        .onUpdate((net.minecraft.entity.EntityLivingBase) event.entity);
            }
        }
    }

    @SubscribeEvent
    public void onLivingJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.entity.worldObj.isRemote && event.entity.equals(HousingClient.getMinecraft().thePlayer)) {
            HousingClient.getInstance().getModuleManager().onJump(event);
        }
    }
}
