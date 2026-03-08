package com.housingclient.gui;

import com.housingclient.module.settings.ItemSetting;
import com.housingclient.utils.RenderUtils;
import com.housingclient.utils.ChatUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Item Selector GUI that dynamically scales to fit all items on screen without
 * scrolling.
 */
public class ItemSelectorGUI extends GuiScreen {

    private final GuiScreen parent;
    private final ItemSetting setting;
    private final SelectionCallback callback;
    private GuiTextField searchField;
    private GuiTextField customItemField;
    private List<ItemStack> allItems = new ArrayList<>();
    private List<ItemStack> filteredItems = new ArrayList<>();

    // Layout constants
    private static final int TOP_BAR_HEIGHT = 40;
    private static final int PADDING = 10;
    private static final int MIN_ITEM_SIZE = 12;
    private static final int MAX_ITEM_SIZE = 24;
    private static final int MIN_GAP = 2;

    // Dynamic layout values
    private int itemSize = 18;
    private int gap = 3;
    private int itemsPerRow = 1;
    private int gridStartX = 0;
    private int gridStartY = TOP_BAR_HEIGHT;

    public ItemSelectorGUI(GuiScreen parent, ItemSetting setting) {
        this.parent = parent;
        this.setting = setting;
        this.callback = null;
        loadItems();
    }

    public ItemSelectorGUI(GuiScreen parent, SelectionCallback callback) {
        this.parent = parent;
        this.setting = null;
        this.callback = callback;
        loadItems();
    }

    private void loadItems() {
        allItems.clear();

        for (Item item : Item.itemRegistry) {
            if (item == null)
                continue;

            // Exclude broken items that missing textures
            if (item == Item.getItemFromBlock(Blocks.farmland) ||
                    item == Item.getItemFromBlock(Blocks.lit_furnace) ||
                    item == Item.getItemFromBlock(Blocks.lit_redstone_ore)) {
                continue;
            }

            // Filter Blocks/Items
            boolean isBlock = item instanceof net.minecraft.item.ItemBlock;
            boolean isAllowed = true;

            if (setting != null) {
                if (setting.isBlocksOnly() && !isBlock) {
                    // Exception: Allow Water/Lava buckets and Splash Potions
                    if (item != net.minecraft.init.Items.water_bucket &&
                            item != net.minecraft.init.Items.lava_bucket &&
                            item != net.minecraft.init.Items.potionitem) {
                        isAllowed = false;
                    }
                }
                if (setting.isItemsOnly() && isBlock)
                    isAllowed = false;

                if (setting.isMusicDiscsOnly()) {
                    // Check if's a music disc. In 1.8.9, they are Items.record_*
                    // A simple check is to see if the item class is ItemRecord
                    if (!(item instanceof net.minecraft.item.ItemRecord)) {
                        isAllowed = false;
                    }
                }

                if (setting.isThrowableOnly()) {
                    // Allow only throwable items: Snowball, Egg, EnderPearl, ExpBottle, Potion
                    // (Splash)
                    boolean isThrowable = item instanceof net.minecraft.item.ItemSnowball ||
                            item instanceof net.minecraft.item.ItemEgg ||
                            item instanceof net.minecraft.item.ItemEnderPearl ||
                            item instanceof net.minecraft.item.ItemExpBottle ||
                            item == net.minecraft.init.Items.potionitem;

                    if (!isThrowable) {
                        isAllowed = false;
                    }
                }
            }

            if (!isAllowed)
                continue;

            List<ItemStack> subItems = new ArrayList<>();
            try {
                item.getSubItems(item, item.getCreativeTab(), subItems);
            } catch (Exception e) {
                if (item.getCreativeTab() == null) {
                    subItems.add(new ItemStack(item));
                }
            }

            // Post-filter for Splash Potions if blocksOnly OR throwableOnly is enabled
            if (setting != null && (setting.isBlocksOnly() || setting.isThrowableOnly())
                    && item == net.minecraft.init.Items.potionitem) {
                java.util.Iterator<ItemStack> it = subItems.iterator();
                while (it.hasNext()) {
                    ItemStack stack = it.next();
                    // Only allow splash potions (metadata bit 16384 check handles this usually, but
                    // ItemPotion.isSplash is cleaner)
                    if (!net.minecraft.item.ItemPotion.isSplash(stack.getMetadata())) {
                        it.remove();
                    }
                }
            }

            allItems.addAll(subItems);
        }
        filterItems("");
    }

