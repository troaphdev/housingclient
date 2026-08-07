package com.housingclient.module.settings;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class ItemSetting extends Setting<ItemStack> {

    private static final String SERIALIZED_STACK_KEY = "stackNbt";

    private boolean blocksOnly;
    private boolean itemsOnly;
    private boolean musicDiscsOnly;
    private boolean throwableOnly;
    private boolean hasKeybind;
    private int keybind = 0;

    public ItemSetting(String name, String description, boolean blocksOnly, boolean itemsOnly) {
        this(name, description, blocksOnly, itemsOnly, false, false);
    }

    public ItemSetting(String name, String description, boolean blocksOnly, boolean itemsOnly, boolean musicDiscsOnly) {
        this(name, description, blocksOnly, itemsOnly, musicDiscsOnly, false);
    }

    public ItemSetting(String name, String description, boolean blocksOnly, boolean itemsOnly, boolean musicDiscsOnly,
            boolean throwableOnly) {
        super(name, description, null);
        this.blocksOnly = blocksOnly;
        this.itemsOnly = itemsOnly;
        this.musicDiscsOnly = musicDiscsOnly;
        this.throwableOnly = throwableOnly;
        this.hasKeybind = false;
    }

    public ItemSetting withKeybind() {
        this.hasKeybind = true;
        return this;
    }

    public boolean hasKeybind() {
        return hasKeybind;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
    }

    public boolean isBlocksOnly() {
        return blocksOnly;
    }

    public boolean isItemsOnly() {
        return itemsOnly;
    }

    public boolean isMusicDiscsOnly() {
        return musicDiscsOnly;
    }

    public boolean isThrowableOnly() {
        return throwableOnly;
    }

    @Override
    public ItemStack getValue() {
        return super.getValue();
    }

    @Override
    public void fromJson(com.google.gson.JsonElement element) {
        if (element.isJsonObject()) {
            com.google.gson.JsonObject obj = element.getAsJsonObject();

            boolean loadedFullStack = false;
            if (obj.has(SERIALIZED_STACK_KEY)) {
                try {
                    byte[] compressed = Base64.getDecoder().decode(obj.get(SERIALIZED_STACK_KEY).getAsString());
                    try (ByteArrayInputStream input = new ByteArrayInputStream(compressed)) {
                        NBTTagCompound itemData = CompressedStreamTools.readCompressed(input);
                        ItemStack stack = ItemStack.loadItemStackFromNBT(itemData);
                        if (stack != null) {
                            setValue(stack);
                            loadedFullStack = true;
                        }
                    }
                } catch (IOException | IllegalArgumentException e) {
                    System.err.println("Failed to load full item data for ItemSetting: " + getName());
                    e.printStackTrace();
                }
            }

            // Backward compatibility with the original ID/meta/SNBT format. A
            // successful legacy load is rewritten in the lossless format next save.
            if (!loadedFullStack && obj.has("id") && obj.has("meta")) {
                int id = obj.get("id").getAsInt();
                int meta = obj.get("meta").getAsInt();
                net.minecraft.item.Item item = net.minecraft.item.Item.getItemById(id);
                if (item != null) {
                    ItemStack stack = new ItemStack(item, 1, meta);
                    if (obj.has("nbt")) {
                        try {
                            String nbtStr = obj.get("nbt").getAsString();
                            net.minecraft.nbt.NBTTagCompound nbt = net.minecraft.nbt.JsonToNBT.getTagFromJson(nbtStr);
                            stack.setTagCompound(nbt);
                        } catch (Exception e) {
                            System.err.println("Failed to parse NBT for ItemSetting: " + getName());
                            e.printStackTrace();
                        }
                    }
                    setValue(stack);
                }
            }
            if (hasKeybind && obj.has("keybind")) {
                keybind = obj.get("keybind").getAsInt();
            }
        }
    }

    @Override
    public com.google.gson.JsonElement toJson() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        if (getValue() != null) {
            NBTTagCompound itemData = new NBTTagCompound();
            getValue().writeToNBT(itemData);

            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                CompressedStreamTools.writeCompressed(itemData, output);
                obj.addProperty(SERIALIZED_STACK_KEY, Base64.getEncoder().encodeToString(output.toByteArray()));
            } catch (IOException e) {
                // This should not fail for an in-memory stream, but retain the old
                // fields as an emergency fallback rather than losing the setting.
                System.err.println("Failed to save full item data for ItemSetting: " + getName());
                e.printStackTrace();
                obj.addProperty("id", net.minecraft.item.Item.getIdFromItem(getValue().getItem()));
                obj.addProperty("meta", getValue().getMetadata());
                if (getValue().hasTagCompound()) {
                    obj.addProperty("nbt", getValue().getTagCompound().toString());
                }
            }
        }
        if (hasKeybind) {
            obj.addProperty("keybind", keybind);
        }
        return obj;
    }
}
