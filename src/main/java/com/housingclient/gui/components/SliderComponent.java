package com.housingclient.gui.components;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.module.settings.Setting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.input.Keyboard;

/**
 * Vape V4 style slider with thin track and circular handle
 */
public class SliderComponent extends SettingComponent {

    private final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
    private final NumberSetting setting;

    private static final int HEIGHT = 26;
    private static final int SLIDER_HEIGHT = 4;
    private static final int HANDLE_RADIUS = 5;

    private boolean dragging = false;
    private boolean editingNumber = false;
    private String inputBuffer = "";
    private float hoverAnimation = 0;

    public SliderComponent(NumberSetting setting, Theme theme) {
        super(theme);
        this.setting = setting;
    }

    @Override
    public void draw(int mouseX, int mouseY) {
        // Handle dragging
        if (dragging && !editingNumber) {
            double percent = (mouseX - x) / (double) width;
            percent = Math.max(0, Math.min(1, percent));
            double value = setting.getMin() + (setting.getMax() - setting.getMin()) * percent;
            setting.setValue(value);
        }

        // Update hover animation
        boolean hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1 : 0;
        hoverAnimation += (targetHover - hoverAnimation) * 0.3f;

        // Draw setting name
        String text = setting.getName();
        fontRenderer.drawStringWithShadow(text, x, y, theme.getTextSecondary());

        // Draw value (clickable for editing)
        String valueText;
        boolean showCursor = editingNumber && (System.currentTimeMillis() / 500) % 2 == 0;

        if (editingNumber) {
            valueText = inputBuffer + (showCursor ? "|" : "");
        } else if (setting.isOnlyInt()) {
            valueText = String.valueOf(setting.getIntValue());
        } else {
            valueText = String.format("%.2f", setting.getValue());
        }

        int valueWidth = fontRenderer.getStringWidth(valueText);
        int valueX = x + width - valueWidth;
        int valueColor = editingNumber ? theme.getAccentColor() : theme.getTextSecondary();

        // Draw underline when editing
        if (editingNumber) {
            String displayText = inputBuffer.isEmpty() ? "0" : inputBuffer;
            int underlineWidth = fontRenderer.getStringWidth(displayText) + 4;
            int underlineX = x + width - underlineWidth;
            RenderUtils.drawRect(underlineX, y + 10, underlineWidth, 1, theme.getAccentColor());
        }

        // Check if mouse is over the value for hover effect
        String checkText = setting.isOnlyInt() ? String.valueOf(setting.getIntValue())
                : String.format("%.2f", setting.getValue());
        int checkWidth = fontRenderer.getStringWidth(checkText);
        int checkX = x + width - checkWidth - 2;
        boolean valueHovered = mouseX >= checkX && mouseX <= x + width && mouseY >= y && mouseY < y + 12;

        // Draw underline on hover (when not editing)
        if (valueHovered && !editingNumber) {
            RenderUtils.drawRect(checkX, y + 10, checkWidth, 1, theme.getTextSecondary());
        }

        fontRenderer.drawStringWithShadow(valueText, valueX, y, valueColor);

        // Draw slider track
        int sliderY = y + 14;
        int trackY = sliderY + (HANDLE_RADIUS - SLIDER_HEIGHT / 2);

        // Background track
        RenderUtils.drawPill(x, trackY, width, SLIDER_HEIGHT, theme.getModuleColor());

        // Calculate fill width
        double displayValue = Math.max(setting.getMin(), Math.min(setting.getMax(), setting.getValue()));
        double percent = (displayValue - setting.getMin()) / (setting.getMax() - setting.getMin());
        int fillWidth = (int) (width * percent);

        // Filled portion with gradient feel
        if (fillWidth > 0) {
            RenderUtils.drawPill(x, trackY, Math.min(fillWidth, width), SLIDER_HEIGHT, theme.getAccentColor());
        }

        // Draw handle
        int handleX = x + Math.max(HANDLE_RADIUS, Math.min(width - HANDLE_RADIUS, fillWidth));
        int handleY = sliderY + HANDLE_RADIUS;

        // Handle glow on hover
        if (hoverAnimation > 0.01f) {
            RenderUtils.drawCircle(handleX, handleY, HANDLE_RADIUS + 3 * hoverAnimation,
                    Theme.withAlpha(theme.getAccentColor(), (int) (40 * hoverAnimation)));
        }

        // Handle shadow
        RenderUtils.drawCircle(handleX + 1, handleY + 1, HANDLE_RADIUS, 0x40000000);
        // Handle
        RenderUtils.drawCircle(handleX, handleY, HANDLE_RADIUS, 0xFFFFFFFF);
        // Handle inner accent
        RenderUtils.drawCircle(handleX, handleY, HANDLE_RADIUS - 2, theme.getAccentColor());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            if (editingNumber) {
                applyInput();
            }
            return;
        }

        if (button == 0) {
            // Check if clicking on value area (top right)
            String valueText = setting.isOnlyInt() ? String.valueOf(setting.getIntValue())
                    : String.format("%.2f", setting.getValue());
            int valueWidth = fontRenderer.getStringWidth(valueText);
            int valueX = x + width - valueWidth - 2;

            if (mouseX >= valueX && mouseY < y + 12) {
                // Toggle editing mode
                if (editingNumber) {
                    applyInput();
                } else {
                    editingNumber = true;
                    inputBuffer = "";
                }
            } else if (!editingNumber && mouseY >= y + 10) {
                // Start slider dragging (only on slider area)
                dragging = true;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;
    }

    public boolean keyTyped(int keyCode) {
        if (!editingNumber)
            return false;

        char c = Keyboard.getEventCharacter();

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            applyInput();
            return true;
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            editingNumber = false;
            inputBuffer = "";
            return true;
        } else if (keyCode == Keyboard.KEY_BACK) {
            if (!inputBuffer.isEmpty()) {
                inputBuffer = inputBuffer.substring(0, inputBuffer.length() - 1);
            }
            return true;
        } else if (Character.isDigit(c) || c == '.' || c == '-') {
            if (c == '.' && inputBuffer.contains("."))
                return true;
            if (c == '-' && !inputBuffer.isEmpty())
                return true;

            inputBuffer += c;
            return true;
        }

        return false;
    }

    private void applyInput() {
        editingNumber = false;

        if (inputBuffer.isEmpty()) {
            return;
        }

        try {
            double value = Double.parseDouble(inputBuffer);
            value = Math.round(value * 100.0) / 100.0;
            setting.setValueUnclamped(value);
        } catch (NumberFormatException e) {
            // Invalid input, ignore
        }

        inputBuffer = "";
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

    public boolean isEditing() {
        return editingNumber;
    }
}
