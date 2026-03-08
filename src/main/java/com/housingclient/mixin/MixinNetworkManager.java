package com.housingclient.mixin;

import com.housingclient.HousingClient;

import com.housingclient.module.modules.exploit.BlinkModule;
import com.housingclient.module.modules.exploit.PacketMultiplierModule;
import com.housingclient.module.modules.visuals.FreeCamModule;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    /**
     * Intercepts OUTGOING packets.
     * SRG: func_179290_a (sendPacket)
     */
    @Inject(method = "func_179290_a", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {

        // Blink
        try {
            BlinkModule blink = HousingClient.getInstance().getModuleManager().getModule(BlinkModule.class);
            if (blink != null && BlinkModule.shouldCancelPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // System.err.println("[HousingClient] Blink Mixin Error: " + t.getMessage());
        }

        // Packet Multiplier - multiply GUI click packets
        try {
            if (PacketMultiplierModule.shouldMultiplyPacket(packet)) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // ignore
        }

        // FreeCam - suppress sneak packets so server doesn't see camera movement sneaking
        try {
            if (packet instanceof C0BPacketEntityAction) {
                FreeCamModule freeCam = HousingClient.getInstance().getModuleManager().getModule(FreeCamModule.class);
                if (freeCam != null && freeCam.isEnabled()) {
                    C0BPacketEntityAction actionPacket = (C0BPacketEntityAction) packet;
                    C0BPacketEntityAction.Action action = actionPacket.getAction();
                    if (action == C0BPacketEntityAction.Action.START_SNEAKING
                            || action == C0BPacketEntityAction.Action.STOP_SNEAKING) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
            // ignore
        }
    }
}
