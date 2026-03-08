package com.housingclient.itemlog;

import com.housingclient.HousingClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Item Log - a collection of logged items with NBT data
 * that can be retrieved from a custom creative tab.
 */
public class ItemLogManager {

    private static ItemLogManager instance;
    private final List<ItemStack> loggedItems = new ArrayList<>();
    private final File saveFile;

    public ItemLogManager() {
        instance = this;
        saveFile = new File(HousingClient.getInstance().getDataDir(), "item_log.dat");
        loadItems();
    }

    public static ItemLogManager getInstance() {
        return instance;
    }

    /**
     * Add an item to the log. Prevents duplicates based on NBT hash.
     * 
     * @param stack The item to add
     * @return true if added, false if duplicate
     */
    public boolean addItem(ItemStack stack) {
        if (stack == null)
            return false;

        // Check for duplicates
        for (ItemStack existing : loggedItems) {
            if (areItemsEqual(existing, stack)) {
                return false; // Duplicate
            }
        }

        // Add a copy to prevent modification
        loggedItems.add(stack.copy());
        saveItems();
        return true;
    }

    /**
     * Check if two item stacks are equal (same item, damage, and NBT)
     */
    private boolean areItemsEqual(ItemStack a, ItemStack b) {
        if (a == null || b == null)
            return a == b;
        if (a.getItem() != b.getItem())
            return false;
        if (a.getItemDamage() != b.getItemDamage())
            return false;

        NBTTagCompound nbtA = a.getTagCompound();
        NBTTagCompound nbtB = b.getTagCompound();

        if (nbtA == null && nbtB == null)
            return true;
        if (nbtA == null || nbtB == null)
            return false;

        return nbtA.equals(nbtB);
    }

    /**
     * Get all logged items
     */
    public List<ItemStack> getLoggedItems() {
        return new ArrayList<>(loggedItems);
    }

    /**
     * Clear all logged items
     */
    public void clearItems() {
        loggedItems.clear();
        saveItems();
    }

    /**
     * Save items to NBT file (replacing JSON)
     */
    public void saveItems() {
        try {
            NBTTagCompound root = new NBTTagCompound();
            net.minecraft.nbt.NBTTagList itemList = new net.minecraft.nbt.NBTTagList();

            for (ItemStack stack : loggedItems) {
                if (stack != null) {
                    NBTTagCompound itemNbt = new NBTTagCompound();
                    stack.writeToNBT(itemNbt);
                    itemList.appendTag(itemNbt);
                }
            }

            root.setTag("Items", itemList);

            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                net.minecraft.nbt.CompressedStreamTools.writeCompressed(root, fos);
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save item log", e);
        }
    }

    /**
     * Load items from NBT file (replacing JSON)
     */
    public void loadItems() {
        if (!saveFile.exists()) {
            // Try load old JSON file for backward compatibility?
            // Skipping for now as user likely wants fix for new items
            return;
        }

        try (FileInputStream fis = new FileInputStream(saveFile)) {
            NBTTagCompound root = net.minecraft.nbt.CompressedStreamTools.readCompressed(fis);

            if (root.hasKey("Items")) {
                net.minecraft.nbt.NBTTagList itemList = root.getTagList("Items", 10); // 10 = Compound

                loggedItems.clear();

                for (int i = 0; i < itemList.tagCount(); i++) {
                    NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
                    ItemStack stack = ItemStack.loadItemStackFromNBT(itemNbt);
                    if (stack != null) {
                        loggedItems.add(stack);
                    }
                }

                HousingClient.LOGGER.info("Loaded " + loggedItems.size() + " items from item log");
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to load item log", e);
        }
    }

    /**
     * Get item count
     */
    public int getItemCount() {
        return loggedItems.size();
    }
}
