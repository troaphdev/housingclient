package com.housingclient.gui;

import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Modern flat button rendered with code instead of textures.
 * Supports any width/height without artifacts.
 */
public class CustomGuiButton extends GuiButton {

    private static final int BG_COLOR = 0xFF202020;
    private static final int BG_HOVER_COLOR = 0xFF303030;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFFAAAAAA;

    private net.minecraft.client.gui.FontRenderer fontRenderer;

    public CustomGuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    public void setFontRenderer(net.minecraft.client.gui.FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && mouseX < this.xPosition + this.width
                    && mouseY < this.yPosition + this.height;

            int fillColor = hovered ? BG_HOVER_COLOR : BG_COLOR;
            int textColor = this.enabled ? TEXT_COLOR : DISABLED_TEXT_COLOR;

            if (!this.enabled) {
                fillColor = 0xFF151515; // Darker when disabled
            }

            // Draw Background
            RenderUtils.drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 4, fillColor);

            // Draw Border
            RenderUtils.drawRoundedRectOutline(this.xPosition, this.yPosition, this.width, this.height, 4, BORDER_COLOR,
                    1.0f);

            // Draw Text
            net.minecraft.client.gui.FontRenderer font = this.fontRenderer != null ? this.fontRenderer
                    : mc.fontRendererObj;

            if (font instanceof com.housingclient.utils.font.CustomFontRenderer) {
                ((com.housingclient.utils.font.CustomFontRenderer) font).drawCenteredSmoothString(this.displayString,
                        this.xPosition + this.width / 2f,
                        this.yPosition + (this.height - 8) / 2f, textColor, true);
            } else {
                this.drawCenteredString(font, this.displayString, this.xPosition + this.width / 2,
                        this.yPosition + (this.height - 8) / 2, textColor);
            }
        }
    }
}
