package com.housingclient.module.modules.items;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.item.ItemStack;

public class RenameModule extends Module {
    
    public RenameModule() {
        super("Rename", "Rename items with color codes", Category.ITEMS, ModuleMode.OWNER);
    }
    
    public void renameHeldItem(String name) {
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }
        
        // Format color codes (& -> §)
        String formatted = ChatUtils.formatColorCodes(name);
        
        held.setStackDisplayName(formatted);
        
        ChatUtils.sendClientMessage("§aRenamed item to: " + formatted);
    }
    
    public void resetName() {
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }
        
        held.clearCustomName();
        ChatUtils.sendClientMessage("§aReset item name to default.");
    }
    
    public String getCurrentName() {
        if (mc.thePlayer == null) return "";
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return "";
        
        return held.getDisplayName();
    }
    
    public boolean hasCustomName() {
        if (mc.thePlayer == null) return false;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        
        return held.hasDisplayName();
    }
}

