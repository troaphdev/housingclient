package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.movement.FlyModule;
import com.housingclient.module.modules.combat.NoDebuffModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import com.housingclient.module.modules.visuals.TPSModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "func_147270_a", at = @At("HEAD"), cancellable = true)
    public void handlePlayerAbilities(S39PacketPlayerAbilities packet, CallbackInfo ci) {
        NoDebuffModule noDebuff = (NoDebuffModule) HousingClient.getInstance().getModuleManager()
                .getModule(NoDebuffModule.class);
        if (noDebuff != null && noDebuff.isEnabled() && noDebuff.isSlownessRemovalEnabled()) {
            // If the server tries to set walkSpeed lower than default, prevent it
            if (packet.getWalkSpeed() < 0.1f) {
                packet.setWalkSpeed(0.1f);
            }
        }

        FlyModule fly = (FlyModule) HousingClient.getInstance().getModuleManager().getModule(FlyModule.class);
        if (fly != null && fly.isEnabled()) {
            // Always capture the server's fly state for safe mode tracking
            fly.updateServerFlyState(packet.isAllowFlying());

            // Only override abilities in blatant mode OR creative mode
            com.housingclient.module.modules.client.ClickGUIModule clickGUI = HousingClient.getInstance()
                    .getModuleManager().getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
            boolean blatant = clickGUI != null && clickGUI.isBlatantModeEnabled();

            if (blatant || (Minecraft.getMinecraft().thePlayer != null
                    && Minecraft.getMinecraft().thePlayer.capabilities.isCreativeMode)) {
                packet.setAllowFlying(true);
                if (Minecraft.getMinecraft().thePlayer != null
                        && Minecraft.getMinecraft().thePlayer.capabilities.isFlying) {
                    packet.setFlying(true);
                }
            } else {
                // Safe mode + survival: let the packet through naturally
                // The fly state was already captured above via updateServerFlyState
                // If the server allows flying, preserve it
                if (packet.isAllowFlying()) {
                    // Server grants flight — keep it
                } else {
                    // Server revokes flight — respect it
                }
            }
        }
    }

    @Inject(method = "func_147260_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityEffect(S1DPacketEntityEffect packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null
                && packet.getEntityId() == Minecraft.getMinecraft().thePlayer.getEntityId()) {
            NoDebuffModule noDebuff = HousingClient.getInstance()
                    .getModuleManager().getModule(NoDebuffModule.class);
            if (noDebuff != null && noDebuff.isEnabled() && noDebuff.shouldRemoveEffect(packet.getEffectId())) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "func_147290_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityProperties(S20PacketEntityProperties packet, CallbackInfo ci) {
        if (Minecraft.getMinecraft().thePlayer != null
                && packet.getEntityId() == Minecraft.getMinecraft().thePlayer.getEntityId()) {
            NoDebuffModule noDebuff = HousingClient.getInstance()
                    .getModuleManager().getModule(NoDebuffModule.class);
            if (noDebuff != null && noDebuff.isEnabled() && noDebuff.isSlownessRemovalEnabled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "func_147285_a", at = @At("HEAD"))
    public void handleTimeUpdate(S03PacketTimeUpdate packet, CallbackInfo ci) {
        TPSModule tpsModule = HousingClient.getInstance().getModuleManager().getModule(TPSModule.class);
        if (tpsModule != null) {
            tpsModule.onTimeUpdate();
        }
    }

    @Inject(method = "func_147236_a", at = @At("HEAD"), cancellable = true)
    public void handleEntityStatus(S19PacketEntityStatus packet, CallbackInfo ci) {
        if (packet.getOpCode() == 17) { // Firework explosion opcode
            net.minecraft.entity.Entity entity = packet.getEntity(Minecraft.getMinecraft().theWorld);
            if (entity instanceof net.minecraft.entity.item.EntityFireworkRocket) {
                com.housingclient.module.modules.visuals.HideEntitiesModule hideEntities = (com.housingclient.module.modules.visuals.HideEntitiesModule) HousingClient
                        .getInstance().getModuleManager()
                        .getModule(com.housingclient.module.modules.visuals.HideEntitiesModule.class);

                if (hideEntities != null && hideEntities.isEnabled() && hideEntities.shouldHideEntity(entity)) {
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "func_147234_a", at = @At("HEAD"))
    public void handleBlockChange(net.minecraft.network.play.server.S23PacketBlockChange packet, CallbackInfo ci) {
        com.housingclient.module.modules.moderation.GrieferDetectorModule detector = (com.housingclient.module.modules.moderation.GrieferDetectorModule) HousingClient
                .getInstance().getModuleManager()
                .getModule(com.housingclient.module.modules.moderation.GrieferDetectorModule.class);
        if (detector != null && detector.isEnabled()) {
            detector.onPacketReceive(packet);
        }
    }

    @Inject(method = "func_147287_a", at = @At("HEAD"))
    public void handleMultiBlockChange(net.minecraft.network.play.server.S22PacketMultiBlockChange packet,
            CallbackInfo ci) {
        com.housingclient.module.modules.moderation.GrieferDetectorModule detector = (com.housingclient.module.modules.moderation.GrieferDetectorModule) HousingClient
                .getInstance().getModuleManager()
                .getModule(com.housingclient.module.modules.moderation.GrieferDetectorModule.class);
        if (detector != null && detector.isEnabled()) {
            detector.onPacketReceive(packet);
        }
    }
}
