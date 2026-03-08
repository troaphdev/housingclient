package com.housingclient.module;

import java.awt.Color;

public enum Category {

    MOVEMENT("Movement", new Color(0, 200, 255), "movement"),
    VISUALS("Visuals", new Color(200, 0, 255), "visuals"),
    COMBAT("Combat", new Color(255, 50, 50), "combat"),
    MISC("Misc", new Color(100, 255, 100), "misc"),
    BUILDING("Building", new Color(255, 165, 0), "building"),
    EXPLOIT("Exploit", new Color(255, 100, 150), "exploit"),
    MISCELLANEOUS("Miscellaneous", new Color(180, 180, 100), "miscellaneous"),
    ITEMS("Items", new Color(255, 215, 0), "items"),
    CLIENT("Client", new Color(150, 150, 150), "client"),
    INSTANT("Instant", new Color(100, 200, 255), "instant"),
    RENDER("Render", new Color(64, 224, 208), "render"),
    QOL("QOL", new Color(0, 150, 255), "qol"),
    MODERATION("Moderation", new Color(255, 69, 0), "moderation");

    private final String displayName;
    private final Color color;
    private final String configName;

    Category(String displayName, Color color, String configName) {
        this.displayName = displayName;
        this.color = color;
        this.configName = configName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getColor() {
        return color;
    }

    public String getConfigName() {
        return configName;
    }

    public int getColorInt() {
        return color.getRGB();
    }
}
