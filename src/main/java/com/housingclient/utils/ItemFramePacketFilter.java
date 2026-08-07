package com.housingclient.utils;

import com.housingclient.module.modules.visuals.HideEntitiesModule;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks item-frame entity IDs directly from object-spawn packets.
 *
 * This registry is available on the Netty decoder thread, before the matching
 * entity metadata packet is allowed to construct ItemStack or NBT objects.
 */
public final class ItemFramePacketFilter {

    private static final int ITEM_FRAME_OBJECT_TYPE = 71;
    private static final Set<Integer> ITEM_FRAME_IDS = Collections.newSetFromMap(
            new ConcurrentHashMap<Integer, Boolean>());

    private ItemFramePacketFilter() {
    }

    public static void recordObjectSpawn(int entityId, int objectType) {
        if (objectType == ITEM_FRAME_OBJECT_TYPE) {
            ITEM_FRAME_IDS.add(entityId);
        } else {
            ITEM_FRAME_IDS.remove(entityId);
        }
    }

    public static void recordItemFrame(int entityId) {
        ITEM_FRAME_IDS.add(entityId);
    }

    public static boolean shouldDiscardMetadata(int entityId) {
        return HideEntitiesModule.shouldDiscardItemFrameContentsAtNetworkBoundary()
                && ITEM_FRAME_IDS.contains(entityId);
    }

    public static void remove(int entityId) {
        ITEM_FRAME_IDS.remove(entityId);
    }

    public static void clear() {
        ITEM_FRAME_IDS.clear();
    }
}
