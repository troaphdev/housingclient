package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleManager;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.modules.client.HUDModule;

import com.housingclient.module.modules.visuals.*;
import com.housingclient.module.modules.exploit.BlinkModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * HUD Editor GUI
 * Allows users to drag HUD elements to new positions.
 * Only shows elements for ENABLED modules.
 */
public class HudEditorGUI extends GuiScreen {

    private final HudDesignerModule hudDesigner;
    private final ModuleManager moduleManager;

    private List<HudElement> elements = new ArrayList<>();
    private HudElement draggedElement = null;
    private int dragOffsetX, dragOffsetY;

    public HudEditorGUI(HudDesignerModule hudDesigner) {
        this.hudDesigner = hudDesigner;
        this.moduleManager = HousingClient.getInstance().getModuleManager();
    }

    @Override
    public void initGui() {
        super.initGui();
        refreshElements();
    }

    private void refreshElements() {
        elements.clear();

        ScaledResolution sr = new ScaledResolution(mc);
        int screenWidth = sr.getScaledWidth();
        int screenHeight = sr.getScaledHeight();

        // Module List - always shown when HUDModule (Module List) is enabled
        HUDModule hudModule = moduleManager.getModule(HUDModule.class);
        if (hudModule != null && hudModule.isEnabled()) {
            // Pass the module instance so it picks up the dynamic width/height
            // Sizing logic in drawScreen will handle updates
            elements.add(new HudElement("Module List", hudDesigner.getModuleListX(screenWidth),
                    hudDesigner.getModuleListY(), 100, 100, "moduleList", hudModule));
        }

        // Standard Modules with dynamic sizing support
        addModuleElementIfEnabled(CoordsModule.class, "Coords", hudDesigner.getCoordsX(),
                hudDesigner.getCoordsY(screenHeight), 100, 15, "coords");
        addModuleElementIfEnabled(FPSModule.class, "FPS", hudDesigner.getFpsX(), hudDesigner.getFpsY(), 40, 15, "fps");
        addModuleElementIfEnabled(CPSModule.class, "CPS", hudDesigner.getCpsX(), hudDesigner.getCpsY(), 60, 15, "cps");
        addModuleElementIfEnabled(PingModule.class, "Ping", hudDesigner.getPingX(), hudDesigner.getPingY(), 50, 15,
                "ping");
        addModuleElementIfEnabled(DirectionModule.class, "Direction", hudDesigner.getDirX(), hudDesigner.getDirY(), 160,
                18, "dir");
        addModuleElementIfEnabled(BiomeModule.class, "Biome", hudDesigner.getBiomeX(), hudDesigner.getBiomeY(), 70, 15,
                "biome");
        addModuleElementIfEnabled(ClockModule.class, "Time", hudDesigner.getTimeX(), hudDesigner.getTimeY(), 50, 15,
                "time");
        addModuleElementIfEnabled(TPSModule.class, "TPS", hudDesigner.getTpsX(), hudDesigner.getTpsY(), 50, 15, "tps");
        addModuleElementIfEnabled(LoadedPlayersModule.class, "Loaded Players", hudDesigner.getLoadedPlayersX(),
                hudDesigner.getLoadedPlayersY(), 100, 15, "loadedPlayers");
        addModuleElementIfEnabled(ActiveEffectsModule.class, "Active Effects", hudDesigner.getActiveEffectsX(),
                hudDesigner.getActiveEffectsY(), 100, 50, "activeEffects");
        addModuleElementIfEnabled(HideEntitiesModule.class, "Entities Hidden", hudDesigner.getHiddenEntitiesX(),
                hudDesigner.getHiddenEntitiesY(), 100, 15, "hiddenEntities");

        // Scoreboard - always visible (it's vanilla Minecraft functionality)
        // Uses -1 as default which means right side of screen
        int sbX = hudDesigner.getScoreboardX();
        int sbY = hudDesigner.getScoreboardY();
        if (sbX < 0)
            sbX = screenWidth - 100; // Default to right side
        if (sbY < 0)
            sbY = screenHeight / 2 - 50; // Default to middle-ish height
        elements.add(new HudElement("Scoreboard", sbX, sbY, 100, 100, "scoreboard", null));

        // Blink Timer - special handling to show even if module disabled, if setting is
        // enabled
        BlinkModule blink = moduleManager.getModule(BlinkModule.class);
        if (blink != null && blink.isDisplayTimerEnabled()) {
            elements.add(new HudElement("Blink Timer", hudDesigner.getBlinkTimerX(), hudDesigner.getBlinkTimerY(),
                    60, 15, "blinkTimer", blink));
        }
    }