    private void filterItems(String query) {
        filteredItems.clear();
        String q = query.toLowerCase();
        for (ItemStack stack : allItems) {
            if (stack == null || stack.getItem() == null)
                continue;
            if (q.isEmpty() || stack.getDisplayName().toLowerCase().contains(q)) {
                filteredItems.add(stack);
            }
        }
        calculateLayout();
    }

    /**
     * Calculate optimal item size and grid layout to fit all items on screen.
     */
    private void calculateLayout() {
        if (filteredItems.isEmpty()) {
            itemsPerRow = 1;
            return;
        }

        int availableWidth = width - (PADDING * 2);
        int availableHeight = height - TOP_BAR_HEIGHT - PADDING;
        int itemCount = filteredItems.size();

        // Try different item sizes from max to min to find the best fit
        for (int testSize = MAX_ITEM_SIZE; testSize >= MIN_ITEM_SIZE; testSize--) {
            int testGap = Math.max(MIN_GAP, testSize / 6);
            int cellSize = testSize + testGap;

            int cols = availableWidth / cellSize;
            if (cols < 1)
                cols = 1;

            int rows = (int) Math.ceil((double) itemCount / cols);
            int totalHeight = rows * cellSize;

            if (totalHeight <= availableHeight) {
                itemSize = testSize;
                gap = testGap;
                itemsPerRow = cols;

                // Center the grid horizontally
                int gridWidth = cols * cellSize;
                gridStartX = PADDING + (availableWidth - gridWidth) / 2;
                gridStartY = TOP_BAR_HEIGHT;
                return;
            }
        }

        // If nothing fits, use minimum size and accept some clipping
        itemSize = MIN_ITEM_SIZE;
        gap = MIN_GAP;
        int cellSize = itemSize + gap;
        itemsPerRow = Math.max(1, availableWidth / cellSize);

        int gridWidth = itemsPerRow * cellSize;
        gridStartX = PADDING + (availableWidth - gridWidth) / 2;
        gridStartY = TOP_BAR_HEIGHT;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int middleX = width / 2;

        // Search field on left side of center
        searchField = new GuiTextField(0, fontRendererObj, middleX - 160, 10, 150, 20);
        searchField.setFocused(true);
        searchField.setCanLoseFocus(true);
        searchField.setText("");

        // Custom item field on right side of center
        customItemField = new GuiTextField(1, fontRendererObj, middleX + 10, 10, 150, 20);
        customItemField.setMaxStringLength(32767);

        filterItems(searchField.getText());
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        searchField.updateCursorCounter();
        customItemField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Draw Search
        searchField.drawTextBox();
        if (searchField.getText().isEmpty() && !searchField.isFocused()) {
            fontRendererObj.drawString("Search...", searchField.xPosition + 4, searchField.yPosition + 6, 0xFFAAAAAA);
        }

        // Draw Custom Item Field
        customItemField.drawTextBox();
        if (customItemField.getText().isEmpty() && !customItemField.isFocused()) {
            fontRendererObj.drawString("item_id [count] [meta] {nbt}", customItemField.xPosition + 4,
                    customItemField.yPosition + 6, 0xFFAAAAAA);
        }

        // Draw item count info
        String countInfo = filteredItems.size() + " items";
        fontRendererObj.drawString(countInfo, width - PADDING - fontRendererObj.getStringWidth(countInfo),
                TOP_BAR_HEIGHT - 12, 0xFFAAAAAA);

        // Draw Items
        int cellSize = itemSize + gap;
        int hoveredIndex = -1;

        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 0; i < filteredItems.size(); i++) {
            int col = i % itemsPerRow;
            int row = i / itemsPerRow;
            int x = gridStartX + col * cellSize;
            int y = gridStartY + row * cellSize;

            // Skip if off screen
            if (y + itemSize < TOP_BAR_HEIGHT || y > height)
                continue;

            ItemStack stack = filteredItems.get(i);
            boolean hovered = mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize;

            if (hovered) {
                hoveredIndex = i;
                RenderUtils.drawRect(x - 1, y - 1, itemSize + 2, itemSize + 2, 0x80FFFFFF);
            }

            // Scale and render item
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0);
            float scale = itemSize / 16.0f;
            GlStateManager.scale(scale, scale, 1.0f);
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
            GlStateManager.popMatrix();
        }

        RenderHelper.disableStandardItemLighting();

        // Draw tooltip last (on top of everything)
        if (hoveredIndex >= 0 && hoveredIndex < filteredItems.size()) {
            renderToolTip(filteredItems.get(hoveredIndex), mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        searchField.mouseClicked(mouseX, mouseY, mouseButton);
        customItemField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseY >= TOP_BAR_HEIGHT && mouseButton == 0) {
            int cellSize = itemSize + gap;

            for (int i = 0; i < filteredItems.size(); i++) {
                int col = i % itemsPerRow;
                int row = i / itemsPerRow;
                int x = gridStartX + col * cellSize;
                int y = gridStartY + row * cellSize;

                if (mouseX >= x && mouseX < x + itemSize && mouseY >= y && mouseY < y + itemSize) {
                    if (callback != null) {
                        callback.onSelect(filteredItems.get(i));
                    } else if (setting != null) {
                        setting.setValue(filteredItems.get(i));
                    }
                    mc.displayGuiScreen(parent);
                    return;
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField.textboxKeyTyped(typedChar, keyCode)) {
            filterItems(searchField.getText());
            return;
        }

        if (customItemField.isFocused() && keyCode == Keyboard.KEY_RETURN) {
            parseAndSetItem(customItemField.getText());
            return;
        }

        if (customItemField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void parseAndSetItem(String input) {
        if (input == null || input.trim().isEmpty())
            return;

        try {
            String itemPart = input.trim();
            String nbtPart = null;

            int braceIdx = input.indexOf('{');
            if (braceIdx > 0) {
                itemPart = input.substring(0, braceIdx).trim();
                nbtPart = input.substring(braceIdx).trim();
            }

            String[] parts = itemPart.split("\\s+");
            String itemId = parts[0];
            int count = 1;
            int meta = 0;

            if (parts.length >= 2) {
                try {
                    count = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            if (parts.length >= 3) {
                try {
                    meta = Integer.parseInt(parts[2]);
                } catch (NumberFormatException ignored) {
                }
            }

            Item item = Item.getByNameOrId(itemId);
            if (item == null) {
                ChatUtils.sendClientMessage("\u00A7cError: Item '" + itemId + "' not found.");
                return;
            }

            ItemStack stack = new ItemStack(item, count, meta);

            if (nbtPart != null && !nbtPart.isEmpty()) {
                nbtPart = nbtPart.replaceAll("\\d+:\"", "\"");

                try {
                    NBTTagCompound nbt = JsonToNBT.getTagFromJson(nbtPart);
                    stack.setTagCompound(nbt);

                    if (stack.hasTagCompound() && stack.getTagCompound().hasKey("display", 10)) {
                        NBTTagCompound display = stack.getTagCompound().getCompoundTag("display");

                        if (display.hasKey("Name", 8)) {
                            display.setString("Name", ChatUtils.formatColorCodes(display.getString("Name")));
                        }

                        if (display.hasKey("Lore", 9)) {
                            net.minecraft.nbt.NBTTagList lore = display.getTagList("Lore", 8);
                            for (int i = 0; i < lore.tagCount(); i++) {
                                lore.set(i, new net.minecraft.nbt.NBTTagString(
                                        ChatUtils.formatColorCodes(lore.getStringTagAt(i))));
                            }
                        }
                    }
                } catch (NBTException e) {
                    ChatUtils.sendClientMessage("\u00A7cError: Invalid NBT format.");
                    return;
                }
            }

            if (callback != null) {
                callback.onSelect(stack);
            } else if (setting != null) {
                setting.setValue(stack);
            }

            mc.displayGuiScreen(parent);

        } catch (Exception e) {
            ChatUtils.sendClientMessage("\u00A7cError parsing item.");
            e.printStackTrace();
        }
    }
}
