package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.gui.theme.Theme;
import com.housingclient.module.Module;
import com.housingclient.module.modules.client.HUDModule;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.Setting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Rise-style HUD with clean module list and toggle notifications
 */
public class HUD {

    private final Minecraft mc = Minecraft.getMinecraft();
    private FontRenderer fontRenderer;
    private final List<Long> leftClicks = new ArrayList<Long>();
    private final List<Long> rightClicks = new ArrayList<Long>();
    private HUDModule hudModule;
    private HudDesignerModule hudDesigner;

    public HUD() {
    }

    private HUDModule getHudModule() {
        if (hudModule == null && HousingClient.getInstance() != null
                && HousingClient.getInstance().getModuleManager() != null) {
            hudModule = HousingClient.getInstance().getModuleManager().getModule(HUDModule.class);
        }
        return hudModule;
    }

    private HudDesignerModule getHudDesigner() {
        if (hudDesigner == null && HousingClient.getInstance() != null
                && HousingClient.getInstance().getModuleManager() != null) {
            hudDesigner = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        }
        return hudDesigner;
    }

    @SubscribeEvent
    public void onRenderHotbar(RenderGameOverlayEvent.Pre event) {
        // Custom hotbar removed - settings were removed from HUD module
    }

    private void renderCustomHotbar(int screenWidth, int screenHeight, String style) {
        int slotSize = 20;
        int slots = 9;
        int hotbarWidth = slots * slotSize + 4;
        int hotbarHeight = slotSize + 4;
        int x = (screenWidth - hotbarWidth) / 2;
        int y = screenHeight - hotbarHeight - 2;

        // Background based on style
        switch (style) {
            case "Dark":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 6, 0xD0101015);
                RenderUtils.drawRoundedRectOutline(x, y, hotbarWidth, hotbarHeight, 6, 0x40ffffff, 1);
                break;
            case "Gradient":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 6, 0xD0101015);
                // Gradient accent at bottom
                RenderUtils.drawGradientRect(x + 6, y + hotbarHeight - 3, hotbarWidth - 12, 2,
                        Theme.ACCENT, Theme.ACCENT_BLUE, true);
                break;
            case "Minimal":
                RenderUtils.drawRoundedRect(x, y, hotbarWidth, hotbarHeight, 4, 0x80000000);
                break;
        }

        // Draw slot backgrounds and items
        for (int i = 0; i < slots; i++) {
            int slotX = x + 2 + i * slotSize;
            int slotY = y + 2;

            boolean selected = (i == mc.thePlayer.inventory.currentItem);

            if (selected) {
                // Selected slot highlight
                RenderUtils.drawRoundedRect(slotX, slotY, slotSize, slotSize, 4, 0x40ffffff);
                if (!style.equals("Minimal")) {
                    RenderUtils.drawRoundedRectOutline(slotX, slotY, slotSize, slotSize, 4, Theme.ACCENT, 1.5f);
                }
            } else {
                // Unselected slot subtle bg
                RenderUtils.drawRoundedRect(slotX, slotY, slotSize, slotSize, 3, 0x20ffffff);
            }

            // Render item
            net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null) {
                net.minecraft.client.renderer.GlStateManager.enableRescaleNormal();
                net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
                mc.getRenderItem().renderItemAndEffectIntoGUI(stack, slotX + 2, slotY + 2);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, stack, slotX + 2, slotY + 2);
                net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
                net.minecraft.client.renderer.GlStateManager.disableRescaleNormal();
            }
        }

        // Offhand (for 1.8.9 compatibility, skip)
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL)
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen instanceof ClickGUI) {
            // Keep rendering
        }
        if (fontRenderer == null)
            fontRenderer = mc.fontRendererObj;

        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();

        // Always render toggle notifications
        ToggleNotification.render();

        HUDModule hud = getHudModule();
        if (hud == null || !hud.isEnabled())
            return;

        HudDesignerModule designer = getHudDesigner();

        Color primary = hud.getPrimaryColor();
        int mlX = designer != null ? designer.getModuleListX(width) : width - 100;
        int mlY = designer != null ? designer.getModuleListY() : 2;
        drawModuleList(width, height, mlX, mlY, primary, hud.showModuleListBackground(), hud.getBackgroundPadding());

        // Draw watermark if enabled (top-left corner)
        if (hud.showWatermark()) {
            drawWatermark(5, 5);
        }

        // HUD info elements are now separate modules in Visuals category
        // (CPS, FPS, Ping, Coords, Direction, Biome, Clock)
        // Draw main HUD info elements
        // Safe Mode Indicator removed as per user request (controlled via Settings now)

        // Blink Timer - special handling to show even if module disabled, if setting is
        // enabled
        // We do this here because modules usually only render when enabled
        com.housingclient.module.modules.exploit.BlinkModule blink = com.housingclient.HousingClient.getInstance()
                .getModuleManager().getModule(com.housingclient.module.modules.exploit.BlinkModule.class);
        if (blink != null && blink.isDisplayTimerEnabled()) {
            blink.onRender();
        }

        // Draw version info removed as requested
        // drawVersionInfo(width, height);
    }

    private void drawModuleList(int screenWidth, int screenHeight, int xPos, int yPos, Color primary, boolean showBg,
            double bgPadding) {
        List<Module> enabledModules = HousingClient.getInstance().getModuleManager().getVisibleModules();
        if (enabledModules.isEmpty())
            return;

        // Filter out hidden modules (HUD, ClickGUI, HUD Designer)
        List<Module> filtered = new ArrayList<>();
        for (Module m : enabledModules) {
            String name = m.getName().toLowerCase();
            if (name.equals("module list") || name.equals("clickgui") || name.equals("hud designer")) {
                continue;
            }
            filtered.add(m);
        }

        // Safe Mode Filter: Hide blatant modules if blatant mode is disabled
        boolean safeMode = HousingClient.getInstance().isSafeMode();
        if (safeMode) {
            filtered.removeIf(Module::isBlatant);
        }
        if (filtered.isEmpty()) {
            // Even if empty, we might want to set size to 0 or something so dragon box
            // disappears?
            // But existing code returns.
            return;
        }

        // Sort by display text length (longest first) for clean stacking
        filtered.sort((a, b) -> {
            String aText = getModuleDisplayText(a);
            String bText = getModuleDisplayText(b);
            return fontRenderer.getStringWidth(bText) - fontRenderer.getStringWidth(aText);
        });

        // CALCULATION VARS
        int maxModuleWidth = 0;
        int totalModuleHeight = 0;

        // SCALE SECTION
        GlStateManager.pushMatrix();
        float scale = 0.85f;
        GlStateManager.scale(scale, scale, 1.0f);

        // Adjust coordinates for scale
        // xPos is "visual" coordinate, so we map to scaled coordinate
        int scaledX = (int) (xPos / scale);
        int scaledY = (int) (yPos / scale);
        int scaledScreenWidth = (int) (screenWidth / scale);

        int y = scaledY;
        int colorOffset = 0;

        for (Module module : filtered) {
            String name = module.getDisplayName().toLowerCase();
            String modeInfo = getModeModeInfo(module);

            String fullText = modeInfo.isEmpty() ? name : name + " " + modeInfo;
            int textWidth = fontRenderer.getStringWidth(fullText);

            int x = scaledX;

            // Simple auto-align logic based on screen position
            boolean rightAlign = scaledX > scaledScreenWidth / 2;
            if (rightAlign) {
                x = scaledX - textWidth; // Anchor point is Right, so draw content to the left of X
            }

            // Rise-style rainbow/gradient colors
            float hue = ((System.currentTimeMillis() + colorOffset * 100L) % 4000) / 4000f;
            int nameColor = Color.HSBtoRGB(hue, 0.5f, 1.0f);

            // Using padding settings but reducing vertical padding to make it "even"
            // Default font height is 9, but visually ~7px. Standard padding makes vertical
            // huge.
            // Adjustment: use base height 8 and reduced padding
            int pad = (int) bgPadding;
            int vPad = Math.max(0, pad - 2); // Aggressively reduce vertical padding

            // Calc item width/height for this module
            int itemWidth = textWidth + (pad * 2);
            int itemHeight = 8 + (vPad * 2);

            if (itemWidth > maxModuleWidth)
                maxModuleWidth = itemWidth;
            totalModuleHeight += itemHeight;

            // Optional subtle background with customizable padding
            if (showBg) {
                // Reduced height for background to be tighter vertically
                RenderUtils.drawRect(x - pad, y - vPad, textWidth + (pad * 2), 8 + (vPad * 2), 0x50000000);
            }

            // Draw module name with shadow
            fontRenderer.drawStringWithShadow(name, x, y, nameColor);

            // Draw mode info in WHITE (not the gradient)
            if (!modeInfo.isEmpty()) {
                int nameDisplayWidth = fontRenderer.getStringWidth(name + " ");
                fontRenderer.drawStringWithShadow(modeInfo, x + nameDisplayWidth, y, 0xFFFFFFFF);
            }

            // Increment Y by tightened height (8 + padding)
            y += itemHeight;
            colorOffset++;
        }

        GlStateManager.popMatrix();

        // Update HUDModule dimensions for Designer
        HUDModule hud = getHudModule();
        if (hud != null) {
            // Convert back to visual pixels
            hud.setWidth((int) (maxModuleWidth * scale));
            hud.setHeight((int) (totalModuleHeight * scale));
        }
    }

    /**
     * Get only the MODE setting value (single word), not numbers
     */
    private String getModeModeInfo(Module module) {
        // First, check if module has a getDisplayInfo that's a simple mode word
        String displayInfo = module.getDisplayInfo();
        if (displayInfo != null && !displayInfo.isEmpty()) {
            // Skip if it's a number
            try {
                Double.parseDouble(displayInfo);
                // It's a number, skip it
            } catch (NumberFormatException e) {
                // Not a number, check if it's a single word mode
                if (!displayInfo.contains(" ") && displayInfo.length() <= 15) {
                    return displayInfo.toLowerCase();
                }
            }
        }

        // Look for first ModeSetting and use its value
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof ModeSetting) {
                String value = ((ModeSetting) setting).getValue();
                if (value != null && !value.isEmpty() && value.length() <= 15) {
                    return value.toLowerCase();
                }
            }
        }

        return "";
    }

    private String getModuleDisplayText(Module module) {
        String info = getModeModeInfo(module);
        if (!info.isEmpty()) {
            return module.getDisplayName().toLowerCase() + " " + info;
        }
        return module.getDisplayName().toLowerCase();
    }

    private void drawCoordinates(int x, int y) {
        if (mc.thePlayer == null)
            return;

        // Rise style: bottom left, simple format
        String coords = String.format("XYZ: %.0f, %.0f, %.0f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        fontRenderer.drawStringWithShadow(coords, x, y, 0xFFAAAAAA);
    }

    private void drawFPS(int x, int y) {
        int fps = Minecraft.getDebugFPS();
        fontRenderer.drawStringWithShadow("FPS: " + fps, x, y, 0xFFAAAAAA);
    }

    private void drawCPS(int x, int y) {
        long now = System.currentTimeMillis();
        while (!leftClicks.isEmpty() && now - leftClicks.get(0) > 1000)
            leftClicks.remove(0);
        while (!rightClicks.isEmpty() && now - rightClicks.get(0) > 1000)
            rightClicks.remove(0);

        String cps = "CPS: " + leftClicks.size() + " | " + rightClicks.size();
        fontRenderer.drawStringWithShadow(cps, x, y, 0xFFAAAAAA);
    }

    private void drawPing(int x, int y) {
        int ping = 0;
        if (mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) != null)
            ping = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();

        fontRenderer.drawStringWithShadow("Ping: " + ping + "ms", x, y, 0xFFAAAAAA);
    }

    private void drawDirection(int x, int y) {
        float yaw = mc.thePlayer.rotationYaw % 360;
        if (yaw < 0)
            yaw += 360;
        String dir = (yaw >= 315 || yaw < 45) ? "S" : (yaw < 135) ? "W" : (yaw < 225) ? "N" : "E";

        fontRenderer.drawStringWithShadow("Facing: " + dir, x, y, 0xFFAAAAAA);
    }

    private void drawBiome(int x, int y) {
        if (mc.theWorld == null)
            return;

        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
        fontRenderer.drawStringWithShadow("Biome: " + biome, x, y, 0xFFAAAAAA);
    }

    private void drawTime(int x, int y) {
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        fontRenderer.drawStringWithShadow(time, x, y, 0xFFAAAAAA);
    }

    private void drawWatermark(int x, int y) {
        // Stacked logo: Housing / Client in bold

        String line1 = "\u00A7lHousing";
        String line2 = "\u00A7lClient";

        // Draw Line 1 (Housing) - Cyan/Purple Gradient + Glow
        RenderUtils.drawGlowingText(fontRenderer, line1, x, y);

        // Draw Line 2 (Client) - slightly below
        int lineHeight = fontRenderer.FONT_HEIGHT + 1;
        RenderUtils.drawGlowingText(fontRenderer, line2, x, y + lineHeight);
    }

    private void drawVersionInfo(int screenWidth, int screenHeight) {
        // Rise style: Version: X.X.X User: Username (bottom right)
        String version = "Version: 1.0.0";
        String user = "User: " + mc.thePlayer.getName();
        String combined = version + "  " + user;

        int w = fontRenderer.getStringWidth(combined);
        fontRenderer.drawStringWithShadow(combined, screenWidth - w - 2, screenHeight - 10, 0xFF666666);
    }

    public void registerLeftClick() {
        leftClicks.add(System.currentTimeMillis());
    }

    public void registerRightClick() {
        rightClicks.add(System.currentTimeMillis());
    }

    public int getLeftCPS() {
        long n = System.currentTimeMillis();
        while (!leftClicks.isEmpty() && n - leftClicks.get(0) > 1000)
            leftClicks.remove(0);
        return leftClicks.size();
    }

    public int getRightCPS() {
        long n = System.currentTimeMillis();
        while (!rightClicks.isEmpty() && n - rightClicks.get(0) > 1000)
            rightClicks.remove(0);
        return rightClicks.size();
    }
}