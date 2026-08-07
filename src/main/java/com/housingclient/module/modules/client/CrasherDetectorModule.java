package com.housingclient.module.modules.client;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class CrasherDetectorModule extends Module {

    private final Map<String, Long> alertCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    /* ==================================================================
     *  Encrypted detection signatures — do not modify
     * ================================================================== */
    private static final int[][] _0xA4E1 = {
        {89, 84, 78, 77, 81, 92, 68},                                                              // 0: decoy
        {80, 84, 83, 88, 94, 79, 92, 91, 73, 7, 74, 79, 84, 73, 73, 88, 83, 98, 95, 82, 82, 86},  // 1: real (item id)
        {110, 86, 72, 81, 81, 114, 74, 83, 88, 79},                                                // 2: decoy
        {92, 72, 73, 85, 82, 79},                                                                   // 3: decoy
        {105, 79, 82, 92, 77, 85},                                                                  // 4: decoy
        {116, 73, 88, 80, 112, 82, 89, 88, 81},                                                    // 5: decoy
        {77, 92, 90, 88, 78},                                                                       // 6: real (pages key)
        {127, 81, 82, 94, 86, 120, 83, 73, 84, 73, 68, 105, 92, 90},                              // 7: decoy
        {102, 96},                                                                                   // 8: real (payload: just [])
        {94, 82, 80, 77, 82, 83, 88, 83, 73, 78},                                                  // 9: decoy
        {80, 82, 89, 88, 81, 84, 73, 88, 80, 78},                                                  // 10: decoy
        {84, 83, 73, 88, 79, 83, 92, 81},                                                          // 11: decoy
    };

    private static final int[] _0xB7C4 = {5, 1, 7, 3, 9, 4, 0, 6, 2, 8, 11, 10};

    private static String _0xD2(int idx) {
        int[] raw = _0xA4E1[_0xB7C4[idx & 0xF]];
        long v = 0x09L;
        v = (v * 0x07L - 0x02L);
        int mask = (int) (v & 0xFFL);
        char[] buf = new char[raw.length];
        for (int i = 0; i < raw.length; i++) buf[i] = (char) ((raw[i] ^ mask) & 0xFF);
        return new String(buf);
    }
    /* ================================================================== */

    public CrasherDetectorModule() {
        super("Crash Detector", "Alerts when players hold crash items", Category.VISUALS,
                ModuleMode.BOTH);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (!isEnabled())
            return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getUniqueID().equals(mc.thePlayer.getUniqueID()))
                continue;

            for (int i = 0; i < 5; i++) {
                ItemStack stack = player.getEquipmentInSlot(i);
                if (stack == null || !stack.hasTagCompound())
                    continue;

                if (classifyThreat(stack)) {
                    tryAlert(player.getName(), stack.getDisplayName());
                }
            }
        }
    }

    /**
     * Detects any written book where any page contains the [] crash marker.
     * The previous {} marker remains covered for backwards compatibility.
     */
    private boolean classifyThreat(ItemStack stack) {
        if (stack == null) return false;
        if (stack.getItem() == null) return false;
        if (!stack.hasTagCompound()) return false;

        // ---- Phase 1: Must be a written book ----
        Item reference = Item.getByNameOrId(_0xD2(1));
        if (reference == null) return false;
        if (stack.getItem() != reference) return false;

        NBTTagCompound compound = stack.getTagCompound();

        // ---- Phase 2: Must have pages tag list ----
        String payloadKey = _0xD2(7);
        if (!compound.hasKey(payloadKey, 9)) return false;

        NBTTagList payload = compound.getTagList(payloadKey, 8);
        if (payload == null || payload.tagCount() < 1) return false;

        // ---- Phase 3: Any page CONTAINS the crash marker ----
        String marker = _0xD2(9);
        String legacyMarker = "{}";

        for (int i = 0; i < payload.tagCount(); i++) {
            String entry = payload.getStringTagAt(i);
            if (entry != null && (entry.contains(marker) || entry.contains(legacyMarker))) {
                return true;
            }
        }

        return false;
    }

    private void tryAlert(String playerName, String itemName) {
        long now = System.currentTimeMillis();
        if (alertCooldowns.containsKey(playerName)) {
            long lastAlert = alertCooldowns.get(playerName);
            if (now - lastAlert < COOLDOWN_MS) {
                return;
            }
        }

        ChatUtils.sendClientMessage(
                "\u00A7c[Crash Detector] \u00A7e" + playerName + " \u00A7fis holding \u00A7c" + itemName + "\u00A7f!");
        alertCooldowns.put(playerName, now);
    }
}
