package com.housingclient.module.modules.items;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;

public class LoreEditorModule extends Module {
    
    public LoreEditorModule() {
        super("Lore Editor", "Edit item lore/description", Category.ITEMS, ModuleMode.OWNER);
    }
    
    public void setLore(ItemStack stack, List<String> lore) {
        if (stack == null) {
            ChatUtils.sendClientMessage("§cNo item to edit!");
            return;
        }
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        
        NBTTagCompound display = nbt.getCompoundTag("display");
        if (display == null) {
            display = new NBTTagCompound();
        }
        
        NBTTagList loreList = new NBTTagList();
        for (String line : lore) {
            // Support color codes
            String formatted = ChatUtils.formatColorCodes(line);
            loreList.appendTag(new NBTTagString(formatted));
        }
        
        display.setTag("Lore", loreList);
        nbt.setTag("display", display);
        
        ChatUtils.sendClientMessage("§aUpdated lore on " + stack.getDisplayName());
    }
    
    public void addLoreLine(ItemStack stack, String line) {
        List<String> lore = getLore(stack);
        lore.add(line);
        setLore(stack, lore);
    }
    
    public void insertLoreLine(ItemStack stack, int index, String line) {
        List<String> lore = getLore(stack);
        if (index < 0) index = 0;
        if (index > lore.size()) index = lore.size();
        lore.add(index, line);
        setLore(stack, lore);
    }
    
    public void removeLoreLine(ItemStack stack, int index) {
        List<String> lore = getLore(stack);
        if (index >= 0 && index < lore.size()) {
            lore.remove(index);
            setLore(stack, lore);
        } else {
            ChatUtils.sendClientMessage("§cInvalid line index!");
        }
    }
    
    public void clearLore(ItemStack stack) {
        setLore(stack, new ArrayList<>());
        ChatUtils.sendClientMessage("§cCleared lore from item.");
    }
    
    public List<String> getLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();
        
        if (stack == null || !stack.hasTagCompound()) return lore;
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey("display")) return lore;
        
        NBTTagCompound display = nbt.getCompoundTag("display");
        if (!display.hasKey("Lore")) return lore;
        
        NBTTagList loreList = display.getTagList("Lore", 8); // 8 = String tag
        for (int i = 0; i < loreList.tagCount(); i++) {
            lore.add(loreList.getStringTagAt(i));
        }
        
        return lore;
    }
    
    public void viewLore() {
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }
        
        List<String> lore = getLore(held);
        
        if (lore.isEmpty()) {
            ChatUtils.sendClientMessage("§7Item has no lore.");
            return;
        }
        
        ChatUtils.sendClientMessage("§6§l--- Lore ---");
        for (int i = 0; i < lore.size(); i++) {
            ChatUtils.sendClientMessageNoPrefix("§7" + i + ": §f" + lore.get(i));
        }
        ChatUtils.sendClientMessage("§6§l------------");
    }
    
    public void setLoreFromChat(String... lines) {
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }
        
        List<String> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(line);
        }
        
        setLore(held, lore);
    }
}

