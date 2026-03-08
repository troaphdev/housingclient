package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.settings.*;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;

import net.minecraft.util.MathHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Modern card-based ClickGUI for HousingClient Public
 * - Tinted Glass Theme (Blur + Transparency)
 * - Lunar-style Settings Page (Full view, specific widgets)
 */
public class ClickGUI extends GuiScreen {

    // GUI Dimensions
    private static final int GUI_WIDTH = 620;
    private static final int GUI_HEIGHT = 410;
    private static final int SIDEBAR_WIDTH = 140;
    private static final int HEADER_HEIGHT = 50;
    private static final int CORNER_RADIUS = 12;

    // Card dimensions
    private static final int CARD_WIDTH = 140;
    private static final int CARD_HEIGHT = 125;
    private static final int CARD_GAP = 12;
    private static final int CARDS_PER_ROW = 3;

    // Colors - Modern 2025 Dark Theme
    private static final int BG_COLOR = 0xF2141414; // ~95% opacity, soft dark
    private static final int BORDER_COLOR = 0xFF333333; // Subtle lighter border
    private static final int SIDEBAR_BG = 0xFF1A1A1A; // Solid dark column
    private static final int CARD_BG = 0xFF1E1E1E; // Slightly lighter than background
    private static final int ACCENT_GREEN = 0xFF2ECC71;
    private static final int ACCENT_RED = 0xFFE74C3C;
    private static final int SEPARATOR_COLOR = 0xFF333333;
    private static final int TEXT_WHITE = 0xFFF0F0F0;
    private static final int TEXT_GRAY = 0xFFAAAAAA;
    private static final int OPTIONS_BAR_BG = 0xFF252525; // Brighter than card BG for options

    // Lunar Style Colors
    private static final int TOGGLE_ON_BG = 0xFF2ECC71; // Green
    private static final int TOGGLE_OFF_BG = 0xFFE74C3C; // Red
    private static final int SLIDER_ACCENT = 0xFF3498DB; // Blue
    private static final int SLIDER_BG = 0xFF2F2F2F;

    // Icons
    // private static final ResourceLocation LOGO = new
    // ResourceLocation("housingclient", "textures/gui/logo2.png"); // Removed
    private static final ResourceLocation ICON_ALL = new ResourceLocation("housingclient",
            "textures/gui/magnifyingglassgray.png");
    private static final ResourceLocation ICON_VISUAL = new ResourceLocation("housingclient",
            "textures/gui/visual.png");
    private static final ResourceLocation ICON_MODERATION = new ResourceLocation("housingclient",
            "textures/gui/moderation.png");
    private static final ResourceLocation ICON_ANTIEXPLOIT = new ResourceLocation("housingclient",
            "textures/gui/antiexploit.png");
    private static final ResourceLocation ICON_QOL = new ResourceLocation("housingclient", "textures/gui/qol.png");
    private static final ResourceLocation ICON_MISC = new ResourceLocation("housingclient", "textures/icons/misc.png");
    private static final ResourceLocation ICON_SETTINGS = new ResourceLocation("housingclient",
            "textures/gui/settings.png");
    private static final ResourceLocation ICON_CLIENT = new ResourceLocation("housingclient",
            "textures/icons/blocks.png");
    private static final ResourceLocation ICON_BACK = new ResourceLocation("housingclient",
            "textures/gui/backarrow1.png");
    private static final ResourceLocation ICON_INFO = new ResourceLocation("housingclient",
            "textures/gui/moduleinfo.png");
    private static final ResourceLocation ICON_INFO_BRIGHT = new ResourceLocation("housingclient",
            "textures/gui/moduleinfo_bright.png");
    private static final int INFO_ICON_SIZE = 12;

    // State
    private boolean modsTab = true;
    private String selectedCategory = "ALL";
    private float scrollOffset = 0;
    private float targetScroll = 0;

    // Settings state
    private Module settingsModule = null;
    private long lastSettingsOpenTime = 0;
    private Module bindingModule = null;
    private float settingsScroll = 0;
    private float targetSettingsScroll = 0;
    private Module hoveredInfoModule = null; // For tooltip tracking
    private int tooltipX = 0, tooltipY = 0;
    private GuiTextField searchField;

    // Categories
    private static final String[][] CATEGORIES = {
            { "ALL", "all" },
            { "VISUAL", "visual" },
            { "MODERATION", "moderation" },
            { "EXPLOIT", "exploits" },
            { "QOL", "qol" },
            { "MISCELLANEOUS", "miscellaneous" },
            { "CLIENT", "client" }
    };

    public ClickGUI() {
    }

