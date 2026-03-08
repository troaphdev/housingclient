package com.housingclient.module.modules.client;

import com.housingclient.HousingClient;
import com.housingclient.gui.HudEditorGUI;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * HUD Designer Module - INSTANT type
 * 
 * This module is not togglable - clicking it opens the HUD Editor GUI
 * where users can drag HUD elements to new positions.
 */
public class HudDesignerModule extends Module {

    // Use larger ranges to fix "middle of screen" glitch
    private final NumberSetting watermarkX = new NumberSetting("Watermark X", "X position", 4, 0, 3000);
    private final NumberSetting watermarkY = new NumberSetting("Watermark Y", "Y position", 5, 0, 2000);
    private final NumberSetting moduleListX = new NumberSetting("ModuleList X", "X position", 957, 0, 3000);
    private final NumberSetting moduleListY = new NumberSetting("ModuleList Y", "Y position", 2, 0, 2000);
    private final NumberSetting coordsX = new NumberSetting("Coords X", "X position", 6, 0, 3000);
    private final NumberSetting coordsY = new NumberSetting("Coords Y", "Y position", 368, -2000, 2000);

    private final NumberSetting fpsX = new NumberSetting("FPS X", "X position", 6, 0, 3000);
    private final NumberSetting fpsY = new NumberSetting("FPS Y", "Y position", 306, 0, 2000);

    private final NumberSetting cpsX = new NumberSetting("CPS X", "X position", 5, 0, 3000);
    private final NumberSetting cpsY = new NumberSetting("CPS Y", "Y position", 327, 0, 2000);

    private final NumberSetting pingX = new NumberSetting("Ping X", "X position", 6, 0, 3000);
    private final NumberSetting pingY = new NumberSetting("Ping Y", "Y position", 387, 0, 2000);

    private final NumberSetting dirX = new NumberSetting("Dir X", "X position", 400, 0, 3000);
    private final NumberSetting dirY = new NumberSetting("Dir Y", "Y position", 21, 0, 2000);

    private final NumberSetting biomeX = new NumberSetting("Biome X", "X position", 5, 0, 3000);
    private final NumberSetting biomeY = new NumberSetting("Biome Y", "Y position", 348, 0, 2000);

    private final NumberSetting timeX = new NumberSetting("Time X", "X position", 6, 0, 3000);
    private final NumberSetting timeY = new NumberSetting("Time Y", "Y position", 265, 0, 2000);

    private final NumberSetting tpsX = new NumberSetting("TPS X", "X position", 6, 0, 3000);
    private final NumberSetting tpsY = new NumberSetting("TPS Y", "Y position", 285, 0, 2000);

    private final NumberSetting loadedPlayersX = new NumberSetting("LoadedPlayers X", "X position", 5, 0, 3000);
    private final NumberSetting loadedPlayersY = new NumberSetting("LoadedPlayers Y", "Y position", 50, 0, 2000);

    private final NumberSetting scoreboardX = new NumberSetting("Scoreboard X", "X position (-1 = default)", 856, -1,
            3000);
    private final NumberSetting scoreboardY = new NumberSetting("Scoreboard Y", "Y position (-1 = default)", 209, -1,
            2000);

    private final NumberSetting blinkTimerX = new NumberSetting("BlinkTimer X", "X position", 100, 0, 3000);
    private final NumberSetting blinkTimerY = new NumberSetting("BlinkTimer Y", "Y position", 200, 0, 2000);

    private final NumberSetting activeEffectsX = new NumberSetting("ActiveEffects X", "X position", 10, 0, 3000);
    private final NumberSetting activeEffectsY = new NumberSetting("ActiveEffects Y", "Y position", 100, 0, 2000);

    private final NumberSetting hiddenEntitiesX = new NumberSetting("HiddenEntities X", "X position", 5, 0, 3000);
    private final NumberSetting hiddenEntitiesY = new NumberSetting("HiddenEntities Y", "Y position", 408, 0, 2000);

    public HudDesignerModule() {
        super("HUD Designer", "Click to open HUD editor (drag elements)", Category.CLIENT, ModuleMode.BOTH);

        addSetting(watermarkX);
        addSetting(watermarkY);
        addSetting(moduleListX);
        addSetting(moduleListY);
        addSetting(coordsX);
        addSetting(coordsY);

        addSetting(fpsX);
        addSetting(fpsY);
        addSetting(cpsX);
        addSetting(cpsY);
        addSetting(pingX);
        addSetting(pingY);
        addSetting(dirX);
        addSetting(dirY);
        addSetting(biomeX);
        addSetting(biomeY);
        addSetting(timeX);
        addSetting(timeY);
        addSetting(tpsX);
        addSetting(tpsY);
        addSetting(loadedPlayersX);
        addSetting(loadedPlayersY);
        addSetting(scoreboardX);
        addSetting(scoreboardY);
        addSetting(blinkTimerX);
        addSetting(blinkTimerY);
        addSetting(activeEffectsX);
        addSetting(activeEffectsY);
        addSetting(hiddenEntitiesX);
        addSetting(hiddenEntitiesY);
    }

    // Getters
    public int getFpsX() {
        return fpsX.getIntValue();
    }

    public int getFpsY() {
        return fpsY.getIntValue();
    }

    public void setFpsPosition(int x, int y) {
        fpsX.setValue((double) x);
        fpsY.setValue((double) y);
    }

