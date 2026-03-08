package com.housingclient.module.modules.items;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class UnbreakableModule extends Module {
    
    private final BooleanSetting autoApply = new BooleanSetting("Auto Apply", "Auto-apply to held items", false);
    private final BooleanSetting showInTooltip = new BooleanSetting("Show in Tooltip", "Show unbreakable status", true);
    
    public UnbreakableModule() {
        super("Unbreakable", "Make items unbreakable", Category.ITEMS, ModuleMode.OWNER);
        
        addSetting(autoApply);
        addSetting(showInTooltip);
    }
    
    @Override
    public void onTick() {
        if (!autoApply.isEnabled()) return;
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.isItemStackDamageable()) {
            if (!isUnbreakable(held)) {
                setUnbreakable(held, true);
            }
        }
    }
    
    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (!showInTooltip.isEnabled()) return;
        
        if (isUnbreakable(event.itemStack)) {
            event.toolTip.add("§5§lUnbreakable");
        }
    }
    
    public void setUnbreakable(ItemStack stack, boolean unbreakable) {
        if (stack == null) return;
        
        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }
        
        if (unbreakable) {
            nbt.setBoolean("Unbreakable", true);
            ChatUtils.sendClientMessage("§aSet " + stack.getDisplayName() + " §ato unbreakable!");
        } else {
            nbt.removeTag("Unbreakable");
            ChatUtils.sendClientMessage("§cRemoved unbreakable from " + stack.getDisplayName());
        }
    }
    
    public boolean isUnbreakable(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) return false;
        return stack.getTagCompound().getBoolean("Unbreakable");
    }
    
    public void toggleUnbreakable() {
        if (mc.thePlayer == null) return;
        
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }
        
        setUnbreakable(held, !isUnbreakable(held));
    }
}

