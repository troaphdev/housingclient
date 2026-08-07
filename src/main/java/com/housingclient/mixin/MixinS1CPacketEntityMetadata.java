package com.housingclient.mixin;

import com.housingclient.utils.ItemFramePacketFilter;
import net.minecraft.entity.DataWatcher;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Drops an item frame's complete metadata payload at the decoder boundary.
 * No ItemStack, NBT compound, profile, model, or texture reference is created.
 */
@Mixin(S1CPacketEntityMetadata.class)
public abstract class MixinS1CPacketEntityMetadata {

    @Redirect(method = "func_148837_a", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/DataWatcher;func_151508_b(Lnet/minecraft/network/PacketBuffer;)Ljava/util/List;"))
    private List<DataWatcher.WatchableObject> housingclient$discardHiddenFrameMetadata(PacketBuffer buffer)
            throws IOException {
        int entityId = ((S1CPacketEntityMetadata) (Object) this).getEntityId();
        if (ItemFramePacketFilter.shouldDiscardMetadata(entityId)) {
            // Entity metadata is the final field in S1C. Advancing to the end of
            // this packet avoids invoking readItemStackFromBuffer entirely.
            buffer.skipBytes(buffer.readableBytes());
            return Collections.emptyList();
        }
        return DataWatcher.readWatchedListFromPacketBuffer(buffer);
    }
}
