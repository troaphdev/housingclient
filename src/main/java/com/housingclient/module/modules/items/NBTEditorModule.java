package com.housingclient.module.modules.items;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;

public class NBTEditorModule extends Module {

    private final BooleanSetting viewOnHold = new BooleanSetting("View on Hold", "Hold R to view NBT", true);

    private boolean isViewing = false;

    public NBTEditorModule() {
        super("NBT Editor", "View and edit item NBT data", Category.MISCELLANEOUS, ModuleMode.OWNER);

        addSetting(viewOnHold);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.currentScreen != null)
            return;
        if (mc.thePlayer == null)
            return;

        if (viewOnHold.isEnabled() && Keyboard.isKeyDown(Keyboard.KEY_R)) {
            if (!isViewing) {
                viewCurrentItemNBT();
                isViewing = true;
            }
        } else {
            isViewing = false;
        }
    }

    public void viewCurrentItemNBT() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) {
            ChatUtils.sendClientMessage("§cNo item in hand!");
            return;
        }

        NBTTagCompound nbt = held.getTagCompound();
        if (nbt == null) {
            ChatUtils.sendClientMessage("§7Item has no NBT data.");
            return;
        }

        ChatUtils.sendClientMessage("§6§l--- NBT Data ---");
        ChatUtils.sendClientMessageNoPrefix("§7Item: §f" + held.getDisplayName());
        printNBT(nbt, 0);
        ChatUtils.sendClientMessage("§6§l-----------------");
    }

    private void printNBT(NBTTagCompound nbt, int indent) {
        String prefix = new String(new char[indent]).replace("\0", "  ");

        for (String key : nbt.getKeySet()) {
            Object value = nbt.getTag(key);

            if (value instanceof NBTTagCompound) {
                ChatUtils.sendClientMessageNoPrefix(prefix + "§b" + key + "§7: §f{");
                printNBT((NBTTagCompound) value, indent + 1);
                ChatUtils.sendClientMessageNoPrefix(prefix + "§f}");
            } else if (value instanceof NBTTagList) {
                ChatUtils.sendClientMessageNoPrefix(prefix + "§b" + key + "§7: §f[...]");
            } else {
                ChatUtils.sendClientMessageNoPrefix(prefix + "§b" + key + "§7: §f" + value);
            }
        }
    }

    // NBT Modification methods
    public void setUnbreakable(ItemStack stack, boolean unbreakable) {
        if (stack == null)
            return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        if (unbreakable) {
            nbt.setBoolean("Unbreakable", true);
        } else {
            nbt.removeTag("Unbreakable");
        }
    }

    public void setHideFlags(ItemStack stack, int flags) {
        if (stack == null)
            return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        nbt.setInteger("HideFlags", flags);
    }

    public void setLore(ItemStack stack, List<String> lore) {
        if (stack == null)
            return;

        NBTTagCompound nbt = stack.getTagCompound();
        if (nbt == null) {
            nbt = new NBTTagCompound();
            stack.setTagCompound(nbt);
        }

        NBTTagCompound display = nbt.getCompoundTag("display");
        NBTTagList loreList = new NBTTagList();

        for (String line : lore) {
            loreList.appendTag(new NBTTagString(ChatUtils.formatColorCodes(line)));
        }

        display.setTag("Lore", loreList);
        nbt.setTag("display", display);
    }

    public List<String> getLore(ItemStack stack) {
        List<String> lore = new ArrayList<>();

        if (stack == null || !stack.hasTagCompound())
            return lore;

        NBTTagCompound nbt = stack.getTagCompound();
        if (!nbt.hasKey("display"))
            return lore;

        NBTTagCompound display = nbt.getCompoundTag("display");
        if (!display.hasKey("Lore"))
            return lore;

        NBTTagList loreList = display.getTagList("Lore", 8);
        for (int i = 0; i < loreList.tagCount(); i++) {
            lore.add(loreList.getStringTagAt(i));
        }

        return lore;
    }

    public void setCustomName(ItemStack stack, String name) {
        if (stack == null)
            return;
        stack.setStackDisplayName(ChatUtils.formatColorCodes(name));
    }

    public void setStackSize(ItemStack stack, int size) {
        if (stack == null)
            return;
        stack.stackSize = Math.min(64, Math.max(1, size));
    }

    public boolean isUnbreakable(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound())
            return false;
        return stack.getTagCompound().getBoolean("Unbreakable");
    }

    public int getHideFlags(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound())
            return 0;
        return stack.getTagCompound().getInteger("HideFlags");
    }
}
