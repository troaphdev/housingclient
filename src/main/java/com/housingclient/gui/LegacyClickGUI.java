package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.gui.theme.Theme;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.settings.*;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Rise-style ClickGUI with proper rounded corners, animated gradient branding,
 * and clean visual design
 */
public class LegacyClickGUI extends GuiScreen {

    // GUI Dimensions
    private static final int GUI_WIDTH = 560;
    private static final int GUI_HEIGHT = 360;
    private static final int SIDEBAR_WIDTH = 100;
    private static final int HEADER_HEIGHT = 50;
    private static final int MODULE_HEIGHT = 48;
    private static final int SETTING_HEIGHT = 18;
    private static final int CORNER_RADIUS = 18;

    // State
    private Category selectedCategory = Category.COMBAT;
    private Module expandedModule = null;
    private Module bindingModule = null;
    private String searchText = "";
    private boolean searchFocused = false;
    private float scrollOffset = 0;
    private float targetScroll = 0;
    private float openAnimation = 0;
    private long animationStartTime = 0;

    // Slider drag state
    private NumberSetting draggingSetting = null;
    private int draggingSliderX = 0;

    // Number editing state
    private NumberSetting editingSetting = null;
    private String editingValue = "";

    // Categories (excluding Client, Misc, Items)
    private final List<CategoryEntry> categories = new ArrayList<>();

    private Theme theme;

    public LegacyClickGUI() {
        super();
        theme = new Theme();
        initCategories();
    }

    private void initCategories() {
        categories.clear();
        categories.add(new CategoryEntry(null, "Search"));
        categories.add(new CategoryEntry(Category.COMBAT, "Combat"));
        categories.add(new CategoryEntry(Category.MOVEMENT, "Movement"));
        categories.add(new CategoryEntry(Category.VISUALS, "Render"));
        categories.add(new CategoryEntry(Category.EXPLOIT, "Exploit"));
        categories.add(new CategoryEntry(Category.BUILDING, "Building"));
        categories.add(new CategoryEntry(Category.INSTANT, "Instant"));
        categories.add(new CategoryEntry(Category.CLIENT, "Client"));
    }

    @Override
    public void initGui() {
        openAnimation = 0;
        scrollOffset = 0;
        targetScroll = 0;
        animationStartTime = System.currentTimeMillis();

        // Default to Search and focus it
        selectedCategory = null;
        searchFocused = true;
        searchText = "";
    }

