package com.housingclient.module.modules.visuals;

import com.housingclient.gui.ClickGUI;
import com.housingclient.gui.ItemSelectorGUI;
import com.housingclient.gui.LegacyClickGUI;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ItemSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;

/**
 * Replaces matching item stacks only while the client renders them.
 *
 * The original stacks remain in inventories, entities, containers, and packets;
 * a copied replacement stack is supplied only to rendering code.
 */
public class ItemDisguiserModule extends Module {

    private static ItemDisguiserModule instance;
    private static String recentRealHeldName;
    private static String recentRealHeldPopupName;
    private static String recentVisualHeldPopupName;
    private static long recentHeldDisguiseAt;

    private final ItemSetting itemToReplace = new ItemSetting(
            "Item to Replace",
            "Exact item, metadata, name, lore, and NBT to hide",
            false,
            false);

    private final ItemSetting replacementItem = new ItemSetting(
            "Replacement Item",
            "Item, name, lore, and NBT to show instead",
            false,
            false);

    private final BooleanSetting broadItemType = new BooleanSetting(
            "Broad Item Type",
            "Match every stack of the selected item type and ignore metadata and NBT",
            false);

    public ItemDisguiserModule() {
        super("Item Disguiser", "Visually replace selected items without changing server-side inventory data",
                Category.VISUALS, ModuleMode.BOTH);
        instance = this;
        addSetting(itemToReplace);
        addSetting(replacementItem);
        addSetting(broadItemType);
    }

    /**
     * Returns a detached display copy when the stack matches this module's rule.
     * The real stack is never modified. Its count is retained so inventory and
     * hotbar quantities remain truthful while all other visible data comes from
     * the replacement.
     */
    public static ItemStack getVisualStack(ItemStack original) {
        if (original == null || instance == null || !instance.isEnabled()) {
            return original;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft != null) {
            Object currentScreen = minecraft.currentScreen;
            if (currentScreen instanceof ClickGUI
                    || currentScreen instanceof LegacyClickGUI
                    || currentScreen instanceof ItemSelectorGUI) {
                // Keep HousingClient's menus and item pickers readable so the
                // configured real and replacement stacks can be distinguished.
                return original;
            }
        }

        ItemStack target = instance.itemToReplace.getValue();
        ItemStack replacement = instance.replacementItem.getValue();
        if (target == null || replacement == null || !instance.matches(original, target)) {
            return original;
        }

        ItemStack visual = replacement.copy();
        visual.stackSize = original.stackSize;
        return visual;
    }

    /**
     * Replaces the selected-item popup string at the final draw and measurement
     * boundary. This remains effective when another client mod alters
     * GuiIngame's selected-item rendering method.
     */
    public static String replaceHeldItemName(String text) {
        if (text == null || instance == null || !instance.isEnabled()) {
            return text;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.thePlayer == null) {
            return text;
        }

        updateRecentHeldDisguise(minecraft.thePlayer.inventory.getCurrentItem());

        if (System.currentTimeMillis() - recentHeldDisguiseAt <= 500L
                && (text.equals(recentRealHeldName) || text.equals(recentRealHeldPopupName))) {
            return recentVisualHeldPopupName;
        }
        return text;
    }

    private static void updateRecentHeldDisguise(ItemStack realStack) {
        ItemStack visualStack = getVisualStack(realStack);
        if (realStack == null || visualStack == null || visualStack == realStack) {
            return;
        }

        String realName = realStack.getDisplayName();
        String visualName = visualStack.getDisplayName();
        recentRealHeldName = realName;
        recentRealHeldPopupName = realStack.hasDisplayName() ? "\u00A7o" + realName : realName;
        recentVisualHeldPopupName = visualStack.hasDisplayName() ? "\u00A7o" + visualName : visualName;
        recentHeldDisguiseAt = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer != null) {
            updateRecentHeldDisguise(mc.thePlayer.inventory.getCurrentItem());
        }
    }

    @Override
    protected void onDisable() {
        recentRealHeldName = null;
        recentRealHeldPopupName = null;
        recentVisualHeldPopupName = null;
        recentHeldDisguiseAt = 0L;
    }

    private boolean matches(ItemStack candidate, ItemStack target) {
        if (candidate.getItem() != target.getItem()) {
            return false;
        }

        if (broadItemType.isEnabled()) {
            return true;
        }

        return candidate.getMetadata() == target.getMetadata()
                && ItemStack.areItemStackTagsEqual(candidate, target);
    }

}
