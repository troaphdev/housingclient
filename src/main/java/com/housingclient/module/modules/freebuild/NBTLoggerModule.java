package com.housingclient.module.modules.freebuild;

import com.housingclient.itemlog.ItemLogManager;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.utils.ChatUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * NBT Logger Module
 * 
 * Analyzes S04PacketEntityEquipment packets for items with "ItemModel" NBT.
 * Logs the player name and item name to chat, and saves items to the Item Log.
 */
public class NBTLoggerModule extends Module {

    private final BooleanSetting logToChat = new BooleanSetting("Log to Chat", "Show logged items in chat", true);
    private final BooleanSetting saveToLog = new BooleanSetting("Save to Item Log", "Save items to Item Log tab", true);
    private final BooleanSetting logAllNBT = new BooleanSetting("Log All NBT",
            "Log any item with NBT, not just ItemModel", false);
    private final BooleanSetting logArmor = new BooleanSetting("Log Armor", "Also log armor with custom tags", true);

    // Track logged items to avoid spam (player entity ID + slot + item hash)
    private final Set<String> recentlyLogged = new HashSet<>();
    private int cleanupCounter = 0;

    public NBTLoggerModule() {
        super("NBT Logger", "Log items with ItemModel NBT from other players", Category.EXPLOIT, ModuleMode.BOTH);

        addSetting(logToChat);
        addSetting(saveToLog);
        addSetting(logAllNBT);
        addSetting(logArmor);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        // Cleanup old entries periodically
        cleanupCounter++;
        if (cleanupCounter > 200) { // Every 10 seconds
            recentlyLogged.clear();
            cleanupCounter = 0;
        }

        // Scan all players for equipment with ItemModel NBT
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

            // Check all equipment slots
            for (int slot = 0; slot < 5; slot++) {
                // Skip armor slots if armor logging is disabled
                if (!logArmor.isEnabled() && slot > 0) {
                    continue;
                }

                ItemStack stack = getEquipmentInSlot(player, slot);
                if (stack == null)
                    continue;

                // Check for NBT
                NBTTagCompound nbt = stack.getTagCompound();
                if (nbt == null)
                    continue;

                // Check for ItemModel or any NBT based on setting
                boolean shouldLog = false;
                if (nbt.hasKey("ItemModel")) {
                    shouldLog = true;
                } else if (logAllNBT.isEnabled() && !nbt.hasNoTags()) {
                    shouldLog = true;
                }

                if (shouldLog) {
                    processItem(player, stack, slot);
                }
            }
        }
    }

    private ItemStack getEquipmentInSlot(EntityPlayer player, int slot) {
        switch (slot) {
            case 0:
                return player.getHeldItem();
            case 1:
                return player.getCurrentArmor(3); // Helmet
            case 2:
                return player.getCurrentArmor(2); // Chestplate
            case 3:
                return player.getCurrentArmor(1); // Leggings
            case 4:
                return player.getCurrentArmor(0); // Boots
            default:
                return null;
        }
    }

    private void processItem(EntityPlayer player, ItemStack stack, int slot) {
        // Create unique key to prevent spam
        String key = player.getEntityId() + ":" + slot + ":" + getItemHash(stack);

        if (recentlyLogged.contains(key)) {
            return; // Already logged this item recently
        }

        recentlyLogged.add(key);

        String playerName = player.getName();
        String itemName = stack.getDisplayName();
        NBTTagCompound nbt = stack.getTagCompound();

        // Log to chat
        if (logToChat.isEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("\u00A7b[NBT Logger] \u00A7f").append(playerName);
            msg.append(" \u00A77has \u00A7e").append(itemName);

            if (nbt != null && nbt.hasKey("ItemModel")) {
                msg.append(" \u00A77(ItemModel: \u00A7a").append(nbt.getString("ItemModel")).append("\u00A77)");
            }

            ChatUtils.sendClientMessage(msg.toString());
        }

        // Save to Item Log
        if (saveToLog.isEnabled()) {
            ItemLogManager manager = ItemLogManager.getInstance();
            if (manager != null) {
                boolean added = manager.addItem(stack);
                if (added && logToChat.isEnabled()) {
                    ChatUtils.sendClientMessage("\u00A7a  >> Saved to Item Log!");
                }
            }
        }
    }

    private String getItemHash(ItemStack stack) {
        if (stack == null)
            return "null";

        StringBuilder hash = new StringBuilder();
        hash.append(stack.getItem().getRegistryName());
        hash.append(":").append(stack.getItemDamage());

        if (stack.hasTagCompound()) {
            hash.append(":").append(stack.getTagCompound().hashCode());
        }

        return hash.toString();
    }

    @Override
    public String getDisplayInfo() {
        ItemLogManager manager = ItemLogManager.getInstance();
        if (manager != null) {
            return String.valueOf(manager.getItemCount());
        }
        return null;
    }
}
