package com.housingclient.utils.font;

import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Custom Font Renderer - Renders TrueType fonts in Minecraft
 */
public class CustomFontRenderer extends FontRenderer {
    private final Font font;
    private final boolean antiAlias;
    private final boolean fractionalMetrics;
    private final CharacterData[] charData = new CharacterData[256];
    private DynamicTexture tex;
    private float fontHeight = -1;
    private int[] colorCode = new int[32];

    public CustomFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(Minecraft.getMinecraft().gameSettings, new ResourceLocation("textures/font/ascii.png"),
                Minecraft.getMinecraft().getTextureManager(), false);
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.tex = setupTexture(font, antiAlias, fractionalMetrics, this.charData);
        setupColorCodes();
    }

    private DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics,
            CharacterData[] chars) {
        System.out.println("CustomFontRenderer.setupTexture: Generating font texture for " + font.getName());
        BufferedImage imgTemp = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) imgTemp.getGraphics();
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics();

        this.fontHeight = fontMetrics.getHeight();

        int imgSize = 512;
        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        g2.setFont(font);
        g2.setColor(new Color(255, 255, 255, 0));
        g2.fillRect(0, 0, imgSize, imgSize);
        g2.setColor(Color.WHITE);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                        : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);

        int rowHeight = 0;
        int positionX = 0;
        int positionY = 0;

        for (int i = 0; i < 256; i++) {
            char ch = (char) i;
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g2);

            CharacterData charData = new CharacterData();
            charData.width = dimensions.getBounds().width + 8; // Padding
            charData.height = dimensions.getBounds().height;

            if (positionX + charData.width >= imgSize) {
                positionX = 0;
                positionY += rowHeight;
                rowHeight = 0;
            }

            charData.storedX = positionX;
            charData.storedY = positionY;

            if (charData.height > rowHeight) {
                rowHeight = charData.height;
            }

            chars[i] = charData;
            g2.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += charData.width;
        }

        return new DynamicTexture(img);
    }

    private void setupColorCodes() {
        for (int i = 0; i < 32; i++) {
            int no = (i >> 3 & 0x1) * 85;
            int red = (i >> 2 & 0x1) * 170 + no;
            int green = (i >> 1 & 0x1) * 170 + no;
            int blue = (i >> 0 & 0x1) * 170 + no;

            if (i == 6) {
                red += 85;
            }

            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCode[i] = (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
        }
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (text == null)
            return 0;

        x -= 1;
        if (color == 0x20FFFFFF) {
            color = 0xFFFFFF; // Fix for weird transparency issues
        }

        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }

        if (dropShadow) {
            int shadowColor = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
            drawText(text, x + 0.5f, y + 0.5f, shadowColor, true);
        }

        return drawText(text, x, y, color, false);
    }

    /**
     * Explicit Method to bypass override/obfuscation issues
     */
    public int drawSmoothString(String text, float x, float y, int color, boolean shadow) {
        // System.out.println("CustomFontRenderer.drawSmoothString called for: " +
        // text);
        return drawText(text, x, y, color, shadow);
    }

    public void drawCenteredSmoothString(String text, float x, float y, int color, boolean shadow) {
        try {
            int width = getStringWidth(text);
            drawSmoothString(text, x - width / 2f, y, color, shadow);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Wrap standard draw calls
    @Override
    public int drawStringWithShadow(String text, float x, float y, int color) {
        return drawString(text, x, y, color, true);
    }

    @Override
    public int drawString(String text, int x, int y, int color) {
        return drawString(text, (float) x, (float) y, color, false);
    }

    @Override
    public int getStringWidth(String text) {
        if (text == null)
            return 0;
        int width = 0;
        char[] currentData = text.toCharArray();

        for (int i = 0; i < currentData.length; i++) {
            char c = currentData[i];
            if (c < charData.length) {
                if (c == '\u00A7' && i + 1 < currentData.length) {
                    i++; // Skip color code
                } else {
                    width += charData[c].width - 8 + 2; // Subtract padding, add spacing
                }
            }
        }
        return width / 2;
    }

    @Override
    public int getCharWidth(char c) {
        return getStringWidth(String.valueOf(c));
    }

    private int drawText(String text, float x, float y, int color, boolean shadow) {
        if (text == null || text.isEmpty())
            return 0;

        // DEBUG: Log the call
        System.out.println("CustomFontRenderer.drawText: '" + text + "' at " + x + ", " + y);

        // Ensure blend is enabled for anti-aliased font
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // CRITICAL: Disable Culling AND Depth Test
        GlStateManager.disableCull();
        GlStateManager.disableDepth();

        float alpha = (float) (color >> 24 & 0xFF) / 255.0F;
        float red = (float) (color >> 16 & 0xFF) / 255.0F;
        float green = (float) (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        x *= 2.0;
        y *= 2.0; // Determine scale
        y -= 2.0;

        GL11.glPushMatrix();
        GL11.glScaled(0.5, 0.5, 0.5);

        // DEBUG: Color values
        // System.out.println(String.format("CustomFontRenderer.drawText Color: R=%.2f
        // G=%.2f B=%.2f A=%.2f", red, green, blue, alpha));

        // Restore State for Text
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(tex.getGlTextureId());

        // Ensure proper filtering for smooth font
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

        GlStateManager.color(red, green, blue, alpha);

        boolean bold = false;
        boolean italic = false;
        boolean strikeThrough = false;
        boolean underline = false;

        int originalColor = color;

        char[] characters = text.toCharArray();

        for (int i = 0; i < characters.length; i++) {
            char c = characters[i];

            if (c == '\u00A7' && i + 1 < characters.length) {
                int colorIndex = "0123456789abcdefklmnor".indexOf(text.toLowerCase().charAt(i + 1));
                if (colorIndex < 16) {
                    bold = false;
                    italic = false;
                    strikeThrough = false;
                    underline = false;

                    if (colorIndex < 0)
                        colorIndex = 15;

                    if (shadow)
                        colorIndex += 16;

                    int colorCode = this.colorCode[colorIndex];
                    GlStateManager.color((float) (colorCode >> 16) / 255.0F, (float) (colorCode >> 8 & 255) / 255.0F,
                            (float) (colorCode & 255) / 255.0F, alpha);
                } else if (colorIndex == 17) { // bold
                    bold = true;
                } else if (colorIndex == 18) { // strike
                    strikeThrough = true;
                } else if (colorIndex == 19) { // underline
                    underline = true;
                } else if (colorIndex == 20) { // italic
                    italic = true;
                } else if (colorIndex == 21) { // reset
                    bold = false;
                    italic = false;
                    strikeThrough = false;
                    underline = false;
                    GlStateManager.color(red, green, blue, alpha);
                }

                i++;
            } else if (c < charData.length) {
                CharacterData data = charData[c];

                drawChar(data, x, y);

                if (bold) {
                    drawChar(data, x + 1, y);
                }

                if (strikeThrough) {
                    RenderUtils.draw2DLine(x, y + data.height / 2, x + data.width - 8, y + data.height / 2, -1, 1.0f);
                }
                if (underline) {
                    RenderUtils.draw2DLine(x, y + data.height - 2, x + data.width - 8, y + data.height - 2, -1, 1.0f);
                }

                x += (data.width - 8 + 2);
            }
        }

        GL11.glPopMatrix();

        // Restore defaults
        GlStateManager.enableDepth();
        GlStateManager.enableCull();

        return (int) x / 2;
    }

    private void drawChar(CharacterData data, float x, float y) {
        try {
            float drawX = x;
            float drawY = y;
            float texX = (float) data.storedX / 512.0f;
            float texY = (float) data.storedY / 512.0f;
            float texWidth = (float) data.width / 512.0f;
            float texHeight = (float) data.height / 512.0f;
            float width = (float) data.width;
            float height = (float) data.height;

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(texX, texY);
            GL11.glVertex2f(drawX, drawY);

            GL11.glTexCoord2f(texX, texY + texHeight);
            GL11.glVertex2f(drawX, drawY + height);

            GL11.glTexCoord2f(texX + texWidth, texY + texHeight);
            GL11.glVertex2f(drawX + width, drawY + height);

            GL11.glTexCoord2f(texX + texWidth, texY);
            GL11.glVertex2f(drawX + width, drawY);
            GL11.glEnd();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class CharacterData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;
    }
}