    public int getCpsX() {
        return cpsX.getIntValue();
    }

    public int getCpsY() {
        return cpsY.getIntValue();
    }

    public void setCpsPosition(int x, int y) {
        cpsX.setValue((double) x);
        cpsY.setValue((double) y);
    }

    public int getPingX() {
        return pingX.getIntValue();
    }

    public int getPingY() {
        return pingY.getIntValue();
    }

    public void setPingPosition(int x, int y) {
        pingX.setValue((double) x);
        pingY.setValue((double) y);
    }

    public int getDirX() {
        return dirX.getIntValue();
    }

    public int getDirY() {
        return dirY.getIntValue();
    }

    public void setDirPosition(int x, int y) {
        dirX.setValue((double) x);
        dirY.setValue((double) y);
    }

    public int getBiomeX() {
        return biomeX.getIntValue();
    }

    public int getBiomeY() {
        return biomeY.getIntValue();
    }

    public void setBiomePosition(int x, int y) {
        biomeX.setValue((double) x);
        biomeY.setValue((double) y);
    }

    // Time defaults to right side, might need layout logic in HUD
    public int getTimeX() {
        return timeX.getIntValue();
    }

    public int getTimeY() {
        return timeY.getIntValue();
    }

    public void setTimePosition(int x, int y) {
        timeX.setValue((double) x);
        timeY.setValue((double) y);
    }

    // TPS getters/setters
    public int getTpsX() {
        return tpsX.getIntValue();
    }

    public int getTpsY() {
        return tpsY.getIntValue();
    }

    public void setTpsPosition(int x, int y) {
        tpsX.setValue((double) x);
        tpsY.setValue((double) y);
    }

    // Loaded Players getters/setters
    public int getLoadedPlayersX() {
        return loadedPlayersX.getIntValue();
    }

    public int getLoadedPlayersY() {
        return loadedPlayersY.getIntValue();
    }

    public void setLoadedPlayersPosition(int x, int y) {
        loadedPlayersX.setValue((double) x);
        loadedPlayersY.setValue((double) y);
    }

    // Scoreboard getters/setters
    public int getScoreboardX() {
        return scoreboardX.getIntValue();
    }

    public int getScoreboardY() {
        return scoreboardY.getIntValue();
    }

    public void setScoreboardPosition(int x, int y) {
        scoreboardX.setValue((double) x);
        scoreboardY.setValue((double) y);
    }

    // Blink Timer
    public int getBlinkTimerX() {
        return blinkTimerX.getIntValue();
    }

    public int getBlinkTimerY() {
        return blinkTimerY.getIntValue();
    }

    public void setBlinkTimerPosition(int x, int y) {
        blinkTimerX.setValue((double) x);
        blinkTimerY.setValue((double) y);
    }

    // Active Effects
    public int getActiveEffectsX() {
        return activeEffectsX.getIntValue();
    }

    public int getActiveEffectsY() {
        return activeEffectsY.getIntValue();
    }

    public void setActiveEffectsPosition(int x, int y) {
        activeEffectsX.setValue((double) x);
        activeEffectsY.setValue((double) y);
    }

    // Hidden Entities getters/setters
    public int getHiddenEntitiesX() {
        return hiddenEntitiesX.getIntValue();
    }

    public int getHiddenEntitiesY() {
        return hiddenEntitiesY.getIntValue();
    }

    public void setHiddenEntitiesPosition(int x, int y) {
        hiddenEntitiesX.setValue((double) x);
        hiddenEntitiesY.setValue((double) y);
    }

    /**
     * Override toggle to open GUI instead of toggling on/off
     */
    @Override
    public void toggle() {
        // Don't toggle - just open the GUI
        openHudEditor();
    }

    /**
     * Override setEnabled to prevent enabling - this is an instant action
     */
    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            openHudEditor();
        }
        // Never actually enable this module
    }

    private void openHudEditor() {
        if (mc.theWorld != null) {
            mc.displayGuiScreen(new HudEditorGUI(this));
        }
    }

    /**
     * Mark this as an instant module (not togglable)
     */
    public boolean isInstant() {
        return true;
    }

    // Position getters for HUD elements
    public int getWatermarkX() {
        return watermarkX.getIntValue();
    }

    public int getWatermarkY() {
        return watermarkY.getIntValue();
    }

    public int getModuleListX(int screenWidth) {
        int x = moduleListX.getIntValue();
        if (x < 0) {
            return 2; // Fixed default position
        }
        return x;
    }

    public int getModuleListY() {
        return moduleListY.getIntValue();
    }

    public int getCoordsX() {
        return coordsX.getIntValue();
    }

    public int getCoordsY(int screenHeight) {
        int y = coordsY.getIntValue();
        if (y < 0) {
            return screenHeight + y;
        }
        return y;
    }

    public boolean isModuleListRightAligned() {
        return moduleListX.getIntValue() < 0;
    }

    // Setters for HUD Editor to update positions
    public void setWatermarkPosition(int x, int y) {
        watermarkX.setValue((double) x);
        watermarkY.setValue((double) y);
    }

    public void setModuleListPosition(int x, int y) {
        moduleListX.setValue((double) x);
        moduleListY.setValue((double) y);
    }

    public void setCoordsPosition(int x, int y) {
        coordsX.setValue((double) x);
        coordsY.setValue((double) y);
    }
}
