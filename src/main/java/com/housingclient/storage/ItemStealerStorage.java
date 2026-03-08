package com.housingclient.storage;

import com.housingclient.HousingClient;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Storage for Item Stealer creative tab
 * Separate from the NBT Logger storage
 */
public class ItemStealerStorage {

    // 9 columns x 6 rows = 54 slots (like a large chest)
    public static final int ROWS = 6;
    public static final int COLS = 9;
    public static final int SIZE = ROWS * COLS;

    private final ItemStack[] items = new ItemStack[SIZE];
    private final File storageFile;

    public ItemStealerStorage(File dataDir) {
        this.storageFile = new File(dataDir, "itemstealer.db");
    }

    public void load() {
        if (!storageFile.exists()) {
            return;
        }

        try (FileInputStream fis = new FileInputStream(storageFile)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);

            if (root.hasKey("items")) {
                NBTTagList itemList = root.getTagList("items", 10); // 10 = Compound tag

                for (int i = 0; i < itemList.tagCount(); i++) {
                    NBTTagCompound itemNbt = itemList.getCompoundTagAt(i);
                    int slot = itemNbt.getInteger("slot");

                    if (slot >= 0 && slot < SIZE) {
                        items[slot] = ItemStack.loadItemStackFromNBT(itemNbt);
                    }
                }
            }

            HousingClient.LOGGER.info("Loaded " + getItemCount() + " items from Item Stealer storage.");
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to load Item Stealer storage", e);
        }
    }

    public void save() {
        try {
            NBTTagCompound root = new NBTTagCompound();
            NBTTagList itemList = new NBTTagList();

            for (int i = 0; i < SIZE; i++) {
                if (items[i] != null) {
                    NBTTagCompound itemNbt = new NBTTagCompound();
                    items[i].writeToNBT(itemNbt);
                    itemNbt.setInteger("slot", i);
                    itemList.appendTag(itemNbt);
                }
            }

            root.setTag("items", itemList);

            try (FileOutputStream fos = new FileOutputStream(storageFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }

            HousingClient.LOGGER.info("Saved " + getItemCount() + " items to Item Stealer storage.");
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save Item Stealer storage", e);
        }
    }

    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= SIZE)
            return null;
        return items[slot];
    }

    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE)
            return;
        items[slot] = stack != null ? stack.copy() : null;
    }

    public void clearSlot(int slot) {
        if (slot >= 0 && slot < SIZE) {
            items[slot] = null;
        }
    }

    public void clearAll() {
        for (int i = 0; i < SIZE; i++) {
            items[i] = null;
        }
    }

    public int getItemCount() {
        int count = 0;
        for (ItemStack item : items) {
            if (item != null)
                count++;
        }
        return count;
    }

    public boolean isEmpty() {
        return getItemCount() == 0;
    }

    public boolean hasItem(int slot) {
        return slot >= 0 && slot < SIZE && items[slot] != null;
    }

    public ItemStack[] getItems() {
        return items;
    }

    // Find first empty slot
    public int findEmptySlot() {
        for (int i = 0; i < SIZE; i++) {
            if (items[i] == null) {
                return i;
            }
        }
        return -1;
    }

    // Add item to first empty slot
    public boolean addItem(ItemStack stack) {
        int slot = findEmptySlot();
        if (slot != -1) {
            setItem(slot, stack);
            save(); // Auto-save when adding items
            return true;
        }
        return false;
    }
}
