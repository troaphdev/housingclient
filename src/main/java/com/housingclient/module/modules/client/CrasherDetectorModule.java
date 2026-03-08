package com.housingclient.module.modules.client;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.Map;

public class CrasherDetectorModule extends Module {

    private final Map<String, Long> alertCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

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
                continue; // Don't flag self

            // Scan all equipment slots
            for (int i = 0; i < 5; i++) {
                ItemStack stack = player.getEquipmentInSlot(i);
                if (stack == null || !stack.hasTagCompound())
                    continue;

                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt.hasKey("ItemModel")) {
                    tryAlert(player.getName(), stack.getDisplayName());
                }
            }
        }
    }

    private void tryAlert(String playerName, String itemName) {
        long now = System.currentTimeMillis();
        if (alertCooldowns.containsKey(playerName)) {
            long lastAlert = alertCooldowns.get(playerName);
            if (now - lastAlert < COOLDOWN_MS) {
                return; // Cooldown active
            }
        }

        ChatUtils.sendClientMessage(
                "\u00A7c[CrasherDetector] \u00A7e" + playerName + " \u00A7fis holding \u00A7c" + itemName + "\u00A7f!");
        alertCooldowns.put(playerName, now);
    }
}
