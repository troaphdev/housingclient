package com.housingclient.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.awt.Color;

public class ColorSetting extends Setting<Color> {
    
    private boolean rainbow;
    private float rainbowSpeed = 1.0f;
    
    public ColorSetting(String name, String description, Color defaultValue) {
        super(name, description, defaultValue);
        this.rainbow = false;
    }
    
    public ColorSetting(String name, String description, int r, int g, int b) {
        this(name, description, new Color(r, g, b));
    }
    
    public ColorSetting(String name, String description, int r, int g, int b, int a) {
        this(name, description, new Color(r, g, b, a));
    }
    
    public int getRGB() {
        if (rainbow) {
            return getRainbowColor().getRGB();
        }
        return getValue().getRGB();
    }
    
    public int getRed() {
        return getValue().getRed();
    }
    
    public int getGreen() {
        return getValue().getGreen();
    }
    
    public int getBlue() {
        return getValue().getBlue();
    }
    
    public int getAlpha() {
        return getValue().getAlpha();
    }
    
    public boolean isRainbow() {
        return rainbow;
    }
    
    public void setRainbow(boolean rainbow) {
        this.rainbow = rainbow;
    }
    
    public float getRainbowSpeed() {
        return rainbowSpeed;
    }
    
    public void setRainbowSpeed(float rainbowSpeed) {
        this.rainbowSpeed = rainbowSpeed;
    }
    
    public Color getColor() {
        if (rainbow) {
            return getRainbowColor();
        }
        return getValue();
    }
    
    private Color getRainbowColor() {
        float hue = (System.currentTimeMillis() % (long)(10000 / rainbowSpeed)) / (10000f / rainbowSpeed);
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }
    
    public static Color getRainbowColor(long offset, float speed) {
        float hue = ((System.currentTimeMillis() + offset) % (long)(10000 / speed)) / (10000f / speed);
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }
    
    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("red", getRed());
        json.addProperty("green", getGreen());
        json.addProperty("blue", getBlue());
        json.addProperty("alpha", getAlpha());
        json.addProperty("rainbow", rainbow);
        json.addProperty("rainbowSpeed", rainbowSpeed);
        return json;
    }
    
    @Override
    public void fromJson(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            int r = json.has("red") ? json.get("red").getAsInt() : 255;
            int g = json.has("green") ? json.get("green").getAsInt() : 255;
            int b = json.has("blue") ? json.get("blue").getAsInt() : 255;
            int a = json.has("alpha") ? json.get("alpha").getAsInt() : 255;
            setValue(new Color(r, g, b, a));
            
            if (json.has("rainbow")) {
                rainbow = json.get("rainbow").getAsBoolean();
            }
            if (json.has("rainbowSpeed")) {
                rainbowSpeed = json.get("rainbowSpeed").getAsFloat();
            }
        }
    }
}

