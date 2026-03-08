package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.storage.CreativeTabStorage;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

public class CreativeTabGUI extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int TAB_WIDTH = 28;
    private static final int TAB_HEIGHT = 28;

    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;
    private static final int ROWS = 6;

    private static boolean isOpen = false;

    public static void drawTabButton(int guiLeft, int guiTop, int xSize, int ySize, int mouseX, int mouseY) {
        int tabX = guiLeft + xSize;
        int tabY = guiTop + 4;

        boolean hover = mouseX >= tabX && mouseX <= tabX + TAB_WIDTH &&
                        mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT;

        int bgColor = hover ? 0xFFCCCCCC : 0xFFAAAAAA;
        int borderColor = isOpen ? 0xFF00FF00 : 0xFF555555;

        RenderUtils.drawBorderedRect(tabX, tabY, TAB_WIDTH, TAB_HEIGHT, 2, bgColor, borderColor);
        mc.fontRendererObj.drawString("\u00A76\u2726", tabX + 10, tabY + 10, -1);

        if (hover) {
            String tooltip = "Creative Tab";
            int tooltipWidth = mc.fontRendererObj.getStringWidth(tooltip);
            RenderUtils.drawRect(mouseX + 8, mouseY, tooltipWidth + 6, 12, 0xDD000000);
            mc.fontRendererObj.drawStringWithShadow(tooltip, mouseX + 11, mouseY + 2, -1);
        }
    }

    public static boolean handleClick(int guiLeft, int guiTop, int xSize, int ySize, int mouseX, int mouseY, int button) {
        int tabX = guiLeft + xSize;
        int tabY = guiTop + 4;

        if (mouseX >= tabX && mouseX <= tabX + TAB_WIDTH &&
            mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            isOpen = !isOpen;
            return true;
        }

        if (isOpen) {
            return handleCreativeTabClick(guiLeft, guiTop, xSize, mouseX, mouseY, button);
        }

        return false;
    }

    private static boolean handleCreativeTabClick(int guiLeft, int guiTop, int xSize, int mouseX, int mouseY, int button) {
        int panelX = guiLeft + xSize + TAB_WIDTH + 5;
        int panelY = guiTop;
        int panelWidth = SLOTS_PER_ROW * SLOT_SIZE + 14;
        int panelHeight = ROWS * SLOT_SIZE + 30;

        if (mouseX < panelX || mouseX > panelX + panelWidth ||
            mouseY < panelY || mouseY > panelY + panelHeight) {
            return false;
        }

        int slotX = (mouseX - panelX - 7) / SLOT_SIZE;
        int slotY = (mouseY - panelY - 20) / SLOT_SIZE;

        if (slotX >= 0 && slotX < SLOTS_PER_ROW && slotY >= 0 && slotY < ROWS) {
            int slot = slotY * SLOTS_PER_ROW + slotX;
            CreativeTabStorage storage = HousingClient.getInstance().getCreativeTabStorage();

            if (button == 0) {
                ItemStack cursorStack = mc.thePlayer.inventory.getItemStack();
                if (cursorStack != null) {
                    storage.setItem(slot, cursorStack.copy());
                    storage.save();
                } else if (storage.hasItem(slot)) {
                    ItemStack item = storage.getItem(slot);
                    if (item != null) {
                        ItemStack copy = item.copy();
                        copy.stackSize = copy.getMaxStackSize();
                        mc.thePlayer.inventory.setItemStack(copy);
                    }
                }
            } else if (button == 1) {
                storage.clearSlot(slot);
                storage.save();
            }
            return true;
        }
        return true;
    }

    public static void drawCreativeTabPanel(int guiLeft, int guiTop, int xSize, int mouseX, int mouseY) {
        if (!isOpen) return;

        int panelX = guiLeft + xSize + TAB_WIDTH + 5;
        int panelY = guiTop;
        int panelWidth = SLOTS_PER_ROW * SLOT_SIZE + 14;
        int panelHeight = ROWS * SLOT_SIZE + 30;

        RenderUtils.drawBorderedRect(panelX, panelY, panelWidth, panelHeight, 2, 0xDD222233, 0xFF00CCFF);
        mc.fontRendererObj.drawStringWithShadow("\u00A7b\u00A7lCreative Tab", panelX + 7, panelY + 6, -1);

        CreativeTabStorage storage = HousingClient.getInstance().getCreativeTabStorage();

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slot = row * SLOTS_PER_ROW + col;
                int slotXPos = panelX + 7 + col * SLOT_SIZE;
                int slotYPos = panelY + 20 + row * SLOT_SIZE;

                boolean hover = mouseX >= slotXPos && mouseX <= slotXPos + SLOT_SIZE - 2 &&
                               mouseY >= slotYPos && mouseY <= slotYPos + SLOT_SIZE - 2;
                int slotColor = hover ? 0xFF444466 : 0xFF333344;
                RenderUtils.drawRect(slotXPos, slotYPos, SLOT_SIZE - 2, SLOT_SIZE - 2, slotColor);

                ItemStack item = storage.getItem(slot);
                if (item != null) {
                    mc.getRenderItem().renderItemAndEffectIntoGUI(item, slotXPos + 1, slotYPos + 1);
                    mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, item, slotXPos + 1, slotYPos + 1);
                }
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();

        int itemCount = storage.getItemCount();
        String countText = "Items: " + itemCount + "/" + CreativeTabStorage.SIZE;
        mc.fontRendererObj.drawStringWithShadow(countText, panelX + panelWidth - mc.fontRendererObj.getStringWidth(countText) - 5,
            panelY + panelHeight - 12, 0xFF888888);
    }

    public static boolean isOpen() {
        return isOpen;
    }

    public static void setOpen(boolean open) {
        isOpen = open;
    }

    public static void toggle() {
        isOpen = !isOpen;
    }
}