    @Override
    public void onGuiClosed() {
        HousingClient.getInstance().saveAll();
        bindingModule = null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Smooth scrolling interpolation
        // scrollOffset = RenderUtils.lerp(scrollOffset, targetScroll, 0.1f);
        scrollOffset = scrollOffset + (targetScroll - scrollOffset) * 0.1f;
        if (Math.abs(scrollOffset - targetScroll) < 0.1f)
            scrollOffset = targetScroll;

        // Clamp scroll logic (re-calculate max scroll to ensure target doesn't
        // overshoot if content changes)
        int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;
        int totalHeight = calculateTotalHeight(getDisplayModules());
        int maxScroll = Math.max(0, totalHeight - contentHeight + 20);

        targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        // Update slider while dragging (hold and drag)
        if (draggingSetting != null && org.lwjgl.input.Mouse.isButtonDown(0)) {
            double progress = Math.max(0, Math.min(1, (mouseX - draggingSliderX) / 140.0));
            draggingSetting.setValue(

                    draggingSetting.getMin() + (draggingSetting.getMax() - draggingSetting.getMin()) * progress);
        } else {
            draggingSetting = null;
        }

        if (this.doesGuiPauseGame()) {
            this.drawDefaultBackground();
        }

        openAnimation = Math.min(1, openAnimation + 0.08f);

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        int guiX = (screenWidth - GUI_WIDTH) / 2;
        int guiY = (screenHeight - GUI_HEIGHT) / 2;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        // Scale animation
        if (openAnimation < 1) {
            float scale = 0.92f + 0.08f * easeOutCubic(openAnimation);
            GlStateManager.translate(screenWidth / 2f, screenHeight / 2f, 0);
            GlStateManager.scale(scale, scale, 1);
            GlStateManager.translate(-screenWidth / 2f, -screenHeight / 2f, 0);
        }

        drawMainContainer(guiX, guiY, mouseX, mouseY);

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawMainContainer(int x, int y, int mouseX, int mouseY) {
        // Draw ONE unified rounded background for the entire GUI
        RenderUtils.drawRoundedRect(x, y, GUI_WIDTH, GUI_HEIGHT, CORNER_RADIUS, 0xF0141418);

        // Draw sidebar darkening (slightly darker) - but preserve rounded corners on
        // left
        // Only darken the inner part, not the corners
        // Draw sidebar darkening using unified shape (no seams)
        RenderUtils.drawLeftRoundedRect(x, y, SIDEBAR_WIDTH, GUI_HEIGHT, CORNER_RADIUS, 0x20000000);

        // Animated wrapping gradient border
        // Use a slightly thicker line (1.5f) for better visibility
        RenderUtils.drawAnimatedGradientBorder(x, y, GUI_WIDTH, GUI_HEIGHT, CORNER_RADIUS, 1.5f);

        // Sidebar content
        drawSidebar(x, y, mouseX, mouseY);

        // Vertical divider
        RenderUtils.drawRect(x + SIDEBAR_WIDTH, y + 8, 1, GUI_HEIGHT - 16, 0x25ffffff);

        // Module list
        drawModuleList(x + SIDEBAR_WIDTH + 1, y + HEADER_HEIGHT, mouseX, mouseY);
    }

    private void drawSidebarCorners(int x, int y, double radius) {
        int color = 0x20000000;

        // Fill the vertical gap between the corners (Middle-Left Strip)
        RenderUtils.drawRect(x, y + radius, radius, GUI_HEIGHT - 2 * radius, color);

        // Prepare for arcs
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        // Draw only the needed corner sectors (Left side only)
        GlStateManager.color(red, green, blue, alpha);
        RenderUtils.drawFilledArc(x + radius, y + radius, radius, 180, 270); // Top-Left

        GlStateManager.color(red, green, blue, alpha);
        RenderUtils.drawFilledArc(x + radius, y + GUI_HEIGHT - radius, radius, 90, 180); // Bottom-Left
    }

    private void drawSidebar(int x, int y, int mouseX, int mouseY) {

        // Animated gradient brand
        String line1 = "\u00A7lHousing";
        String line2 = "\u00A7lClient";
        // String verText = "v1.0.0"; // Removed for cleaner look

        int line1W = fontRendererObj.getStringWidth(line1);
        int line2W = fontRendererObj.getStringWidth(line2);

        int totalH = fontRendererObj.FONT_HEIGHT * 2 + 2;
        int startY = y + (HEADER_HEIGHT - totalH) / 2;

        int line1X = x + (SIDEBAR_WIDTH - line1W) / 2;
        int line2X = x + (SIDEBAR_WIDTH - line2W) / 2;

        RenderUtils.drawGlowingText(fontRendererObj, line1, line1X, startY);
        RenderUtils.drawGlowingText(fontRendererObj, line2, line2X, startY + fontRendererObj.FONT_HEIGHT + 2);

        // Category list
        int catY = y + HEADER_HEIGHT;
        for (int i = 0; i < categories.size(); i++) {
            CategoryEntry entry = categories.get(i);
            boolean isSelected = (entry.category == null && searchFocused) ||
                    (entry.category != null && entry.category == selectedCategory && !searchFocused);
            boolean isHovered = mouseX >= x + 8 && mouseX < x + SIDEBAR_WIDTH - 8 &&
                    mouseY >= catY && mouseY < catY + 26;

            // Selection background with rounded corners
            if (isSelected) {
                RenderUtils.drawRoundedRect(x + 8, catY, SIDEBAR_WIDTH - 16, 26, 6, 0xFF1e3a2f);
                // Left accent removed as requested
            } else if (isHovered) {
                RenderUtils.drawRoundedRect(x + 8, catY, SIDEBAR_WIDTH - 16, 26, 6, 0x30ffffff);
            }

            // Simple dot icon
            int iconColor = isSelected ? Theme.ACCENT : 0xFF666666;
            RenderUtils.drawCircle(x + 22, catY + 13, 3, iconColor);

            // Category name
            // Category name (White default, Vivid Green selected)
            int textColor = isSelected ? 0xFF00E676 : 0xFFFFFFFF;
            fontRendererObj.drawStringWithShadow(entry.name, x + 32, catY + 9, textColor);

            catY += 28;
        }
    }

    private void drawModuleList(int x, int y, int mouseX, int mouseY) {
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 1;
        int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;

        // Always show search bar at the top header area
        String searchDisplay;
        if (searchFocused) {
            // When focused: show text + cursor, or just cursor if empty
            if (searchText.isEmpty()) {
                searchDisplay = (System.currentTimeMillis() % 1000 < 500) ? "_" : "";
            } else {
                searchDisplay = searchText + ((System.currentTimeMillis() % 1000 < 500) ? "_" : "");
            }
        } else {
            // When not focused: show text or placeholder
            searchDisplay = searchText.isEmpty() ? "Type to search..." : searchText;
        }
        int searchY = y - 32;
        int searchTextColor = searchFocused ? 0xFFFFFFFF : (searchText.isEmpty() ? 0xFF555555 : 0xFFAAAAAA);

        // No background, just text
        fontRendererObj.drawStringWithShadow(searchDisplay, x + 12, searchY, searchTextColor);

        List<Module> modules = getDisplayModules();

        // Scissor for proper clipping (with padding)
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(x, y - 5, contentWidth, contentHeight + 5);

        int moduleY = (int) (y - scrollOffset + 5);

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (moduleY + getModuleHeight(module) < y - 60 || moduleY > y + contentHeight + 60) {
                moduleY += getModuleHeight(module) + 8;
                continue;
            }

            drawModule(x + 8, moduleY, contentWidth - 24, module, mouseX, mouseY);
            moduleY += getModuleHeight(module) + 8;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Fade edges removed as requested

        // Scrollbar
        int totalHeight = calculateTotalHeight(modules);
        if (totalHeight > contentHeight) {
            drawScrollbar(x + contentWidth - 6, y + 10, 3, contentHeight - 20, totalHeight);
        }
    }

    private void drawModule(int x, int y, int width, Module module, int mouseX, int mouseY) {
        boolean isExpanded = expandedModule == module;
        boolean isBinding = bindingModule == module;
        boolean isHovered = mouseX >= x && mouseX < x + width &&
                mouseY >= y && mouseY < y + MODULE_HEIGHT;

        int totalHeight = getModuleHeight(module);

        // Background with rounded corners
        int bgColor = isExpanded ? 0xFF1a1a20 : (isHovered ? 0xFF1c1c22 : 0xFF151515);
        if (bgColor != 0) {
            RenderUtils.drawRoundedRect(x, y, width, totalHeight, 8, bgColor);
        }

        // Enabled indicator bar (Curved right edges / Straight left)
        if (module.isEnabled()) {
            int barX = x + 3;
            int barY = y + 6;
            int barW = 3;
            int barH = MODULE_HEIGHT - 12;
            int color = Theme.ACCENT;

            // Simple rectangular bar
            RenderUtils.drawRect(barX, barY, barW, barH, color);
        }

        // Module name
        String displayName = module.getName();
        if (module.isBlatant() && !searchFocused) {
            displayName += " \u00A7c(Blatant)";
        }

        int nameColor = module.isEnabled() ? 0xFFFFFFFF
                : 0xFFbbbbbb;
        fontRendererObj.drawStringWithShadow(displayName, x + 12, y + 8, nameColor);

        // Category tag (Search Mode)
        if (searchFocused) {
            String tag = "";
            if (module.isBlatant()) {
                tag += "\u00A7c(Blatant) ";
            }
            tag += "\u00A77(" + module.getCategory().getDisplayName() + ")";

            int nameW = fontRendererObj.getStringWidth(module.getName());
            fontRendererObj.drawString(tag, x + 16 + nameW, y + 8, 0xFF444444);
        }

        // Keybind
        String keybindText = isBinding ? "[...]"
                : (module.getKeybind() != 0 ? "[" + getKeyName(module.getKeybind()) + "]" : "[None]");
        if (!keybindText.isEmpty()) {
            int kw = fontRendererObj.getStringWidth(keybindText);
            fontRendererObj.drawString(keybindText, x + width - kw - 8, y + 8,
                    isBinding ? Theme.ACCENT : 0xFF555555);
        }

        // Description
        String desc = module
                .getDescription();
        if (desc.length() > 50)
            desc = desc.substring(0, 47) + "...";
        fontRendererObj.drawString(desc, x + 12, y + 22, 0xFF555555);

        // Settings
        if (isExpanded && !module.getSettings().isEmpty()) {
            int settingY = y + MODULE_HEIGHT + 4;
            for (Setting<?> setting : module.getSettings()) {
                if (!setting.isVisible())
                    continue;
                drawSetting(x + 16, settingY, width - 32, setting, mouseX, mouseY);
                settingY += SETTING_HEIGHT + 3;
            }
        }
    }

    private String getKeyName(int keyCode) {
        if (keyCode < 0) {
            int btn = keyCode + 100;
            return btn <= 2 ? "M" + (btn + 1) : "M" + btn;
        }
        return Keyboard.getKeyName(keyCode);
    }

    private void drawSetting(int x, int y, int width, Setting<?> setting, int mouseX, int mouseY) {
        String name = setting.getName();

        if (setting instanceof BooleanSetting) {
            BooleanSetting bool = (BooleanSetting) setting;

            // Checkbox (simple rounded square)
            int boxX = x + width - 16;
            int boxColor = bool.isEnabled() ? Theme.ACCENT : 0xFF333340;
            RenderUtils.drawRoundedRect(boxX, y + 1, 14, 14, 3, boxColor);

            // Checkmark (simple)
            if (bool.isEnabled()) {
                fontRendererObj.drawString("\u2713", boxX + 3, y + 4, 0xFF000000);
            }

            fontRendererObj.drawString(name, x, y + 4, 0xFFFFFFFF);

        } else if (setting instanceof ModeSetting) {
            ModeSetting mode = (ModeSetting) setting;
            fontRendererObj.drawString(name + ":", x, y + 4, 0xFF888888);
            int nw = fontRendererObj.getStringWidth(name + ": ");
            fontRendererObj.drawString(mode.getValue(), x + nw, y + 4, 0xFFFFFFFF);

            // Arrows
            fontRendererObj.drawString("<  >", x + width - 24, y + 4, 0xFF666666);

        } else if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting) setting;

            fontRendererObj.drawString(name + ":", x, y + 4, 0xFF888888);
            int nw = fontRendererObj.getStringWidth(name + ": ");

            if (editingSetting == num) {
                // Show editing value with blinking cursor and underline
                boolean showCursor = System.currentTimeMillis() % 1000 < 500;
                String display = editingValue + (showCursor ? "|" : "");
                int valWidth = fontRendererObj.getStringWidth(editingValue.isEmpty() ? "0" : editingValue);

                // Draw underline
                RenderUtils.drawRect(x + nw, y + 13, valWidth + 2, 1, Theme.ACCENT);
                fontRendererObj.drawString(display, x + nw, y + 4, Theme.ACCENT);
            } else {
                String val = num.isOnlyInt() ? String.valueOf(num.getIntValue())
                        : String.format("%.1f", num.getValue());
                fontRendererObj.drawString(val, x + nw, y + 4, 0xFFFFFFFF);
            }

            // Mini slider (always visible)
            int sliderX = x + width - 150;
            RenderUtils.drawRoundedRect(sliderX, y + 6, 140, 4, 2, 0xFF333340);
            double progress = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
            RenderUtils.drawRoundedRect(sliderX, y + 6, (int) (140 * progress), 4, 2, Theme.ACCENT);
        }
    }

    private int getModuleHeight(Module module) {
        if (expandedModule == module && !module.getSettings().isEmpty()) {
            int count = 0;
            for (Setting<?> s : module.getSettings()) {
                if (s.isVisible())
                    count++;
            }
            return MODULE_HEIGHT + 8 + (count * (SETTING_HEIGHT + 3));
        }
        return MODULE_HEIGHT;
    }

    private int calculateTotalHeight(List<Module> modules) {
        int total = 10;
        for (Module m : modules) {
            total += getModuleHeight(m) + 8;
        }
        return total;
    }

    private void drawScrollbar(int x, int y, int width, int viewHeight, int contentHeight) {
        RenderUtils.drawRoundedRect(x, y, width, viewHeight, 2, 0xFF222228);

        float ratio = (float) viewHeight / contentHeight;
        int thumbHeight = Math.max(20, (int) (viewHeight * ratio));
        int maxScroll = contentHeight - viewHeight;
        float scrollProgress = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int thumbY = y + (int) ((viewHeight - thumbHeight) * scrollProgress);

        RenderUtils.drawRoundedRect(x, thumbY, width, thumbHeight, 2, Theme.ACCENT);
    }

    private List<Module> getDisplayModules() {
        List<Module> result = new ArrayList<>();

        List<Module> source;
        if (searchFocused) {
            String query = searchText.toLowerCase();
            source = new ArrayList<>();
            for (Module m : HousingClient.getInstance().getModuleManager().getModules()) {
                if (searchText.isEmpty() ||
                        m.getName().toLowerCase().contains(query) ||
                        m.getDescription().toLowerCase().contains(query)) {
                    source.add(m);
                }
            }
        } else if (selectedCategory != null) {
            source = new ArrayList<>(
                    HousingClient.getInstance().getModuleManager().getModulesByCategory(selectedCategory));
        } else {
            source = new ArrayList<>();
        }

        // Sort ALL categories alphabetically
        source.sort(Comparator.comparing(Module::getName));

        // Filter out hidden modules (HUD is internal)
        for (Module m : source) {
            String name = m.getName().toLowerCase();
            if (name.equals("hud")) {
                continue;
            }
            // Skip excluded categories in search
            if (searchFocused && isExcludedCategory(m.getCategory())) {
                continue;
            }
            result.add(m);
        }

        return result;
    }

    private boolean isExcludedCategory(Category cat) {
        return cat == Category.MISC || cat == Category.ITEMS;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        ScaledResolution sr = new ScaledResolution(mc);
        int guiX = (sr.getScaledWidth() - GUI_WIDTH) / 2;
        int guiY = (sr.getScaledHeight() - GUI_HEIGHT) / 2;

        // Keybind binding
        if (bindingModule != null) {
            if (mouseButton == 0) {
                bindingModule.setKeybind(0);
            } else {
                bindingModule.setKeybind(-(mouseButton + 100));
            }
            bindingModule = null;
            return;
        }

        // Search bar click detection (always visible at top of content area)
        int contentX = guiX + SIDEBAR_WIDTH + 1;
        int searchBarY = guiY + HEADER_HEIGHT - 36;
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 1;
        if (mouseX >= contentX + 8 && mouseX < contentX + contentWidth - 8 &&
                mouseY >= searchBarY && mouseY < searchBarY + 18) {
            searchFocused = true;
            selectedCategory = null;
            scrollOffset = 0;
            targetScroll = 0;
            expandedModule = null;
            return;
        }

        // Sidebar clicks
        int catY = guiY + HEADER_HEIGHT;
        for (CategoryEntry entry : categories) {
            if (mouseX >= guiX + 8 && mouseX < guiX + SIDEBAR_WIDTH - 8 &&
                    mouseY >= catY && mouseY < catY + 26) {
                if (entry.category == null) {
                    searchFocused = true;
                    selectedCategory = null;
                } else {
                    searchFocused = false;
                    searchText = "";
                    selectedCategory = entry.category;
                }
                scrollOffset = 0;
                targetScroll = 0;
                expandedModule = null;
                return;
            }
            catY += 28;
        }

        // Module clicks
        int moduleContentX = guiX + SIDEBAR_WIDTH + 9;
        int contentY = guiY + HEADER_HEIGHT;
        int moduleContentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;

        List<Module> modules = getDisplayModules();
        int moduleY = (int) (contentY - scrollOffset + 5);

        for (Module module : modules) {
            int height = getModuleHeight(module);

            if (mouseX >= moduleContentX && mouseX < moduleContentX + moduleContentWidth &&
                    mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT) {

                // Keybind area
                if (mouseX > moduleContentX + moduleContentWidth - 60) {
                    bindingModule = module;
                    return;
                }

                if (mouseButton == 0) {
                    module.toggle();
                } else if (mouseButton == 1) {
                    expandedModule = (expandedModule == module) ? null : module;
                }
                return;
            }

            // Settings
            if (expandedModule == module) {
                int settingY = moduleY + MODULE_HEIGHT + 4;
                for (Setting<?> setting : module.getSettings()) {
                    if (!setting.isVisible())
                        continue;
                    if (mouseY >= settingY && mouseY < settingY + SETTING_HEIGHT) {
                        handleSettingClick(setting, mouseButton, mouseX, moduleContentX + 16, moduleContentWidth - 32);
                        return;
                    }
                    settingY += SETTING_HEIGHT + 3;
                }
            }

            moduleY += height + 8;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handleSettingClick(Setting<?> setting, int button, int mouseX, int x, int width) {
        if (setting instanceof BooleanSetting) {
            ((BooleanSetting) setting).toggle();
        } else if (setting instanceof ModeSetting) {
            ((ModeSetting) setting).cycle();
        } else if (setting instanceof NumberSetting) {
            NumberSetting num = (NumberSetting) setting;

            int sliderX = x + width - 150;

            if (button == 0) { // Left click
                if (mouseX >= sliderX) {
                    // Click on slider - start dragging
                    draggingSetting = num;
                    draggingSliderX = sliderX;
                    double progress = Math.max(0, Math.min(1, (mouseX - sliderX) / 140.0));
                    num.setValue(num.getMin() + (num.getMax() - num.getMin()) * progress);
                } else {
                    // Click on value text area - toggle editing
                    if (editingSetting == num) {
                        // Apply and exit editing
                        try {
                            double val = Double.parseDouble(editingValue);
                            num.setValueUnclamped(val);
                        } catch (NumberFormatException e) {
                        }
                        editingSetting = null;
                        editingValue = "";
                    } else {
                        // Start editing
                        editingSetting = num;
                        editingValue = "";
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (editingSetting != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                try {
                    double val = Double.parseDouble(editingValue);
                    editingSetting.setValueUnclamped(val);
                } catch (NumberFormatException e) {
                    // Ignore invalid input
                }
                editingSetting = null;
            } else if (keyCode == Keyboard.KEY_ESCAPE) {
                editingSetting = null;
            } else if (keyCode == Keyboard.KEY_BACK) {
                if (!editingValue.isEmpty()) {
                    editingValue = editingValue.substring(0, editingValue.length() - 1);
                }
            } else if (Character.isDigit(typedChar) || typedChar == '.' || typedChar == '-') {
                editingValue += typedChar;
            }
            return;
        }

        if (bindingModule != null) {
            bindingModule.setKeybind(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            bindingModule = null;
            return;
        }

        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        if (searchFocused) {
            if (keyCode == Keyboard.KEY_BACK && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            } else if (Character.isLetterOrDigit(typedChar) || typedChar == ' ') {
                searchText += typedChar;
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {

            // Check if hovering a NumberSetting to scroll value
            ScaledResolution sr = new ScaledResolution(mc);
            int guiX = (sr.getScaledWidth() - GUI_WIDTH) / 2;
            int guiY = (sr.getScaledHeight() - GUI_HEIGHT) / 2;
            int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
            int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

            int moduleContentX = guiX + SIDEBAR_WIDTH + 9;
            int moduleContentWidth = GUI_WIDTH - SIDEBAR_WIDTH - 25;
            int moduleY = (int) (guiY + HEADER_HEIGHT - scrollOffset + 5);

            for (Module module : getDisplayModules()) {
                int height = getModuleHeight(module);

                if (expandedModule == module) {
                    int settingY = moduleY + MODULE_HEIGHT + 4;
                    for (Setting<?> setting : module.getSettings()) {
                        if (!setting.isVisible())
                            continue;

                        if (mouseX >= moduleContentX && mouseX < moduleContentX + moduleContentWidth &&
                                mouseY >= settingY && mouseY < settingY + SETTING_HEIGHT) {

                            if (setting instanceof NumberSetting) {
                                NumberSetting num = (NumberSetting) setting;
                                double change = (scroll > 0 ? 1 : -1) * num.getIncrement();
                                num.setValue(num.getValue() + change);
                                return; // Consumed scroll
                            }
                        }
                        settingY += SETTING_HEIGHT + 3;
                    }
                }
                moduleY += height + 8;
            }

            // Normal List Scrolling
            int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;
            int totalHeight = calculateTotalHeight(getDisplayModules());
            int maxScroll = Math.max(0, totalHeight - contentHeight + 20);

            // Adjust target scroll
            targetScroll -= scroll / 2.0f;
            targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    public Theme getTheme() {
        return theme;
    }

    public void refreshPanels() {
        initCategories();
    }

    private static class CategoryEntry {
        final Category category;
        final String name;

        CategoryEntry(Category category, String name) {
            this.category = category;
            this.name = name;
        }
    }
}
