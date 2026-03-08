package com.housingclient.storage;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import com.housingclient.HousingClient;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Creative Tab for Item Stealer items.
 * Uses reflection to set icon and directly returns tab name without
 * translation.
 */
public class ItemStealerTab extends CreativeTabs {

    public static ItemStealerTab INSTANCE;

    public ItemStealerTab() {
        super("itemstealer");
        INSTANCE = this;
        setIconStackViaReflection(new ItemStack(Items.chest_minecart));
    }

    private void setIconStackViaReflection(ItemStack stack) {
        try {
            Field field = null;
            for (Field f : CreativeTabs.class.getDeclaredFields()) {
                if (f.getType() == ItemStack.class) {
                    field = f;
                    break;
                }
            }
            if (field != null) {
                field.setAccessible(true);
                field.set(this, stack);
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    @Override
    public Item getTabIconItem() {
        return Items.chest_minecart;
    }

    @Override
    public ItemStack getIconItemStack() {
        return new ItemStack(Items.chest_minecart);
    }

    @Override
    public String getTabLabel() {
        return "itemstealer";
    }

    @Override
    public void displayAllReleventItems(List<ItemStack> items) {
        ItemStealerStorage storage = HousingClient.getInstance().getItemStealerStorage();
        if (storage != null) {
            for (ItemStack stack : storage.getItems()) {
                if (stack != null && stack.getItem() != null) {
                    items.add(stack.copy());
                }
            }
        }
    }

    public boolean hasItems() {
        ItemStealerStorage storage = HousingClient.getInstance().getItemStealerStorage();
        return storage != null && storage.getItems().length > 0;
    }

    @Override
    public boolean hasSearchBar() {
        return false;
    }
}
