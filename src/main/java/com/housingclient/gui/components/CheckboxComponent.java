package com.housingclient.gui.components;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.Setting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;

/**
 * Vape V4 style toggle switch (pill-shaped)
 */
public class CheckboxComponent extends SettingComponent {

    private final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
    private final BooleanSetting setting;

    private static final int HEIGHT = 18;
    private static final int TOGGLE_WIDTH = 28;
    private static final int TOGGLE_HEIGHT = 14;

    private float animation = 0;

    public CheckboxComponent(BooleanSetting setting, Theme theme) {
        super(theme);
        this.setting = setting;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        // Update animation with smooth easing
        float target = setting.isEnabled() ? 1 : 0;
        animation += (target - animation) * 0.25f;

        // Draw setting name
        fontRenderer.drawStringWithShadow(setting.getName(), x, y + 4, theme.getTextSecondary());

        // Draw toggle switch (pill-shaped)
        int toggleX = x + width - TOGGLE_WIDTH - 2;
        int toggleY = y + 2;

        // Background track - blend between off and on colors
        int offColor = theme.getModuleColor();
        int onColor = theme.getAccentColor();
        int trackColor = Theme.blend(offColor, onColor, animation);

        RenderUtils.drawPill(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT, trackColor);

        // Draw subtle border
        if (animation < 0.5f) {
            RenderUtils.drawRoundedBorderedRect(toggleX, toggleY, TOGGLE_WIDTH, TOGGLE_HEIGHT,
                    TOGGLE_HEIGHT / 2, 0x00000000, Theme.withAlpha(0xFFFFFFFF, 30), 1);
        }

        // Sliding circle indicator
        float circleRadius = (TOGGLE_HEIGHT - 4) / 2.0f;
        float circleMinX = toggleX + circleRadius + 2;
        float circleMaxX = toggleX + TOGGLE_WIDTH - circleRadius - 2;
        float circleX = circleMinX + (circleMaxX - circleMinX) * animation;
        float circleY = toggleY + TOGGLE_HEIGHT / 2.0f;

        // Circle with slight shadow
        RenderUtils.drawCircle(circleX + 1, circleY + 1, circleRadius, 0x40000000);
        RenderUtils.drawCircle(circleX, circleY, circleRadius, 0xFFFFFFFF);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isMouseOver(mouseX, mouseY)) {
            setting.toggle();
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + HEIGHT;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Setting<?> getSetting() {
        return setting;
    }
}