    @Override
    public void initGui() {
        // Only load blur shader if setting is enabled
        com.housingclient.module.modules.client.ClickGUIModule clickGui = HousingClient.getInstance().getModuleManager()
                .getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
        boolean blurEnabled = clickGui != null && clickGui.isBlurEnabled();

        if (blurEnabled && OpenGlHelper.shadersSupported && mc.getRenderViewEntity() instanceof EntityPlayer) {
            if (mc.entityRenderer.getShaderGroup() != null) {
                mc.entityRenderer.getShaderGroup().deleteShaderGroup();
            }
            try {
                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Only initialize state if not already set (preserve state on resize/re-open)
        if (searchField == null) {
            reset();
        } else {
            // Just re-init the search field position/size without clearing text
            String oldText = searchField.getText();
            searchField = new GuiTextField(0, fontRendererObj, 0, 0, 140, 22);
            searchField.setFocused(true);
            searchField.setCanLoseFocus(false);
            searchField.setText(oldText);
        }
    }

    /**
     * Resets the GUI state (scroll, settings, search)
     * Call this when opening the GUI fresh (e.g. from keybind)
     */
    public void reset() {
        scrollOffset = 0;
        targetScroll = 0;
        settingsModule = null;
        settingsScroll = 0;
        targetSettingsScroll = 0;
        selectedCategory = "ALL";
        modsTab = true;

        searchField = new GuiTextField(0, fontRendererObj, 0, 0, 140, 22);
        searchField.setFocused(true);
        searchField.setCanLoseFocus(false);
        searchField.setText("");
    }

    @Override
    public void updateScreen() {
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        if (mc.entityRenderer.getShaderGroup() != null) {
            mc.entityRenderer.stopUseShader();
        }
        HousingClient.getInstance().saveAll();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Force Scale Factor 2.0 (Normal) logic
        int scaleFactor = new ScaledResolution(mc).getScaleFactor();
        float drawScale = 2.0f / scaleFactor;
        float mouseScale = (float) scaleFactor / 2.0f;

        // Scale the mouse coordinates for hit testing
        int scaledMouseX = (int) (mouseX * mouseScale);
        int scaledMouseY = (int) (mouseY * mouseScale);

        // Smooth scrolling
        scrollOffset += (targetScroll - scrollOffset) * 0.15f;
        if (Math.abs(scrollOffset - targetScroll) < 0.5f)
            scrollOffset = targetScroll;

        settingsScroll += (targetSettingsScroll - settingsScroll) * 0.15f;
        if (Math.abs(settingsScroll - targetSettingsScroll) < 0.5f)
            settingsScroll = targetSettingsScroll;

        // NOTE: We used to get the scaled resolution here, but since we are forcing
        // scale, we need to calculate centering based on the "virtual" resolution
        // (Scale 2)
        int virtualWidth = mc.displayWidth / 2;
        int virtualHeight = mc.displayHeight / 2;

        int guiX = (virtualWidth - GUI_WIDTH) / 2;
        int guiY = (virtualHeight - GUI_HEIGHT) / 2;

        GlStateManager.pushMatrix();
        // Apply the visual scaling
        GlStateManager.scale(drawScale, drawScale, 1.0f);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        // Main background & Border
        RenderUtils.drawRoundedRect(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, CORNER_RADIUS, BG_COLOR);
        RenderUtils.drawRoundedRectOutline(guiX, guiY, GUI_WIDTH, GUI_HEIGHT, CORNER_RADIUS, BORDER_COLOR, 2.0f);

        if (settingsModule == null) {
            // --- MAIN VIEW ---

            // Sidebar BG
            RenderUtils.drawLeftRoundedRect(guiX, guiY, SIDEBAR_WIDTH, GUI_HEIGHT, CORNER_RADIUS, SIDEBAR_BG);
            RenderUtils.drawRect(guiX + SIDEBAR_WIDTH, guiY, 1, GUI_HEIGHT, BORDER_COLOR);

            drawHeader(guiX, guiY, scaledMouseX, scaledMouseY);
            drawSidebar(guiX, guiY + HEADER_HEIGHT, scaledMouseX, scaledMouseY);

            if (modsTab) {
                drawModuleCards(guiX + SIDEBAR_WIDTH, guiY + HEADER_HEIGHT, scaledMouseX, scaledMouseY);
            } else {
                drawClientSettings(guiX + SIDEBAR_WIDTH, guiY + HEADER_HEIGHT, scaledMouseX, scaledMouseY);
            }
        } else {
            // --- SETTINGS VIEW (Overhaul) ---
            drawSettingsPage(guiX, guiY, scaledMouseX, scaledMouseY);
        }

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();

        // We do NOT call super.drawScreen because it would draw tooltips/overlay
        // without our scaling
        // If we want tooltips, we should handle them manually or wrap super in scaling
        // too
        // But super.drawScreen in standard GuiScreen just draws buttons and labels
        // which we don't have (except raw valid ones)
        // Checks: We have buttonList empty (custom handling).
        // Exceptions: existing code called super at end.
        // Let's call super with scaled mouse, wrapped in matrix, just in case.
        GlStateManager.pushMatrix();
        GlStateManager.scale(drawScale, drawScale, 1.0f);
        super.drawScreen(scaledMouseX, scaledMouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        int scaleFactor = new ScaledResolution(mc).getScaleFactor();
        float mouseScale = (float) scaleFactor / 2.0f;
        super.mouseReleased((int) (mouseX * mouseScale), (int) (mouseY * mouseScale), state);
    }

    private void drawHeader(int x, int y, int mouseX, int mouseY) {
        // Logo
        ResourceLocation logo = new ResourceLocation("housingclient", "textures/gui/logov5.png");
        int logoSize = 24;
        int logoY = y + (HEADER_HEIGHT - logoSize) / 2;
        drawSmoothTexture(logo, x + 12, logoY, logoSize, logoSize);

        // Title - HOUSING uses Logo Color from settings
        String title1 = "\u00A7lHOUSING";
        String title2 = "Client";
        int titleX = x + 12 + logoSize + 6; // x + margin + logo + gap

        // Get logo color from ClickGUIModule settings
        com.housingclient.module.modules.client.ClickGUIModule clickGuiModule = HousingClient.getInstance()
                .getModuleManager().getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
        int logoColor = (clickGuiModule != null) ? clickGuiModule.getAccentColor().getRGB() : ACCENT_RED;

        fontRendererObj.drawStringWithShadow(title1, titleX, y + 14, logoColor);
        fontRendererObj.drawStringWithShadow(title2, titleX + fontRendererObj.getStringWidth(title1) + 2, y + 14,
                TEXT_WHITE);
        fontRendererObj.drawString("v" + HousingClient.VERSION, titleX, y + 26, TEXT_GRAY);

        // --- SEARCH BAR (Right Aligned) ---
        int searchW = 105; // Balanced
        int searchX = x + GUI_WIDTH - searchW - 24;
        int searchY = y + (HEADER_HEIGHT - 18) / 2;

        // Tabs
        int tabWidth = 105;
        int tabHeight = 22;
        int tabY = y + (HEADER_HEIGHT - tabHeight) / 2;
        int tabGap = 8;

        // Calculate total width of center elements: [MODS] [SETTINGS] [HUD DESIGNER]
        int totalTabsWidth = (tabWidth * 3) + (tabGap * 2);

        // Center Tabs between Sidebar and Search Bar
        int sidebarEndX = x + SIDEBAR_WIDTH;
        int availableSpace = searchX - sidebarEndX;

        // Ensure we center properly
        int tabStartX = sidebarEndX + (availableSpace - totalTabsWidth) / 2;

        // 1. MODS TAB
        boolean modsHover = mouseX >= tabStartX && mouseX < tabStartX + tabWidth && mouseY >= tabY
                && mouseY < tabY + tabHeight;
        if (modsTab)
            RenderUtils.drawRoundedRect(tabStartX, tabY, tabWidth, tabHeight, 5, 0xFF252525);
        else if (modsHover)
            RenderUtils.drawRoundedRect(tabStartX, tabY, tabWidth, tabHeight, 5, 0x40FFFFFF);
        RenderUtils.drawRoundedRectOutline(tabStartX, tabY, tabWidth, tabHeight, 5, BORDER_COLOR, 2.0f);
        drawCenteredString(fontRendererObj, "MODULES", tabStartX + tabWidth / 2, tabY + 6,
                modsTab ? TEXT_WHITE : TEXT_GRAY);

        // 2. SETTINGS TAB
        int settingsX = tabStartX + tabWidth + tabGap;
        boolean settingsHover = mouseX >= settingsX && mouseX < settingsX + tabWidth && mouseY >= tabY
                && mouseY < tabY + tabHeight;
        if (!modsTab)
            RenderUtils.drawRoundedRect(settingsX, tabY, tabWidth, tabHeight, 5, 0xFF252525);
        else if (settingsHover)
            RenderUtils.drawRoundedRect(settingsX, tabY, tabWidth, tabHeight, 5, 0x40FFFFFF);
        RenderUtils.drawRoundedRectOutline(settingsX, tabY, tabWidth, tabHeight, 5, BORDER_COLOR, 2.0f);
        drawCenteredString(fontRendererObj, "SETTINGS", settingsX + tabWidth / 2, tabY + 6,
                !modsTab ? TEXT_WHITE : TEXT_GRAY);

        // 3. HUD EDITOR TAB
        int hudX = settingsX + tabWidth + tabGap;
        boolean hudHover = mouseX >= hudX && mouseX < hudX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight;
        if (hudHover)
            RenderUtils.drawRoundedRect(hudX, tabY, tabWidth, tabHeight, 5, 0x40FFFFFF);
        RenderUtils.drawRoundedRectOutline(hudX, tabY, tabWidth, tabHeight, 5, BORDER_COLOR, 2.0f);
        drawCenteredString(fontRendererObj, "HUD DESIGNER", hudX + tabWidth / 2, tabY + 6, TEXT_GRAY);

        if (searchField != null) {
            searchField.xPosition = searchX + 4;
            searchField.yPosition = searchY + 5;
            searchField.width = searchW - 8;
            searchField.height = 18;

            RenderUtils.drawRoundedRect(searchX, searchY, searchW, 18, 5, 0xFF1A1A1A);
            RenderUtils.drawRoundedRectOutline(searchX, searchY, searchW, 18, 5, BORDER_COLOR, 2.0f);

            searchField.setEnableBackgroundDrawing(false);
            searchField.drawTextBox();

            if (searchField.getText().isEmpty() && !searchField.isFocused()) {
                fontRendererObj.drawString("Search...", searchX + 4, searchY + 5, TEXT_GRAY);
            }
        }
    }

    private void drawSidebar(int x, int y, int mouseX, int mouseY) {
        int catY = y + 12;
        for (String[] category : CATEGORIES) {
            String name = category[0];
            String iconName = category[1];
            boolean selected = name.equals(selectedCategory);
            boolean hovered = mouseX >= x + 8 && mouseX < x + SIDEBAR_WIDTH - 8 && mouseY >= catY && mouseY < catY + 28;

            if (selected)
                RenderUtils.drawRoundedRect(x + 8, catY, SIDEBAR_WIDTH - 16, 28, 5, 0xFF2A2A2A);
            else if (hovered)
                RenderUtils.drawRoundedRect(x + 8, catY, SIDEBAR_WIDTH - 16, 28, 5, 0x20FFFFFF);

            ResourceLocation icon = getIconForCategory(iconName);
            drawSmoothTexture(icon, x + 15, catY + 4, 20, 20);
            fontRendererObj.drawStringWithShadow(name, x + 40, catY + 9, selected ? ACCENT_GREEN : TEXT_GRAY);
            catY += 35;
        }
    }

    private void drawModuleCards(int x, int y, int mouseX, int mouseY) {
        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;

        List<Module> modules = getDisplayModules();

        // Reset hovered info module at start of each frame
        hoveredInfoModule = null;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissorCustomScale(x, y, contentWidth, contentHeight, 2.0);

        int cardAreaX = x + 12;
        int cardAreaY = (int) (y + 12 - scrollOffset);
        int col = 0, row = 0;

        for (Module module : modules) {
            int cardX = cardAreaX + col * (CARD_WIDTH + CARD_GAP);
            int cardY = cardAreaY + row * (CARD_HEIGHT + CARD_GAP);

            if (cardY + CARD_HEIGHT >= y - 50 && cardY <= y + contentHeight + 50) {
                drawModuleCard(cardX, cardY, module, mouseX, mouseY);
            }
            col++;
            if (col >= CARDS_PER_ROW) {
                col = 0;
                row++;
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar logic
        int totalRows = (modules.size() + CARDS_PER_ROW - 1) / CARDS_PER_ROW;
        int totalHeight = totalRows * (CARD_HEIGHT + CARD_GAP) + 24;
        if (totalHeight > contentHeight) {
            float ratio = (float) contentHeight / totalHeight;
            int thumbH = Math.max(20, (int) (contentHeight * ratio));
            int scrollRange = totalHeight - contentHeight;
            int trackY = y + 8;
            int trackHeight = contentHeight - 16;
            int thumbY = trackY
                    + (int) ((trackHeight - thumbH) * MathHelper.clamp_float(scrollOffset / scrollRange, 0f, 1f));
            RenderUtils.drawRoundedRect(x + contentWidth - 6, trackY, 3, trackHeight, 2, 0xFF1A1A1A);
            RenderUtils.drawRoundedRect(x + contentWidth - 6, thumbY, 3, thumbH, 2, ACCENT_GREEN);
        }

        // Draw tooltip for hovered info icon (outside scissor so it shows on top)
        // Check if tooltips are enabled in ClickGUI settings
        com.housingclient.module.modules.client.ClickGUIModule clickGuiModule = HousingClient.getInstance()
                .getModuleManager().getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
        if (clickGuiModule == null) {
            Module m = HousingClient.getInstance().getModuleManager().getModule("ClickGUI");
            if (m instanceof com.housingclient.module.modules.client.ClickGUIModule) {
                clickGuiModule = (com.housingclient.module.modules.client.ClickGUIModule) m;
            }
        }
        boolean tooltipsEnabled = clickGuiModule != null && clickGuiModule.isTooltipsEnabled();

        if (tooltipsEnabled && hoveredInfoModule != null && hoveredInfoModule.getDescription() != null
                && !hoveredInfoModule.getDescription().isEmpty()) {
            String desc = hoveredInfoModule.getDescription();
            int tooltipWidth = fontRendererObj.getStringWidth(desc) + 10;
            int tooltipHeight = 14;
            int ttX = tooltipX + 10;
            int ttY = tooltipY - 5;

            // Keep on screen
            ScaledResolution sr = new ScaledResolution(mc);
            if (ttX + tooltipWidth > sr.getScaledWidth() - 5) {
                ttX = tooltipX - tooltipWidth - 10;
            }

            RenderUtils.drawRoundedRect(ttX, ttY, tooltipWidth, tooltipHeight, 4, 0xEE141414);
            fontRendererObj.drawStringWithShadow(desc, ttX + 5, ttY + 3, 0xFFE0E0E0);
        }
    }

    private void drawModuleCard(int x, int y, Module module, int mouseX, int mouseY) {
        boolean enabled = module.isEnabled();
        // Check availability of settings for UI state
        boolean hasSettings = !module.getSettings().isEmpty();

        // Determine Risk / Blatant Status
        boolean isRisk = module.isBlatant()
                || (module.getName().equals("Creative Flight") && !HousingClient.getInstance().isSafeMode());

        // Card BG
        int cardColor = CARD_BG;
        int barColor = OPTIONS_BAR_BG;
        int sepColor = SEPARATOR_COLOR;

        if (isRisk) {
            // Slight dark red tint: 0xFF2B1919 (base 1E1E1E + red shift)
            cardColor = 0xFF2B1919;
            // Apply similar tint to bar and separators for consistency
            barColor = 0xFF351F1F;
            sepColor = 0xFF4A2A2A;
        }
        RenderUtils.drawRoundedRect(x, y, CARD_WIDTH, CARD_HEIGHT, 7, cardColor);

        // Check tooltips setting
        com.housingclient.module.modules.client.ClickGUIModule clickGuiModule = HousingClient.getInstance()
                .getModuleManager().getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
        if (clickGuiModule == null) {
            Module m = HousingClient.getInstance().getModuleManager().getModule("ClickGUI");
            if (m instanceof com.housingclient.module.modules.client.ClickGUIModule) {
                clickGuiModule = (com.housingclient.module.modules.client.ClickGUIModule) m;
            }
        }
        boolean tooltipsEnabled = clickGuiModule != null && clickGuiModule.isTooltipsEnabled();

        if (tooltipsEnabled) {
            // Info Icon (Top Left)
            int infoX = x + 5;
            int infoY = y + 5;
            boolean infoHovered = mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE && mouseY >= infoY
                    && mouseY <= infoY + INFO_ICON_SIZE;
            if (infoHovered) {
                hoveredInfoModule = module;
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
            // Use color with alpha for transparency (0x66 = 40%, 0xFF = 100%)
            int infoColor;

            if (isRisk) {
                // Lighter Red for Blatant modules (High visibility)
                // 0xFFD32F2F (Darker Pomegranate)
                infoColor = infoHovered ? 0xFFD32F2F : 0x80D32F2F;
                // Use the white version of the icon so tinting works perfectly
                drawSmoothTexture(ICON_INFO_BRIGHT, infoX, infoY, INFO_ICON_SIZE, INFO_ICON_SIZE, infoColor);
            } else {
                // White for Normal modules
                infoColor = infoHovered ? 0xFFFFFFFF : 0x66FFFFFF;
                drawSmoothTexture(ICON_INFO, infoX, infoY, INFO_ICON_SIZE, INFO_ICON_SIZE, infoColor);
            }
        }

        // Keybind Button (Top Right)
        String bindText = (bindingModule == module) ? "..."
                : (module.getKeybind() == 0 ? "[None]" : "[" + Keyboard.getKeyName(module.getKeybind()) + "]");
        int bindW = fontRendererObj.getStringWidth(bindText) + 6;
        int bindX = x + CARD_WIDTH - bindW - 5;
        int bindY = y + 5;

        boolean bindHover = mouseX >= bindX && mouseX < bindX + bindW && mouseY >= bindY && mouseY < bindY + 10;
        if (bindHover || bindingModule == module) {
            RenderUtils.drawRoundedRect(bindX, bindY, bindW, 10, 3, 0x40FFFFFF);
        }
        fontRendererObj.drawString(bindText, bindX + 3, bindY + 1, bindHover ? ACCENT_GREEN : TEXT_GRAY);

        // Don't draw outline yet, draw it last so it covers the button edges

        int footerHeight = 18;
        int optionsHeight = 16;
        int stateY = y + CARD_HEIGHT - footerHeight;
        int optionsY = stateY - optionsHeight;

        // Options Bar Background (Brightened or Tinted)
        RenderUtils.drawRect(x, optionsY, CARD_WIDTH, optionsHeight, barColor);

        // Icon & Lines
        // Icon
        int iconX = x + (CARD_WIDTH - 32) / 2;
        int iconY = y + 20; // Moved down slightly to provide space from top

        String iconName = module.getName().toLowerCase().replace(" ", "");
        if (iconName.equals("coords"))
            iconName = "coordinates";
        if (iconName.equals("hidehykiaentities"))
            iconName = "hidehykia";

        // Map Module List to use the HUD icon
        if (iconName.equals("modulelist"))
            iconName = "hud";

        java.util.List<String> validIcons = java.util.Arrays.asList(
                "biome", "clock", "coordinates", "cps", "direction", "fps", "friends", "fullbright", "huddesigner",
                "nametags", "chat", "hideentities", "hitboxes", "hidehykia", "ping", "search", "tps", "weather",
                // New Icons
                "blink", "crasherdetector", "fly", "fastplace", "freecam", "ghostblocks", "invispot",
                "itemstealer", "autoclicker", "loadedplayers", "nbtlogger", "nodebuff", "nuker",
                "playercrasher", "reach", "servermatcher", "speed", "sprint", "storageesp", "tracers", "truesight",
                "esp", "leftautoclicker", "creativeflight", "crashdetector", "zoom",
                // Updated Icons
                "antivoidlag", "chams", "fastbreak", "hud", "scoreboard", "huddesigner", "bypassblacklist", "nofall",
                "activeeffects", "dispenserfill", "imagetonbt", "ghostdisc", "grieferdetector", "fancytext",
                "packetmultiplier");

        if (iconName.equals("creativeflight")) {
            if (HousingClient.getInstance().isSafeMode()) {
                iconName = "bird";
            } else {
                iconName = "fly";
            }
        }

        if (!validIcons.contains(iconName) && !iconName.equals("bird")) {
            iconName = "generic";
        }

        ResourceLocation icon = new ResourceLocation("housingclient", "textures/icons/" + iconName + ".png");

        // Draw icon
        int iconColorMult = 0xFFFFFFFF;
        if (iconName.equals("bird")) {
            // Force white filtering for bird icon to ensure it pops
            iconColorMult = 0xFFFFFFFF;
        }
        drawSmoothTexture(icon, iconX, iconY, 32, 32, iconColorMult);

        // Name
        String name = module.getDisplayName();
        if (name.length() > 25) // Increased limit from 16 to 25
            name = name.substring(0, 23) + "..";

        // Text Positioning
        drawCenteredString(fontRendererObj, name, x + CARD_WIDTH / 2, y + 68, 0xFFFFFFFF);

        // Separator 1 (Top of options)
        RenderUtils.drawRect(x, optionsY - 1, CARD_WIDTH, 1, sepColor);

        // Options Row
        int gearX = x + CARD_WIDTH - 26;
        RenderUtils.drawRect(gearX, optionsY, 1, optionsHeight, sepColor);

        // Options Text Area
        boolean optHover = hasSettings && mouseX >= x && mouseX < gearX && mouseY >= optionsY
                && mouseY < optionsY + optionsHeight;
        if (optHover)
            RenderUtils.drawRect(x, optionsY, gearX - x, optionsHeight, 0x10FFFFFF);
        // Centered (+4 instead of +5) and Grayed check
        drawCenteredString(fontRendererObj, "OPTIONS", x + (gearX - x) / 2, optionsY + 4,
                hasSettings ? 0xFFE0E0E0 : 0xFF555555);

        // Gear Area
        boolean gearHover = hasSettings && mouseX >= gearX && mouseX < x + CARD_WIDTH && mouseY >= optionsY
                && mouseY < optionsY + optionsHeight;
        if (gearHover)
            RenderUtils.drawRect(gearX + 1, optionsY, 26 - 1, optionsHeight, 0x10FFFFFF);

        // Grayed gear
        int gearColor = hasSettings ? 0xFFFFFFFF : 0xFF555555;
        drawSmoothTexture(ICON_SETTINGS, gearX + (26 - 12) / 2, optionsY + 2, 12, 12, gearColor);

        // Separator 2 (Above Enabled/Disabled)
        RenderUtils.drawRect(x, stateY, CARD_WIDTH, 1, sepColor);

        // Enabled/Disabled Row
        boolean stateHover = mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= stateY && mouseY < y + CARD_HEIGHT;
        int stateColor = enabled ? ACCENT_GREEN : ACCENT_RED;
        if (stateHover)
            stateColor = enabled ? 0xFF27AE60 : 0xFFC0392B;
        else if (!enabled)
            stateColor = 0xFFE74C3C;

        RenderUtils.drawBottomRoundedRect(x, stateY, CARD_WIDTH, footerHeight, 7, stateColor);
        drawCenteredString(fontRendererObj, enabled ? "ENABLED" : "DISABLED", x + CARD_WIDTH / 2, stateY + 5,
                0xFFFFFFFF);

        // Card Border (Outline) - Drawn last to overlap/bound everything nicely
        RenderUtils.drawRoundedRectOutline(x, y, CARD_WIDTH, CARD_HEIGHT, 7, BORDER_COLOR, 2.0f);
    }

    private void drawSettingsPage(int x, int y, int mouseX, int mouseY) {
        // ... (Overhaul Settings Page Implementation) ...
        int headerH = 40;
        RenderUtils.drawRect(x, y + headerH - 1, GUI_WIDTH, 1, BORDER_COLOR); // Separator

        // Back Button with custom icon
        boolean backHover = mouseX >= x + 10 && mouseX < x + 36 && mouseY >= y + 7 && mouseY < y + 33;
        ResourceLocation backIcon = new ResourceLocation("housingclient", "textures/icons/backarrow.png");
        GlStateManager.color(backHover ? 0.3f : 0.6f, backHover ? 0.9f : 0.6f, backHover ? 0.3f : 0.6f, 1.0f);
        drawSmoothTexture(backIcon, x + 15, y + 12, 16, 16);
        GlStateManager.color(1f, 1f, 1f, 1f);
        if (backHover)
            RenderUtils.drawRoundedRect(x + 10, y + 7, 26, 26, 4, 0x20FFFFFF);

        // Title
        String title = settingsModule.getDisplayName().toUpperCase();
        fontRendererObj.drawStringWithShadow(title, x + 45, y + 14, 0xFFFFFFFF);

        // --- CONTENT ---
        int contentY = y + headerH;
        int contentH = GUI_HEIGHT - headerH;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissorCustomScale(x, contentY, GUI_WIDTH, contentH, 2.0);

        float currentY = contentY + 20 - settingsScroll;

        for (Setting<?> setting : settingsModule.getSettings()) {
            if (!setting.isVisible())
                continue;

            // Only draw if visible
            if (currentY + 30 >= contentY && currentY <= y + GUI_HEIGHT) {
                int itemX = x + 30;
                int itemWidth = GUI_WIDTH - 60;

                if (setting instanceof BooleanSetting) {
                    BooleanSetting bool = (BooleanSetting) setting;
                    fontRendererObj.drawString(setting.getName(), itemX, (int) currentY + 6, TEXT_WHITE);
                    // Animated Toggle Switch
                    int toggleH = 20;
                    int toggleX = itemX + itemWidth - 40;

                    drawAnimatedToggle(toggleX, (int) currentY, 40, toggleH, bool);
                } else if (setting instanceof NumberSetting) {
                    NumberSetting num = (NumberSetting) setting;
                    String valStr = num.isOnlyInt() ? String.valueOf(num.getIntValue())
                            : String.format("%.1f", num.getValue());
                    fontRendererObj.drawString(setting.getName(), itemX, (int) currentY, TEXT_WHITE);
                    fontRendererObj.drawString(valStr, itemX + fontRendererObj.getStringWidth(setting.getName()) + 8,
                            (int) currentY, TEXT_GRAY);
                    // Slider
                    int sliderX = itemX + 10;
                    int sliderY = (int) currentY + 14;
                    int sliderW = itemWidth - 20;
                    int sliderH = 4; // Thicker slider background
                    RenderUtils.drawRoundedRect(sliderX, sliderY, sliderW, sliderH, 2, SLIDER_BG);
                    double progress = (num.getValue() - num.getMin()) / (num.getMax() - num.getMin());
                    int fillW = (int) (sliderW * progress);
                    RenderUtils.drawRoundedRect(sliderX, sliderY, fillW, sliderH, 2, SLIDER_ACCENT);

                    // Slider Knob
                    RenderUtils.drawCircle(sliderX + fillW, sliderY + 2, 5, SLIDER_ACCENT);
                    RenderUtils.drawCircle(sliderX + fillW, sliderY + 2, 3, 0xFFFFFFFF);

                    currentY += 10;
                } else if (setting instanceof ModeSetting) {
                    ModeSetting mode = (ModeSetting) setting;
                    fontRendererObj.drawString(setting.getName(), itemX, (int) currentY + 6, TEXT_GRAY);
                    String val = mode.getValue();
                    int valW = fontRendererObj.getStringWidth(val);
                    fontRendererObj.drawString(val, itemX + itemWidth - valW - 15, (int) currentY + 6, TEXT_WHITE);
                    fontRendererObj.drawString(">", itemX + itemWidth - 10, (int) currentY + 6, 0xFF666666);
                } else if (setting instanceof ItemSetting) {
                    ItemSetting itemSet = (ItemSetting) setting;
                    fontRendererObj.drawString(setting.getName(), itemX, (int) currentY + 6, TEXT_WHITE);

                    int boxW = 100;
                    int boxH = 20;
                    int boxX = itemX + itemWidth - boxW;
                    int boxY = (int) currentY + 2;

                    // Draw Box
                    RenderUtils.drawRoundedRect(boxX, boxY, boxW, boxH, 4, 0xFF2F2F2F);

                    ItemStack stack = itemSet.getValue();
                    String display = (stack == null) ? "None" : stack.getDisplayName();

                    // Truncate
                    if (fontRendererObj.getStringWidth(display) > boxW - 25) {
                        display = fontRendererObj.trimStringToWidth(display, boxW - 35) + "...";
                    }

                    fontRendererObj.drawString(display, boxX + 22, boxY + 6, 0xFFFFFFFF);

                    // Render Icon if exists
                    if (stack != null) {
                        RenderHelper.enableGUIStandardItemLighting();
                        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, boxX + 2, boxY + 2);
                        RenderHelper.disableStandardItemLighting();
                    }

                    // Detect Click
                    boolean hover = mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY && mouseY < boxY + boxH;
                    if (hover && Mouse.isButtonDown(0)) {
                        // Debounce check? Mouse.isButtonDown fires every frame.
                        // Better to handle in mouseClicked but we are in drawSettingsPage which is
                        // called from drawScreen.
                        // Standard ClickGUI structure usually handles clicks in mouseClicked.
                        // But for now, let's just assume we check in mouseClicked.
                    }
                }
            }
            currentY += 30; // Row height
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Scrollbar logic for Settings
        float totalH = settingsModule.getSettings().size() * 30 + 40;
        if (totalH > contentH) {
            float ratio = contentH / totalH;
            int thumbH = Math.max(20, (int) (contentH * ratio));
            int scrollRange = (int) (totalH - contentH);
            int thumbY = contentY + (int) (contentH * (settingsScroll / scrollRange));
            if (thumbY + thumbH > y + GUI_HEIGHT)
                thumbY = y + GUI_HEIGHT - thumbH;
            RenderUtils.drawRect(x + GUI_WIDTH - 4, thumbY, 2, thumbH, ACCENT_GREEN);
        }
    }

    private void drawClientSettings(int x, int y, int mouseX, int mouseY) {
        // --- Client Settings Implementation ---
        // Instead of a module list, we draw custom settings controls directly here
        // Backed by ClickGUIModule or HUDModule

        int contentWidth = GUI_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;
        int startX = x + 20;
        int currentY = y + 20;

        com.housingclient.module.modules.client.ClickGUIModule clickGui = HousingClient.getInstance()
                .getModuleManager()
                .getModule(com.housingclient.module.modules.client.ClickGUIModule.class);

        com.housingclient.module.modules.client.HUDModule hud = HousingClient.getInstance()
                .getModuleManager()
                .getModule(com.housingclient.module.modules.client.HUDModule.class);

        // Title
        fontRendererObj.drawStringWithShadow("Client Customization", startX, currentY, TEXT_WHITE);
        currentY += 30;

        // 1. Logo Color (changes HOUSING text color in header)
        fontRendererObj.drawString("Logo Color", startX, currentY, TEXT_GRAY);
        currentY += 15;

        // Color Swatches
        int[] colors = { 0xFF2ECC71, 0xFF3498DB, 0xFFE74C3C, 0xFFF1C40F, 0xFF9B59B6, 0xFF2F2F2F }; // Green, Blue, Red,
                                                                                                   // Gold, Purple,
                                                                                                   // Chroma(Mock)
        String[] colorNames = { "Emerald", "Blue", "Red", "Gold", "Purple", "Chroma" };

        int swatchSize = 20;
        int swatchGap = 10;

        for (int i = 0; i < colors.length; i++) {
            int swatchX = startX + i * (swatchSize + swatchGap);
            int color = colors[i];

            boolean selected = false;
            // Check selection
            if (i == 0) {
                // Rainbow Mode Check
                selected = clickGui != null && clickGui.isRainbowEnabled();
            } else {
                // Color Match Check
                selected = (clickGui != null && !clickGui.isRainbowEnabled()
                        && clickGui.getAccentColor().getRGB() == color);
            }

            if (selected) {
                RenderUtils.drawRoundedRectOutline(swatchX - 2, currentY - 2, swatchSize + 4, swatchSize + 4, 6,
                        0xFFFFFFFF, 1.5f);
            }

            if (i == 0) {
                // Draw Rainbow Gradient Token
                // Connected quadrants with only outer corners rounded
                // COLORS: Red, Purple, Blue, Orange (High Contrast)

                // TL (Red) - Only Top Left rounded
                RenderUtils.drawRoundedRect(swatchX, currentY, swatchSize / 2, swatchSize / 2, 4, 0xFFFF0000, true,
                        false, false, false);
                // TR (Purple) - Only Top Right rounded
                RenderUtils.drawRoundedRect(swatchX + swatchSize / 2, currentY, swatchSize / 2, swatchSize / 2, 4,
                        0xFF9B59B6, false, true, false, false);
                // BL (Blue) - Only Bottom Left rounded
                RenderUtils.drawRoundedRect(swatchX, currentY + swatchSize / 2, swatchSize / 2, swatchSize / 2, 4,
                        0xFF3498DB, false, false, false, true);
                // BR (Orange) - Only Bottom Right rounded
                RenderUtils.drawRoundedRect(swatchX + swatchSize / 2, currentY + swatchSize / 2, swatchSize / 2,
                        swatchSize / 2, 4, 0xFFE67E22, false, false, true, false);
            } else {
                RenderUtils.drawRoundedRect(swatchX, currentY, swatchSize, swatchSize, 4, color);
            }
        }
        currentY += 40;

        // Common Toggle Coords
        int toggleW = 40;
        int toggleH = 20;
        int toggleX = startX + 200;

        // 2. Menu Blur (Toggle)
        fontRendererObj.drawString("Menu Blur", startX, currentY + 6, TEXT_WHITE);
        if (clickGui != null) {
            com.housingclient.module.settings.Setting<?> s = clickGui.getSetting("Blur Background");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s);
            }
        }
        currentY += 30;

        // 3. Watermark (Toggle)
        fontRendererObj.drawString("HUD Watermark", startX, currentY + 6, TEXT_WHITE);
        if (hud != null) {
            com.housingclient.module.settings.Setting<?> s = hud.getSetting("Watermark");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s);
            }
        }
        currentY += 30;

        // 4. Notifications
        fontRendererObj.drawString("Notifications", startX, currentY + 6, TEXT_WHITE);
        if (clickGui != null) {
            com.housingclient.module.settings.Setting<?> s = clickGui.getSetting("Notifications");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s);
            }
        }
        currentY += 30;

        // 5. Legacy Mode (Private Only)
        fontRendererObj.drawString("Legacy Mode", startX, currentY + 6, TEXT_WHITE);
        if (clickGui != null) {
            com.housingclient.module.settings.Setting<?> s = clickGui.getSetting("Legacy Mode");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s);
            }
        }
        currentY += 30;

        // 6. Tooltips
        fontRendererObj.drawString("Module Tooltips", startX, currentY + 6, TEXT_WHITE);
        if (clickGui != null) {
            com.housingclient.module.settings.Setting<?> s = clickGui.getSetting("Tooltips");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s);
            }
        }
        currentY += 30;

        // 7. Blatant Mode (Safe Mode Switch)
        fontRendererObj.drawString("Blatant Mode", startX, currentY + 6, 0xFFE74C3C);
        if (clickGui != null) {
            com.housingclient.module.settings.Setting<?> s = clickGui.getSetting("Blatant Mode");
            if (s instanceof BooleanSetting) {
                drawAnimatedToggle(toggleX, currentY, toggleW, toggleH, (BooleanSetting) s, 0xFFE74C3C);
            }
        }
        currentY += 30;
    }

    private void drawAnimatedToggle(int x, int y, int w, int h, BooleanSetting setting) {
        drawAnimatedToggle(x, y, w, h, setting, 0xFF2ECC71);
    }

    private void drawAnimatedToggle(int x, int y, int w, int h, BooleanSetting setting, int onColor) {
        // Animation Logic
        float target = setting.isEnabled() ? 1.0f : 0.0f;
        // Smooth lerp
        setting.animation = setting.animation + (target - setting.animation) * 0.15f;

        // Clamp logic to ensure it settles
        if (Math.abs(target - setting.animation) < 0.005f) {
            setting.animation = target;
        }

        // Colors
        // Off: Dark Gray/Blueish (0xFF353B48)
        int offColor = 0xFF2F3640;

        int color = interpolateColor(offColor, onColor, setting.animation);

        // Draw Background (Capsule) using fully rounded ends (radius = h/2)
        RenderUtils.drawRoundedRect(x, y, w, h, (float) h / 2.0f, color);

        // Draw Knob
        int padding = 2; // Gap between edge and circle
        int knobSize = h - padding * 2;

        // Logic for knob position:
        // 0.0 -> Left side (x + padding)
        // 1.0 -> Right side (x + w - padding - knobSize)
        float maxTravel = w - padding * 2 - knobSize;
        float knobX = x + padding + (maxTravel * setting.animation);

        // Draw Knob Shadow (Optional, subtle)
        // RenderUtils.drawCircle((int)(knobX + knobSize/2), y + h/2 + 1, knobSize/2,
        // 0x40000000);

        // Draw Knob (White Circle)
        // usage: x, y, w, h, radius, color
        RenderUtils.drawRoundedRect(knobX, y + padding, knobSize, knobSize, knobSize / 2.0, 0xFFFFFFFF);
    }

    // Color Interpolation Helper
    private int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * amount);
        int r = (int) (r1 + (r2 - r1) * amount);
        int g = (int) (g1 + (g2 - g1) * amount);
        int b = (int) (b1 + (b2 - b1) * amount);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ... Helpers ...
    private void drawSmoothTexture(ResourceLocation texture, int x, int y, int w, int h) {
        drawSmoothTexture(texture, x, y, w, h, 0xFFFFFFFF);
    }

    private void drawSmoothTexture(ResourceLocation texture, int x, int y, int w, int h, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        if ((color & 0xFF000000) == 0)
            a = 1.0f; // Fix for simple RGB passed as 0xRRGGBB

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(texture);

        // Standard Linear Filtering (Stable)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        GlStateManager.color(r, g, b, a);

        // Manual Quad drawing for precision
        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator.getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + h, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(x + w, y + h, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(x + w, y, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private ResourceLocation getIconForCategory(String iconName) {
        switch (iconName) {
            case "all":
                return ICON_ALL;
            case "client":
                return ICON_CLIENT;
            case "visual":
                return ICON_VISUAL;
            case "moderation":
                return ICON_MODERATION;
            case "exploits":
                return ICON_ANTIEXPLOIT;
            case "qol":
                return ICON_QOL;
            case "miscellaneous":
                return ICON_MISC;
            default:
                return ICON_ALL;
        }
    }

    private List<Module> getDisplayModules() {
        List<Module> result = new ArrayList<Module>();
        List<Module> all = HousingClient.getInstance().getModuleManager().getModules();
        boolean safeMode = HousingClient.getInstance().isSafeMode();

        for (Module m : all) {
            // Hide blatant modules if Safe Mode is active
            if (safeMode && m.isBlatant())
                continue;

            String name = m.getName().toLowerCase();
            if (name.equals("clickgui") || name.equals("hud designer"))
                continue;

            // Search Filtering
            if (searchField != null && !searchField.getText().isEmpty()) {
                String filter = searchField.getText().toLowerCase();
                if (name.contains(filter)) {
                    result.add(m);
                }
                continue; // Skip category checks if searching
            }

            // Treat null or "ALL" as showing all modules
            if (selectedCategory == null || selectedCategory.equals("ALL")) {
                if (!name.contains("clickgui"))
                    result.add(m);
            } else if (selectedCategory.equals("CLIENT")) {
                if (m.getCategory() == Category.CLIENT) {
                    result.add(m);
                }
            } else if (selectedCategory.equals("VISUAL")) {
                if (m.getCategory() == Category.VISUALS || m.getCategory() == Category.RENDER) {
                    String n = m.getName().toLowerCase().replace(" ", "");
                    // Exclude modules moved to other categories
                    if (!n.equals("crashdetector") && !n.equals("hidehykiaentities")) {
                        result.add(m);
                    }
                }
            } else if (selectedCategory.equals("MODERATION")) {
                // Explicitly exclude NBT Editor from MODERATION (it should be in MISCELLANEOUS)
                String modName = m.getName().toLowerCase().replace(" ", "");
                if (modName.equals("nbteditor")) {
                    continue;
                }
                if (m.getCategory() == Category.COMBAT || m.getCategory() == Category.INSTANT
                        || m.getCategory() == Category.MODERATION) {
                    result.add(m);
                } else if (m.getCategory() == Category.EXPLOIT) {
                    // Include generic exploits here, exclude specific ones for EXPLOITS tab
                    String n = m.getName().toLowerCase().replace(" ", "");
                    // NBT Logger, NBT Editor, Item Stealer go to EXPLOITS, not MODERATION
                    if (!n.equals("playercrasher") && !n.equals("servermatcher") && !n.equals("nbtlogger")
                            && !n.equals("nbteditor") && !n.equals("nbtgiver") && !n.equals("itemstealer")
                            && !n.equals("bypassblacklist") && !n.equals("boatfill")
                            && !n.equals("dispenserfill") && !n.equals("imagetonbt") && !n.equals("ghostdisc")
                            && !n.equals("packetmultiplier")) {
                        result.add(m);
                    }
                } else if (m.getCategory() == Category.VISUALS || m.getCategory() == Category.RENDER) {
                    // Crash Detector moved to MODERATION
                    String n = m.getName().toLowerCase().replace(" ", "");
                    if (n.equals("crashdetector")) {
                        result.add(m);
                    }
                }
            } else if (selectedCategory.equals("EXPLOIT")) {
                if (m.getCategory() == Category.EXPLOIT) {
                    result.add(m);
                } else {
                    // Include specific cross-category modules
                    String n = m.getName().toLowerCase().replace(" ", "");
                    if (n.equals("playercrasher") || n.equals("servermatcher") || n.equals("nbtlogger")
                            || n.equals("itemstealer")) {
                        // Avoid duplicates if they are already Category.EXPLOIT
                        if (m.getCategory() != Category.EXPLOIT) {
                            result.add(m);
                        }
                    }
                }
            } else if (selectedCategory.equals("MISCELLANEOUS")) {
                if (m.getCategory() == Category.MISCELLANEOUS) {
                    result.add(m);
                } else {
                    // Explicitly include NBT Editor even if module category doesn't match
                    String n = m.getName().toLowerCase().replace(" ", "");
                    if (n.equals("nbteditor")) {
                        result.add(m);
                    }
                }
            } else if (selectedCategory.equals("QOL")) {
                if (m.getCategory() == Category.MOVEMENT || m.getCategory() == Category.ITEMS
                        || m.getCategory() == Category.BUILDING || m.getCategory() == Category.MISC
                        || m.getCategory() == Category.QOL) {
                    result.add(m);
                } else {
                    // Hide Hykia Entities and Anti Void Lag moved to QOL
                    String n = m.getName().toLowerCase().replace(" ", "");
                    if (n.equals("hidehykiaentities") || n.equals("antivoidlag")) {
                        result.add(m);
                    }
                }
            }
        }
        result.sort(Comparator.comparing(Module::getName));
        return result;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int scaleFactor = new ScaledResolution(mc).getScaleFactor();
        float mouseScale = (float) scaleFactor / 2.0f;
        mouseX = (int) (mouseX * mouseScale);
        mouseY = (int) (mouseY * mouseScale);

        // Use virtual resolution (Scale 2) for centering to match drawScreen
        int virtualWidth = mc.displayWidth / 2;
        int virtualHeight = mc.displayHeight / 2;

        int guiX = (virtualWidth - GUI_WIDTH) / 2;
        int guiY = (virtualHeight - GUI_HEIGHT) / 2;

        // Check search bar FIRST - if clicked, handle it and return
        if (searchField != null && modsTab && settingsModule == null) {
            int searchW = 105;
            int searchX = guiX + GUI_WIDTH - searchW - 24;
            int searchY = guiY + (HEADER_HEIGHT - 18) / 2;

            if (mouseX >= searchX && mouseX < searchX + searchW &&
                    mouseY >= searchY && mouseY < searchY + 18) {
                searchField.mouseClicked(mouseX, mouseY, mouseButton);
                return; // Don't process other clicks
            }
        }

        // Handle search field for typing (but we already returned if clicked in bounds)
        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        // If in settings mode, only process settings interactions (not sidebar)
        if (settingsModule != null) {
            // --- SETTINGS INTERACTION ---

            // Back Button
            if (mouseX >= guiX + 10 && mouseX < guiX + 60 && mouseY >= guiY + 8 && mouseY < guiY + 36) {
                settingsModule = null;
                settingsScroll = 0;
                targetSettingsScroll = 0; // RESET TARGET SCROLL
                return;
            }

            int headerH = 40;
            int contentY = guiY + headerH;
            float currentY = contentY + 20 - settingsScroll;

            for (Setting<?> setting : settingsModule.getSettings()) {
                if (!setting.isVisible())
                    continue;

                int itemX = guiX + 30;
                int itemWidth = GUI_WIDTH - 60;

                // Only interact if in visible area (simplified)
                if (currentY + 20 >= contentY && currentY <= guiY + GUI_HEIGHT && mouseY >= currentY
                        && mouseY < currentY + 30) {
                    if (setting instanceof BooleanSetting) {
                        int toggleW = 40;
                        int toggleX = itemX + itemWidth - toggleW;
                        if (mouseX >= toggleX && mouseX < toggleX + toggleW) {
                            ((BooleanSetting) setting).toggle();
                            return;
                        }
                    } else if (setting instanceof ModeSetting) {
                        if (mouseX >= itemX + itemWidth - 100) {
                            ((ModeSetting) setting).cycle();
                            return;
                        }
                    } else if (setting instanceof NumberSetting) {
                        // Click jump
                        NumberSetting num = (NumberSetting) setting;
                        int sliderX = itemX + 10;
                        int sliderW = itemWidth - 20;
                        if (mouseX >= sliderX && mouseX <= sliderX + sliderW && mouseY >= currentY + 10) {
                            double progress = (double) (mouseX - sliderX) / sliderW;
                            progress = Math.max(0, Math.min(1, progress));
                            num.setValue(num.getMin() + (num.getMax() - num.getMin()) * progress);
                            return;
                        }
                    } else if (setting instanceof ItemSetting) {
                        int boxW = 100;
                        int boxH = 20;
                        int boxX = itemX + itemWidth - boxW;
                        int boxY = (int) currentY + 2;

                        if (mouseButton == 0 && mouseX >= boxX && mouseX < boxX + boxW && mouseY >= boxY
                                && mouseY < boxY + boxH) {
                            mc.displayGuiScreen(new ItemSelectorGUI(this, (ItemSetting) setting));
                            return;
                        }
                    }
                }
                if (setting instanceof NumberSetting)
                    currentY += 10; // Extra spacing
                currentY += 30;
            }
            return;
        }

        // --- MAIN VIEW INTERACTION ---

        // Check for Category clicks (only when NOT in settings mode)
        int catY = guiY + HEADER_HEIGHT + 12;
        for (String[] category : CATEGORIES) {
            if (mouseX >= guiX + 8 && mouseX < guiX + SIDEBAR_WIDTH - 8 && mouseY >= catY && mouseY < catY + 28) {
                selectedCategory = category[0];
                if (searchField != null)
                    searchField.setText("");
                scrollOffset = 0;
                targetScroll = 0;
                return;
            }
            catY += 35;
        }

        // Match drawHeader logic for Search Bar position to detect center gap
        int searchW = 105;
        int searchX = guiX + GUI_WIDTH - searchW - 24;

        int tabWidth = 105;
        int tabHeight = 22;
        int tabY = guiY + (HEADER_HEIGHT - tabHeight) / 2;

        // Center Tabs between Sidebar and Search Bar
        int sidebarEndX = guiX + SIDEBAR_WIDTH;
        int availableSpace = searchX - sidebarEndX;
        int totalTabsWidth = (tabWidth * 3) + 16; // Fix: 3 tabs with 2 gaps of 8px

        int tabStartX = sidebarEndX + (availableSpace - totalTabsWidth) / 2;

        if (mouseX >= tabStartX && mouseX < tabStartX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
            modsTab = true;
            settingsModule = null; // Clear settings
            scrollOffset = 0;
            targetScroll = 0; // RESET TARGET SCROLL
            return;
        }
        int settingsTabX = tabStartX + tabWidth + 8;
        if (mouseX >= settingsTabX && mouseX < settingsTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
            modsTab = false;
            settingsModule = null; // Clear settings
            if (searchField != null)
                searchField.setText("");
            return;
        }
        int hudTabX = settingsTabX + tabWidth + 8;
        if (mouseX >= hudTabX && mouseX < hudTabX + tabWidth && mouseY >= tabY && mouseY < tabY + tabHeight) {
            // Open HUD Designer
            // Close ClickGUI first? Or just switch?
            // Need to get HudDesignerModule reference to pass to constructor
            com.housingclient.module.modules.client.HudDesignerModule pad = HousingClient.getInstance()
                    .getModuleManager().getModule(com.housingclient.module.modules.client.HudDesignerModule.class);
            if (pad != null) {
                // Trigger the HUD Editor
                pad.setEnabled(true);
            }
            return;
        }

        // REMOVED CATEGORY LOOP HERE - MOVED UP
        // int catY = guiY + HEADER_HEIGHT + 12;
        // for (String[] category : CATEGORIES) { ... }

        // Ignore clicks in header area (prevents click-through to module cards)
        if (mouseY >= guiY && mouseY < guiY + HEADER_HEIGHT) {
            return; // Click is in header, don't process module interactions
        }

        if (modsTab) {
            int cardAreaX = guiX + SIDEBAR_WIDTH + 12;
            int cardAreaY = (int) (guiY + HEADER_HEIGHT + 12 - scrollOffset);
            List<Module> modules = getDisplayModules();
            int col = 0, row = 0;

            for (Module module : modules) {
                int cardX = cardAreaX + col * (CARD_WIDTH + CARD_GAP);
                int cardY = cardAreaY + row * (CARD_HEIGHT + CARD_GAP);

                if (cardY + CARD_HEIGHT >= guiY + HEADER_HEIGHT && cardY <= guiY + GUI_HEIGHT && mouseX >= cardX
                        && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {

                    int footerHeight = 18;
                    int optionsHeight = 16;
                    int gearWidth = 26;
                    int stateY = cardY + CARD_HEIGHT - footerHeight;
                    int optionsY = stateY - optionsHeight;

                    // Options Click
                    if (mouseY >= optionsY && mouseY < optionsY + optionsHeight) {
                        if (module.getSettings().isEmpty())
                            return; // Don't open if no settings
                        settingsModule = module;
                        lastSettingsOpenTime = System.currentTimeMillis();
                        settingsScroll = 0;
                        targetSettingsScroll = 0; // RESET TARGET SCROLL
                        return;
                    }

                    // Keybind Click
                    String bindText = (bindingModule == module) ? "..."
                            : (module.getKeybind() == 0 ? "[None]"
                                    : "[" + Keyboard.getKeyName(module.getKeybind()) + "]");
                    int bindW = fontRendererObj.getStringWidth(bindText) + 6;
                    int bindX = cardX + CARD_WIDTH - bindW - 5;
                    int bindY = cardY + 5;
                    if (mouseX >= bindX && mouseX < bindX + bindW && mouseY >= bindY && mouseY < bindY + 10) {
                        bindingModule = module;
                        // Defocus search while binding to prevent key conflicts
                        if (searchField != null) {
                            searchField.setFocused(false);
                        }
                        return;
                    }

                    // Enabled Toggle
                    if (mouseY >= stateY && mouseY < cardY + CARD_HEIGHT) {
                        module.toggle();
                        return;
                    }

                    // Card Click (Default to toggle)
                    if (mouseButton == 0) {
                        module.toggle();
                    }
                    return;
                }
                col++;
                if (col >= CARDS_PER_ROW) {
                    col = 0;
                    row++;
                }
            }
        } else {
            // --- CLIENT SETTINGS INTERACTION ---
            com.housingclient.module.modules.client.ClickGUIModule clickGui = HousingClient.getInstance()
                    .getModuleManager()
                    .getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
            com.housingclient.module.modules.client.HUDModule hud = HousingClient.getInstance().getModuleManager()
                    .getModule(com.housingclient.module.modules.client.HUDModule.class);

            if (clickGui != null && hud != null) {
                int startX = guiX + SIDEBAR_WIDTH + 20;
                int currentY = guiY + HEADER_HEIGHT + 20;

                // Title skip
                currentY += 30;

                // 1. Accent Color
                // Text skip
                currentY += 15;
                int[] colors = { 0xFF2ECC71, 0xFF3498DB, 0xFFE74C3C, 0xFFF1C40F, 0xFF9B59B6, 0xFF2F2F2F };
                int swatchSize = 20;
                int swatchGap = 10;
                for (int i = 0; i < colors.length; i++) {
                    int swatchX = startX + i * (swatchSize + swatchGap);
                    if (mouseX >= swatchX && mouseX < swatchX + swatchSize && mouseY >= currentY
                            && mouseY < currentY + swatchSize) {
                        // Set Accent Color
                        com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                                .filter(set -> set.getName().equals("Accent Color")).findFirst().orElse(null);
                        com.housingclient.module.settings.Setting<?> r = clickGui.getSettings().stream()
                                .filter(set -> set.getName().equals("Rainbow Mode")).findFirst().orElse(null);

                        if (i == 0) {
                            // First swatch is now Rainbow Mode
                            if (r instanceof com.housingclient.module.settings.BooleanSetting) {
                                ((com.housingclient.module.settings.BooleanSetting) r).setValue(true);
                            }
                        } else {
                            // Other swatches disable Rainbow Mode and set color
                            if (r instanceof com.housingclient.module.settings.BooleanSetting) {
                                ((com.housingclient.module.settings.BooleanSetting) r).setValue(false);
                            }
                            if (s instanceof com.housingclient.module.settings.ColorSetting) {
                                ((com.housingclient.module.settings.ColorSetting) s)
                                        .setValue(new java.awt.Color(colors[i]));
                            }
                        }
                        return;
                    }
                }
                currentY += 40;

                // Common Toggle Coords
                int toggleW = 40;
                // Use full row width for easier clicking
                int rowWidth = GUI_WIDTH - SIDEBAR_WIDTH - 40; // Approximate available width

                // 2. Menu Blur
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                            .filter(set -> set.getName().equals("Blur Background")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting) {
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                        // Live toggle shader
                        boolean nowEnabled = ((com.housingclient.module.settings.BooleanSetting) s).isEnabled();
                        if (nowEnabled && OpenGlHelper.shadersSupported
                                && mc.getRenderViewEntity() instanceof EntityPlayer) {
                            try {
                                mc.entityRenderer.loadShader(new ResourceLocation("shaders/post/blur.json"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (mc.entityRenderer.getShaderGroup() != null) {
                            mc.entityRenderer.stopUseShader();
                        }
                    }
                    return;
                }
                currentY += 30;

                // 3. Watermark
                // Note: Watermark is in HUD module, so check hud settings
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = hud.getSettings().stream()
                            .filter(set -> set.getName().equals("Watermark")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting)
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                    return;
                }
                currentY += 30;

                // 4. Notifications
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                            .filter(set -> set.getName().equals("Notifications")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting)
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                    return;
                }
                currentY += 30;

                // 5. Legacy Mode
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                            .filter(set -> set.getName().equals("Legacy Mode")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting)
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                    return;
                }
                currentY += 30;

                // 6. Tooltips
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                            .filter(set -> set.getName().equals("Tooltips")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting)
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                    return;
                }
                currentY += 30;

                // 7. Blatant Mode
                if (mouseX >= startX && mouseX < startX + rowWidth && mouseY >= currentY && mouseY < currentY + 30) {
                    com.housingclient.module.settings.Setting<?> s = clickGui.getSettings().stream()
                            .filter(set -> set.getName().equals("Blatant Mode")).findFirst().orElse(null);
                    if (s instanceof com.housingclient.module.settings.BooleanSetting)
                        ((com.housingclient.module.settings.BooleanSetting) s).toggle();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (settingsModule != null && clickedMouseButton == 0) {
            if (System.currentTimeMillis() - lastSettingsOpenTime < 200)
                return; // Debounce slider drag
            ScaledResolution sr = new ScaledResolution(mc);
            int guiX = (sr.getScaledWidth() - GUI_WIDTH) / 2;
            int guiY = (sr.getScaledHeight() - GUI_HEIGHT) / 2;

            int headerH = 40;
            int contentY = guiY + headerH;
            float currentY = contentY + 20 - settingsScroll;

            for (Setting<?> setting : settingsModule.getSettings()) {
                if (!setting.isVisible())
                    continue;

                int itemX = guiX + 30;
                int itemWidth = GUI_WIDTH - 60;

                if (setting instanceof NumberSetting) {
                    int sliderX = itemX + 10;
                    int sliderW = itemWidth - 20;
                    // Interact if broadly in row
                    if (mouseY >= currentY && mouseY < currentY + 30) {
                        NumberSetting num = (NumberSetting) setting;
                        double progress = (double) (mouseX - sliderX) / sliderW;
                        progress = Math.max(0, Math.min(1, progress));
                        num.setValue(num.getMin() + (num.getMax() - num.getMin()) * progress);
                    }
                    currentY += 10;
                }
                currentY += 30;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        // IMPORTANT: Check binding mode FIRST before search field
        if (bindingModule != null) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_DELETE) {
                bindingModule.setKeybind(0);
            } else {
                bindingModule.setKeybind(keyCode);
            }
            bindingModule = null;
            // Re-focus search field after binding is complete
            if (searchField != null) {
                searchField.setFocused(true);
            }
            return;
        }

        // Now handle search field input (even in settings)
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            // Auto-switch to "All" category when user starts typing
            if (searchField.getText().length() > 0) {
                selectedCategory = "ALL";
                settingsModule = null; // Exit settings view
                modsTab = true; // Switch to modules tab
            }
            return;
        }

        // REMOVED OLD SEARCH HANDLER - MOVED UP

        if (keyCode == Keyboard.KEY_ESCAPE) {
            if (settingsModule != null) {
                settingsModule = null;
                return;
            }
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if (scroll != 0) {
            if (settingsModule != null) {
                // Settings scroll
                float totalH = settingsModule.getSettings().size() * 30 + 40;
                float maxScroll = Math.max(0, totalH - (GUI_HEIGHT - 40));
                targetSettingsScroll -= scroll / 3.0f;
                targetSettingsScroll = Math.max(0, Math.min(maxScroll, targetSettingsScroll));
            } else if (modsTab) {
                // Grid scroll
                List<Module> modules = getDisplayModules();
                int totalRows = (modules.size() + CARDS_PER_ROW - 1) / CARDS_PER_ROW;
                int contentHeight = GUI_HEIGHT - HEADER_HEIGHT;
                int totalHeight = totalRows * (CARD_HEIGHT + CARD_GAP) + 24;
                int maxScroll = Math.max(0, totalHeight - contentHeight);
                targetScroll -= scroll / 3.0f;
                targetScroll = Math.max(0, Math.min(maxScroll, targetScroll));
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}