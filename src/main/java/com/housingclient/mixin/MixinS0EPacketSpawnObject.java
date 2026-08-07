package com.housingclient.mixin;

import com.housingclient.utils.ItemFramePacketFilter;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Records the item-frame ID before any following metadata packet is decoded. */
@Mixin(S0EPacketSpawnObject.class)
public abstract class MixinS0EPacketSpawnObject {

    @Inject(method = "func_148837_a", at = @At("RETURN"))
    private void housingclient$recordObjectType(PacketBuffer buffer, CallbackInfo ci) {
        S0EPacketSpawnObject packet = (S0EPacketSpawnObject) (Object) this;
        ItemFramePacketFilter.recordObjectSpawn(packet.getEntityID(), packet.getType());
    }
}
