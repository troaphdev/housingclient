package com.housingclient.gui;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.Module;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Rise-style toggle notification system
 * Shows animated popups in bottom-right when modules are toggled
 * Dark mode with proper rounded corners
 */
public class ToggleNotification {

    private static final List<Notification> notifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 3;
    private static final int NOTIFICATION_WIDTH = 200;
    private static final int NOTIFICATION_HEIGHT = 45;
    private static final int MARGIN = 12;
    private static final long DISPLAY_TIME = 2500; // 2.5 seconds
    private static final long FADE_TIME = 400; // 400ms fade

    // Debounce to prevent double notifications
    private static String lastModuleName = "";
    private static long lastNotificationTime = 0;
    private static final long DEBOUNCE_TIME = 100; // 100ms debounce

    /**
     * Add a new toggle notification
     */
    public static void addNotification(Module module, boolean enabled) {
        // Debounce: prevent duplicate notifications for same module within short time
        long now = System.currentTimeMillis();
        String displayName = module.getDisplayName();
        if (displayName.equals(lastModuleName) && (now - lastNotificationTime) < DEBOUNCE_TIME) {
            return; // Skip duplicate
        }
        lastModuleName = displayName;
        lastNotificationTime = now;

        // Remove oldest if at max
        while (notifications.size() >= MAX_NOTIFICATIONS) {
            notifications.remove(0);
        }

        notifications.add(new Notification(displayName, enabled));
    }

    /**
     * Render all active notifications
     */
    public static void render() {
        if (notifications.isEmpty())
            return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null)
            return;

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fr = mc.fontRendererObj;

        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // Update and render notifications
        Iterator<Notification> iterator = notifications.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            Notification notif = iterator.next();

            // Update animation
            notif.update();

            // Remove if expired
            if (notif.isExpired()) {
                iterator.remove();
                continue;
            }

            // Calculate position (stack from bottom)
            // Calculate position (stack from bottom)
            // Start from bottom margin + height of one notification
            int spacing = 12; // Increased spacing
            int bottomOffset = MARGIN + NOTIFICATION_HEIGHT;
            int targetY = screenHeight - bottomOffset - (index * (NOTIFICATION_HEIGHT + spacing));

            // Slide animation
            float slideProgress = notif.getSlideProgress();
            int slideOffset = (int) ((NOTIFICATION_WIDTH + MARGIN + 20) * (1 - slideProgress));
            int x = screenWidth - MARGIN - NOTIFICATION_WIDTH + slideOffset;

            // Fade animation
            float alpha = notif.getAlpha();

            // Render notification
            renderNotification(fr, x, targetY, notif, alpha);

            index++;
        }
    }

    private static void renderNotification(FontRenderer fr, int x, int y, Notification notif, float alpha) {
        // Transparent (glass-like) background
        int bgAlpha = (int) (128 * alpha);
        int textAlpha = (int) (255 * alpha);

        // Dark background with rounded corners
        int bgColor = (bgAlpha << 24) | 0x1a1a22;
        RenderUtils.drawRoundedRect(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 10, bgColor);

        // Subtle border
        RenderUtils.drawRoundedRectOutline(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 10,
                Theme.withAlpha(0x404050, bgAlpha / 2), 1);

        // Left accent bar with glow
        int accentColor = notif.enabled ? Theme.ACCENT : 0xFFef4444;
        RenderUtils.drawRoundedRect(x + 6, y + 10, 4, NOTIFICATION_HEIGHT - 20, 2,
                Theme.withAlpha(accentColor, textAlpha));

        // Icon circle
        int iconX = x + 20;
        int iconY = y + 12;
        int iconSize = 22;
        int iconBgColor = notif.enabled ? Theme.withAlpha(0x1a4a3a, bgAlpha) : Theme.withAlpha(0x4a1a1a, bgAlpha);
        RenderUtils.drawRoundedRect(iconX, iconY, iconSize, iconSize, 6, iconBgColor);

        // Checkmark or X icon
        String icon = notif.enabled ? "\u2713" : "\u2715";
        int iconColor = notif.enabled ? Theme.withAlpha(Theme.ACCENT, textAlpha) : Theme.withAlpha(0xef4444, textAlpha);
        fr.drawString(icon, iconX + 7, iconY + 7, iconColor);

        // "Toggled" header
        int textX = iconX + iconSize + 10;
        int headerColor = notif.enabled ? Theme.withAlpha(Theme.ACCENT, textAlpha)
                : Theme.withAlpha(0xef4444, textAlpha);
        fr.drawStringWithShadow("Toggled", textX, y + 12, headerColor);

        // Module name and state
        String stateText = notif.moduleName + (notif.enabled ? " on" : " off");
        int stateColor = Theme.withAlpha(0xaaaaaa, textAlpha);
        fr.drawStringWithShadow(stateText, textX, y + 26, stateColor);
    }

    /**
     * Individual notification data
     */
    private static class Notification {
        final String moduleName;
        final boolean enabled;
        final long startTime;
        float slideAnimation = 0;

        Notification(String moduleName, boolean enabled) {
            this.moduleName = moduleName;
            this.enabled = enabled;
            this.startTime = System.currentTimeMillis();
        }

        void update() {
            // Slide in animation
            if (slideAnimation < 1) {
                slideAnimation = Math.min(1, slideAnimation + 0.12f);
            }
        }

        boolean isExpired() {
            return System.currentTimeMillis() - startTime > DISPLAY_TIME + FADE_TIME;
        }

        float getSlideProgress() {
            // Ease out cubic
            float t = slideAnimation;
            return 1 - (1 - t) * (1 - t) * (1 - t);
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < DISPLAY_TIME) {
                return 1.0f;
            }
            // Fade out
            float fadeProgress = (elapsed - DISPLAY_TIME) / (float) FADE_TIME;
            return Math.max(0, 1 - fadeProgress);
        }
    }
}