    private void addModuleElementIfEnabled(Class<? extends Module> clazz, String name, int x, int y, int defaultW,
            int defaultH, String id) {
        Module m = moduleManager.getModule(clazz);
        if (m != null && m.isEnabled()) {
            int w = m.getWidth() > 0 ? m.getWidth() : defaultW;
            int h = m.getHeight() > 0 ? m.getHeight() : defaultH;
            elements.add(new HudElement(name, x, y, w, h, id, m));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Handle dragging in drawScreen for smoother updates (render rate instead of
        // input rate)
        if (draggedElement != null) {
            if (org.lwjgl.input.Mouse.isButtonDown(0)) {
                draggedElement.x = mouseX - dragOffsetX;
                draggedElement.y = mouseY - dragOffsetY;

                // Clamp to screen (allow touching edges exactly - no margin)
                draggedElement.x = Math.max(0, Math.min(width, draggedElement.x));
                draggedElement.y = Math.max(0, Math.min(height, draggedElement.y));

                // REAL-TIME UPDATE: Write to module immediately
                updateModulePosition(draggedElement);
            } else {
                // Button released but event missed?
                draggedElement = null;
            }
        }

        // Transparent background
        drawRect(0, 0, width, height, new Color(0, 0, 0, 100).getRGB());

        // Draw instructions
        // Draw instructions (Reduced)
        // drawCenteredString(fontRendererObj, "\u00A7b\u00A7lHUD Editor", width / 2,
        // 10, -1);

        if (elements.isEmpty()) {
            drawCenteredString(fontRendererObj, "\u00A7cNo HUD modules are enabled!", width / 2, height / 2, -1);
            drawCenteredString(fontRendererObj, "\u00A77Enable modules in the Click GUI first", width / 2,
                    height / 2 + 15, -1);
        }

        // Draw elements
        for (HudElement element : elements) {
            // DYNAMIC SIZING CHECK
            if (element.module != null) {
                int newWidth = element.module.getWidth();
                int newHeight = element.module.getHeight();

                // Only update if valid size returned
                if (newWidth > 0)
                    element.width = newWidth;
                if (newHeight > 0)
                    element.height = newHeight;
            }

            int drawX = element.x;

            // Special handling for ModuleList if it's right-aligned
            // In HUDModule, if alignment is Right, the X passed here is likely the Anchor.
            // Since we don't have easy access to the alignment enum here without
            // casting/fetching,
            // we'll infer: if x > screenWidth / 2, treat X as right anchor.
            if ("moduleList".equals(element.id)) {
                ScaledResolution sr = new ScaledResolution(mc);
                if (element.x > sr.getScaledWidth() / 2) {
                    drawX = element.x - element.width;
                }
            }

            drawElement(element, drawX, element.y, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int getRenderX(HudElement element) {
        // Special handling for ModuleList if it's right-aligned (inferred from
        // position)
        if ("moduleList".equals(element.id)) {
            ScaledResolution sr = new ScaledResolution(mc);
            // If X is on the right half, treat it as the right anchor
            if (element.x > sr.getScaledWidth() / 2) {
                return element.x - element.width;
            }
        }
        return element.x;
    }

    private void drawElement(HudElement element, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(x, y, element.width, element.height, mouseX, mouseY);
        boolean dragging = draggedElement == element;

        // No filled background, just outline
        int borderColor = dragging ? new Color(0, 255, 0, 255).getRGB() // Bright Green dragging
                : hovered ? new Color(255, 255, 255, 200).getRGB() // White hover
                        : new Color(200, 200, 200, 100).getRGB(); // Gray default

        // Draw border
        drawHorizontalLine(x, x + element.width - 1, y, borderColor);
        drawHorizontalLine(x, x + element.width - 1, y + element.height - 1, borderColor);
        drawVerticalLine(x, y, y + element.height - 1, borderColor);
        drawVerticalLine(x + element.width - 1, y, y + element.height - 1, borderColor);

        // Draw label centered in the box
        drawCenteredString(fontRendererObj, element.name,
                x + element.width / 2,
                y + (element.height - 8) / 2,
                -1);
    }

    private boolean isMouseOver(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private boolean isMouseOver(HudElement element, int mouseX, int mouseY) {
        int renderX = getRenderX(element);
        return isMouseOver(renderX, element.y, element.width, element.height, mouseX, mouseY);
    }

    // Legacy signature for other calls just in case
    private void drawElement(HudElement element, int mouseX, int mouseY) {
        drawElement(element, element.x, element.y, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            // Check elements in reverse order so top-most is clicked first if overlapping
            for (int i = elements.size() - 1; i >= 0; i--) {
                HudElement el = elements.get(i);
                if (isMouseOver(el, mouseX, mouseY)) {
                    draggedElement = el;
                    dragOffsetX = mouseX - el.x;
                    dragOffsetY = mouseY - el.y;
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggedElement = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        // Logic moved to drawScreen for smoothness
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    // Updates the actual module setting so it moves on screen instantly
    private void updateModulePosition(HudElement element) {
        switch (element.id) {
            case "watermark":
                hudDesigner.setWatermarkPosition(element.x, element.y);
                break;
            case "moduleList":
                hudDesigner.setModuleListPosition(element.x, element.y);
                break;
            case "coords":
                hudDesigner.setCoordsPosition(element.x, element.y);
                break;
            case "fps":
                hudDesigner.setFpsPosition(element.x, element.y);
                break;
            case "cps":
                hudDesigner.setCpsPosition(element.x, element.y);
                break;
            case "ping":
                hudDesigner.setPingPosition(element.x, element.y);
                break;
            case "dir":
                hudDesigner.setDirPosition(element.x, element.y);
                break;
            case "biome":
                hudDesigner.setBiomePosition(element.x, element.y);
                break;
            case "time":
                hudDesigner.setTimePosition(element.x, element.y);
                break;
            case "tps":
                hudDesigner.setTpsPosition(element.x, element.y);
                break;
            case "loadedPlayers":
                hudDesigner.setLoadedPlayersPosition(element.x, element.y);
                break;
            case "scoreboard":
                hudDesigner.setScoreboardPosition(element.x, element.y);
                break;
            case "blinkTimer":
                hudDesigner.setBlinkTimerPosition(element.x, element.y);
                break;
            case "activeEffects":
                hudDesigner.setActiveEffectsPosition(element.x, element.y);
                break;
            case "hiddenEntities":
                hudDesigner.setHiddenEntitiesPosition(element.x, element.y);
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            savePositions();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        savePositions();
        super.onGuiClosed();
    }

    private void savePositions() {
        // Final save to disk
        HousingClient.getInstance().getModuleManager().saveModuleStates();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static class HudElement {
        String name;
        String id;
        int x, y, width, height;
        Module module;

        HudElement(String name, int x, int y, int width, int height, String id, Module module) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.id = id;
            this.module = module;
        }
    }
}