package com.housingclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.housingclient.HousingClient;

import java.awt.Color;
import java.io.*;

public class ConfigManager {
    
    private final File dataDir;
    private final File configFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Global settings
    private int guiKey = 54; // Right Shift
    private Color primaryColor = new Color(0, 200, 255);
    private Color secondaryColor = new Color(200, 0, 255);
    private Color backgroundColor = new Color(20, 20, 30, 220);
    private Color panelColor = new Color(30, 30, 40, 240);
    private boolean rainbowMode = false;
    private float rainbowSpeed = 1.0f;
    
    // Bypass settings
    private int packetDelay = 50;
    private double randomizationFactor = 0.2;
    private boolean humanizedMovement = true;
    
    // HUD settings
    private boolean showModuleList = true;
    private boolean showCoords = true;
    private boolean showCPS = true;
    private boolean showVisitorCount = true;
    private String moduleListPosition = "TOP_RIGHT";
    
    public ConfigManager(File dataDir) {
        this.dataDir = dataDir;
        this.configFile = new File(dataDir, "config.json");
    }
    
    public void loadConfig() {
        if (!configFile.exists()) {
            saveConfig();
            return;
        }
        
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) return;
            
            // GUI settings
            if (json.has("guiKey")) guiKey = json.get("guiKey").getAsInt();
            if (json.has("rainbowMode")) rainbowMode = json.get("rainbowMode").getAsBoolean();
            if (json.has("rainbowSpeed")) rainbowSpeed = json.get("rainbowSpeed").getAsFloat();
            
            // Colors
            if (json.has("primaryColor")) primaryColor = parseColor(json.getAsJsonObject("primaryColor"));
            if (json.has("secondaryColor")) secondaryColor = parseColor(json.getAsJsonObject("secondaryColor"));
            if (json.has("backgroundColor")) backgroundColor = parseColor(json.getAsJsonObject("backgroundColor"));
            if (json.has("panelColor")) panelColor = parseColor(json.getAsJsonObject("panelColor"));
            
            // Bypass settings
            if (json.has("packetDelay")) packetDelay = json.get("packetDelay").getAsInt();
            if (json.has("randomizationFactor")) randomizationFactor = json.get("randomizationFactor").getAsDouble();
            if (json.has("humanizedMovement")) humanizedMovement = json.get("humanizedMovement").getAsBoolean();
            
            // HUD settings
            if (json.has("showModuleList")) showModuleList = json.get("showModuleList").getAsBoolean();
            if (json.has("showCoords")) showCoords = json.get("showCoords").getAsBoolean();
            if (json.has("showCPS")) showCPS = json.get("showCPS").getAsBoolean();
            if (json.has("showVisitorCount")) showVisitorCount = json.get("showVisitorCount").getAsBoolean();
            if (json.has("moduleListPosition")) moduleListPosition = json.get("moduleListPosition").getAsString();
            
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to load config", e);
        }
    }
    
    public void saveConfig() {
        try {
            JsonObject json = new JsonObject();
            
            // GUI settings
            json.addProperty("guiKey", guiKey);
            json.addProperty("rainbowMode", rainbowMode);
            json.addProperty("rainbowSpeed", rainbowSpeed);
            
            // Colors
            json.add("primaryColor", colorToJson(primaryColor));
            json.add("secondaryColor", colorToJson(secondaryColor));
            json.add("backgroundColor", colorToJson(backgroundColor));
            json.add("panelColor", colorToJson(panelColor));
            
            // Bypass settings
            json.addProperty("packetDelay", packetDelay);
            json.addProperty("randomizationFactor", randomizationFactor);
            json.addProperty("humanizedMovement", humanizedMovement);
            
            // HUD settings
            json.addProperty("showModuleList", showModuleList);
            json.addProperty("showCoords", showCoords);
            json.addProperty("showCPS", showCPS);
            json.addProperty("showVisitorCount", showVisitorCount);
            json.addProperty("moduleListPosition", moduleListPosition);
            
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save config", e);
        }
    }
    
    private Color parseColor(JsonObject json) {
        int r = json.has("r") ? json.get("r").getAsInt() : 255;
        int g = json.has("g") ? json.get("g").getAsInt() : 255;
        int b = json.has("b") ? json.get("b").getAsInt() : 255;
        int a = json.has("a") ? json.get("a").getAsInt() : 255;
        return new Color(r, g, b, a);
    }
    
    private JsonObject colorToJson(Color color) {
        JsonObject json = new JsonObject();
        json.addProperty("r", color.getRed());
        json.addProperty("g", color.getGreen());
        json.addProperty("b", color.getBlue());
        json.addProperty("a", color.getAlpha());
        return json;
    }
    
    // Getters and Setters
    public int getGuiKey() { return guiKey; }
    public void setGuiKey(int guiKey) { this.guiKey = guiKey; }
    
    public Color getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(Color color) { this.primaryColor = color; }
    
    public Color getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(Color color) { this.secondaryColor = color; }
    
    public Color getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(Color color) { this.backgroundColor = color; }
    
    public Color getPanelColor() { return panelColor; }
    public void setPanelColor(Color color) { this.panelColor = color; }
    
    public boolean isRainbowMode() { return rainbowMode; }
    public void setRainbowMode(boolean rainbow) { this.rainbowMode = rainbow; }
    
    public float getRainbowSpeed() { return rainbowSpeed; }
    public void setRainbowSpeed(float speed) { this.rainbowSpeed = speed; }
    
    public int getPacketDelay() { return packetDelay; }
    public void setPacketDelay(int delay) { this.packetDelay = delay; }
    
    public double getRandomizationFactor() { return randomizationFactor; }
    public void setRandomizationFactor(double factor) { this.randomizationFactor = factor; }
    
    public boolean isHumanizedMovement() { return humanizedMovement; }
    public void setHumanizedMovement(boolean humanized) { this.humanizedMovement = humanized; }
    
    public boolean isShowModuleList() { return showModuleList; }
    public void setShowModuleList(boolean show) { this.showModuleList = show; }
    
    public boolean isShowCoords() { return showCoords; }
    public void setShowCoords(boolean show) { this.showCoords = show; }
    
    public boolean isShowCPS() { return showCPS; }
    public void setShowCPS(boolean show) { this.showCPS = show; }
    
    public boolean isShowVisitorCount() { return showVisitorCount; }
    public void setShowVisitorCount(boolean show) { this.showVisitorCount = show; }
    
    public String getModuleListPosition() { return moduleListPosition; }
    public void setModuleListPosition(String position) { this.moduleListPosition = position; }
}

