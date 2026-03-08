package com.housingclient.module.modules.client;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ColorSetting;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.NumberSetting;

import java.awt.Color;

public class ClickGUIModule extends Module {

    private final ModeSetting theme = new ModeSetting("Theme", "GUI color theme", "Dark", "Dark", "Light", "Blue",
            "Purple", "Custom");
    private final ColorSetting accentColor = new ColorSetting("Accent Color", "Main accent color",
            new Color(231, 76, 60));
    private final ColorSetting secondaryAccent = new ColorSetting("Secondary Accent", "Secondary accent color",
            new Color(200, 0, 255));
    private final ColorSetting backgroundColor = new ColorSetting("Background", "Panel background color",
            new Color(30, 30, 40, 240));
    private final ColorSetting headerColor = new ColorSetting("Header Color", "Header/footer color",
            new Color(20, 20, 30, 250));

    private final BooleanSetting blur = new BooleanSetting("Blur Background", "Blur the background", true);
    private final NumberSetting blurStrength = new NumberSetting("Blur Strength", "How strong the blur is", 5.0, 1.0,
            20.0, 1.0);
    private final BooleanSetting rainbow = new BooleanSetting("Rainbow Mode", "Cycle colors", false);
    private final NumberSetting rainbowSpeed = new NumberSetting("Rainbow Speed", "How fast colors cycle", 1.0, 0.1,
            5.0, 0.1);

    private final BooleanSetting animations = new BooleanSetting("Animations", "Enable GUI animations", true);
    private final NumberSetting animationSpeed = new NumberSetting("Animation Speed", "Speed of animations", 1.0, 0.5,
            3.0, 0.1);

    private final NumberSetting panelWidth = new NumberSetting("Panel Width", "Width of category panels", 150, 120,
            200);
    private final NumberSetting panelSpacing = new NumberSetting("Panel Spacing", "Space between panels", 8, 2, 20);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners", "Use rounded corners", true);
    private final NumberSetting cornerRadius = new NumberSetting("Corner Radius", "Radius of rounded corners", 5, 2,
            15);

    private final BooleanSetting tooltips = new BooleanSetting("Tooltips", "Show module descriptions", true);
    private final BooleanSetting sounds = new BooleanSetting("Sounds", "Play sounds on click", false);
    private final BooleanSetting snapToGrid = new BooleanSetting("Snap to Grid", "Snap panels to grid", false);
    private final BooleanSetting legacyMode = new BooleanSetting("Legacy Mode", "Use Old ClickGUI", false);
    private final BooleanSetting notifications = new BooleanSetting("Notifications", "Show toggle notifications", true);
    private final BooleanSetting blatantMode = new BooleanSetting("Blatant Mode", "Enable risky modules", false);

    public ClickGUIModule() {
        super("ClickGUI", "Customize the GUI appearance", Category.CLIENT, ModuleMode.BOTH);

        addSetting(theme);
        addSetting(accentColor);
        addSetting(secondaryAccent);
        addSetting(backgroundColor);
        addSetting(headerColor);
        addSetting(blur);
        addSetting(blurStrength);
        addSetting(rainbow);
        addSetting(rainbowSpeed);
        addSetting(animations);
        addSetting(animationSpeed);
        addSetting(panelWidth);
        addSetting(panelSpacing);
        addSetting(roundedCorners);
        addSetting(cornerRadius);
        addSetting(tooltips);
        addSetting(sounds);
        addSetting(snapToGrid);

        // New Settings for Private
        addSetting(legacyMode);
        addSetting(notifications);
        addSetting(blatantMode);

        setEnabled(true);
    }

    // Getters
    public String getTheme() {
        return theme.getValue();
    }

    public Color getAccentColor() {
        if (rainbow.isEnabled()) {
            float hue = (System.currentTimeMillis() % (int) (10000 / rainbowSpeed.getValue()))
                    / (float) (10000 / rainbowSpeed.getValue());
            return Color.getHSBColor(hue, 0.8f, 1.0f);
        }
        return accentColor.getValue();
    }

    public Color getSecondaryAccent() {
        return secondaryAccent.getValue();
    }

    public Color getBackgroundColor() {
        return backgroundColor.getValue();
    }

    public Color getHeaderColor() {
        return headerColor.getValue();
    }

    public boolean isBlurEnabled() {
        return blur.isEnabled();
    }

    public double getBlurStrength() {
        return blurStrength.getValue();
    }

    public boolean isRainbowEnabled() {
        return rainbow.isEnabled();
    }

    public double getRainbowSpeed() {
        return rainbowSpeed.getValue();
    }

    public boolean isAnimationsEnabled() {
        return animations.isEnabled();
    }

    public double getAnimationSpeed() {
        return animationSpeed.getValue();
    }

    public int getPanelWidth() {
        return panelWidth.getIntValue();
    }

    public int getPanelSpacing() {
        return panelSpacing.getIntValue();
    }

    public boolean isRoundedCorners() {
        return roundedCorners.isEnabled();
    }

    public int getCornerRadius() {
        return cornerRadius.getIntValue();
    }

    public boolean isTooltipsEnabled() {
        return tooltips.isEnabled();
    }

    public boolean isSoundsEnabled() {
        return sounds.isEnabled();
    }

    public boolean isLegacyMode() {
        return legacyMode.isEnabled();
    }

    public boolean isNotificationsEnabled() {
        return notifications.isEnabled();
    }

    public boolean isBlatantModeEnabled() {
        return blatantMode.isEnabled();
    }
}
