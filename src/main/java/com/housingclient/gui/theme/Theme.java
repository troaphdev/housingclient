package com.housingclient.gui.theme;

import com.housingclient.HousingClient;
import com.housingclient.config.ConfigManager;

import java.awt.Color;

/**
 * Rise-style theme with clean dark aesthetics
 */
public class Theme {

    // Rise Color Palette
    public static final int BACKGROUND = 0xF21a1a1f; // Main modal background
    public static final int SIDEBAR = 0xF2151518; // Left sidebar
    public static final int SIDEBAR_SELECTED = 0xFF3d3d45; // Selected category bg
    public static final int ACCENT = 0xFF4ade80; // Green accent (selected items)
    public static final int ACCENT_BLUE = 0xFF60a5fa; // Blue accent (toggle notification)
    public static final int MODULE_HOVER = 0xFF2a2a30; // Module hover state
    public static final int MODULE_EXPANDED = 0xFF252528; // Expanded module background
    public static final int TEXT_PRIMARY = 0xFFffffff; // White text
    public static final int TEXT_SECONDARY = 0xFF888888; // Gray text
    public static final int TEXT_DARK = 0xFF666666; // Darker gray
    public static final int DIVIDER = 0xFF2a2a30; // Divider lines
    public static final int NOTIFICATION_BG = 0xFFf5f5f5; // Toggle notification background
    public static final int SCROLLBAR = 0xFF3a3a42; // Scrollbar track
    public static final int SCROLLBAR_THUMB = 0xFF5a5a65; // Scrollbar handle

    // Instance colors (can be overridden by config)
    private int accentColor = ACCENT;
    private int accentBlue = ACCENT_BLUE;
    private int backgroundColor = BACKGROUND;
    private int sidebarColor = SIDEBAR;
    private int textColor = TEXT_PRIMARY;
    private int textSecondary = TEXT_SECONDARY;

    // Animation
    private float animationSpeed = 0.15f;

    public Theme() {
        loadFromConfig();
    }

    public void loadFromConfig() {
        ConfigManager config = HousingClient.getInstance().getConfigManager();
        if (config != null) {
            Color primary = config.getPrimaryColor();
            if (primary != null) {
                accentColor = primary.getRGB();
            }
        }
    }

    // Static getters for constants
    public static int getBackground() {
        return BACKGROUND;
    }

    public static int getSidebar() {
        return SIDEBAR;
    }

    public static int getSidebarSelected() {
        return SIDEBAR_SELECTED;
    }

    public static int getModuleHover() {
        return MODULE_HOVER;
    }

    public static int getModuleExpanded() {
        return MODULE_EXPANDED;
    }

    public static int getTextPrimary() {
        return TEXT_PRIMARY;
    }

    public static int getTextSecondaryColor() {
        return TEXT_SECONDARY;
    }

    public static int getTextDark() {
        return TEXT_DARK;
    }

    public static int getDivider() {
        return DIVIDER;
    }

    public static int getNotificationBg() {
        return NOTIFICATION_BG;
    }

    public static int getScrollbar() {
        return SCROLLBAR;
    }

    public static int getScrollbarThumb() {
        return SCROLLBAR_THUMB;
    }

    // Instance getters
    public int getAccentColor() {
        return accentColor;
    }

    public int getAccentBlue() {
        return accentBlue;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getSidebarColor() {
        return sidebarColor;
    }

    public int getTextColor() {
        return textColor;
    }

    public int getTextSecondary() {
        return textSecondary;
    }

    public float getAnimationSpeed() {
        return animationSpeed;
    }

    // Legacy getters for compatibility
    public int getHeaderColor() {
        return SIDEBAR;
    }

    public int getPanelColor() {
        return BACKGROUND;
    }

    public int getModuleColor() {
        return BACKGROUND;
    }

    public int getModuleColorHover() {
        return MODULE_HOVER;
    }

    public int getEnabledColor() {
        return accentColor;
    }

    public int getEnabledBackgroundColor() {
        return accentColor;
    }

    public int getDisabledColor() {
        return TEXT_SECONDARY;
    }

    public int getHoverColor() {
        return MODULE_HOVER;
    }

    public int getBorderColor() {
        return DIVIDER;
    }

    public int getShadowColor() {
        return 0x60000000;
    }

    public int getSettingsBackground() {
        return MODULE_EXPANDED;
    }

    // Setters
    public void setAccentColor(int color) {
        this.accentColor = color;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    // Color utilities
    public static int withAlpha(int color, int alpha) {
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    public static int darker(int color, float factor) {
        int r = (int) (((color >> 16) & 0xFF) * (1 - factor));
        int g = (int) (((color >> 8) & 0xFF) * (1 - factor));
        int b = (int) ((color & 0xFF) * (1 - factor));
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int brighter(int color, float factor) {
        int r = (int) Math.min(255, ((color >> 16) & 0xFF) * (1 + factor));
        int g = (int) Math.min(255, ((color >> 8) & 0xFF) * (1 + factor));
        int b = (int) Math.min(255, (color & 0xFF) * (1 + factor));
        int a = (color >> 24) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int blend(int color1, int color2, float ratio) {
        float ir = 1.0f - ratio;
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 * ir + a2 * ratio);
        int r = (int) (r1 * ir + r2 * ratio);
        int g = (int) (g1 * ir + g2 * ratio);
        int b = (int) (b1 * ir + b2 * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getRainbowColor(long offset) {
        float hue = ((System.currentTimeMillis() + offset) % 3000) / 3000f;
        return Color.HSBtoRGB(hue, 0.6f, 1.0f);
    }
}
