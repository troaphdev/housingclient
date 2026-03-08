package com.housingclient.imagetonbt;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;

public class ItemGenerator {

    /**
     * Creates an item with the given name, type, and image lore lines.
     * 
     * @param player    The player to give the item to
     * @param itemName  The custom display name for the item
     * @param itemType  The item type ID (e.g. "paper", "fishing_rod", "diamond")
     * @param loreLines The lore lines (image data)
     * @return true if item was added successfully
     */
    public static boolean giveImageItem(EntityPlayer player, String itemName, String itemType, String[] loreLines) {
        if (player == null || loreLines == null || loreLines.length == 0) {
            System.out.println("[ImageToNBT] giveImageItem failed: player=" + player + ", loreLines="
                    + (loreLines == null ? "null" : loreLines.length));
            return false;
        }

        // Get the item from registry
        Item item = Item.itemRegistry.getObject(new ResourceLocation("minecraft", itemType));
        if (item == null) {
            // Try without namespace
            item = Item.itemRegistry.getObject(new ResourceLocation(itemType));
        }
        if (item == null) {
            System.out.println("[ImageToNBT] Unknown item type: " + itemType + ", falling back to paper");
            item = Item.itemRegistry.getObject(new ResourceLocation("minecraft", "paper"));
        }
        if (item == null) {
            System.out.println("[ImageToNBT] Failed to get any item!");
            return false;
        }

        ItemStack itemStack = new ItemStack(item);

        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();

        // Use the provided item name
        display.setString("Name", "\u00A7f" + itemName);

        NBTTagList loreList = new NBTTagList();
        for (String line : loreLines) {
            if (line != null && !line.isEmpty()) {
                loreList.appendTag(new NBTTagString(line));
            }
        }

        System.out.println("[ImageToNBT] Adding " + loreList.tagCount() + " lore lines to '" + itemName + "' (type: "
                + itemType + ")");
        display.setTag("Lore", loreList);

        nbt.setTag("display", display);
        nbt.setInteger("HideFlags", 63);

        itemStack.setTagCompound(nbt);

        boolean success = player.inventory.addItemStackToInventory(itemStack);
        System.out.println("[ImageToNBT] Item added: " + success);
        return success;
    }
}
