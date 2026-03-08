package com.housingclient.module.modules.client;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ColorSetting;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.NumberSetting;

import java.awt.Color;

public class HUDModule extends Module {

    // Watermark Setting
    private final BooleanSetting showWatermark = new BooleanSetting("Watermark", "Show client watermark on HUD", true);

    // Module List Settings
    private final ModeSetting moduleListPos = new ModeSetting("Position", "Module list position", "Top Right",
            "Top Right", "Top Left", "Bottom Right", "Bottom Left");
    private final ModeSetting moduleListSort = new ModeSetting("Sort", "How to sort modules", "Length", "Length",
            "Alphabetical");
    private final BooleanSetting moduleListBackground = new BooleanSetting("Background",
            "Show background behind list", true);
    private final NumberSetting backgroundPadding = new NumberSetting("Background Size",
            "Padding around module text", 3.5, 0.0, 10.0, 0.5);
    private final ColorSetting primaryColor = new ColorSetting("Primary Color", "Main text color",
            new Color(0, 200, 255));
    private final ColorSetting secondaryColor = new ColorSetting("Secondary Color", "Accent color",
            new Color(200, 0, 255));

    public HUDModule() {
        super("Module List", "Display enabled modules on screen", Category.CLIENT, ModuleMode.BOTH);

        addSetting(showWatermark);
        addSetting(moduleListPos);
        addSetting(moduleListSort);
        addSetting(moduleListBackground);
        addSetting(backgroundPadding);
        addSetting(primaryColor);
        addSetting(secondaryColor);

        // Make background padding only visible when background is enabled
        backgroundPadding.setVisibility(() -> moduleListBackground.isEnabled());

        setEnabled(true); // Enabled by default
    }

    public boolean showWatermark() {
        return showWatermark.isEnabled();
    }

    public String getModuleListPos() {
        return moduleListPos.getValue();
    }

    public String getModuleListSort() {
        return moduleListSort.getValue();
    }

    public boolean showModuleListBackground() {
        return moduleListBackground.isEnabled();
    }

    public double getBackgroundPadding() {
        return backgroundPadding.getValue();
    }

    public Color getPrimaryColor() {
        return primaryColor.getValue();
    }

    public Color getSecondaryColor() {
        return secondaryColor.getValue();
    }
}
