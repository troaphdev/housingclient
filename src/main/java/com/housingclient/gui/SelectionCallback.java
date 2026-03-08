package com.housingclient.gui;

import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface SelectionCallback {
    void onSelect(ItemStack stack);
}
