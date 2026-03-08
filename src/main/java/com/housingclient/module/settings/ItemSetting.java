package com.housingclient.module.settings;

import net.minecraft.item.ItemStack;

public class ItemSetting extends Setting<ItemStack> {

    private boolean blocksOnly;
    private boolean itemsOnly;
    private boolean musicDiscsOnly;
    private boolean throwableOnly;

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
            if (obj.has("id") && obj.has("meta")) {
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
        }
    }

    @Override
    public com.google.gson.JsonElement toJson() {
        com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
        if (getValue() != null) {
            obj.addProperty("id", net.minecraft.item.Item.getIdFromItem(getValue().getItem()));
            obj.addProperty("meta", getValue().getMetadata());
            if (getValue().hasTagCompound()) {
                obj.addProperty("nbt", getValue().getTagCompound().toString());
            }
        }
        return obj;
    }
}
