package com.housingclient.gui.components;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.Module;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Vape V4 style panel with clean header and cyan accents
 */
public class Panel {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final FontRenderer fontRenderer = mc.fontRendererObj;

    private final String title;
    private int x, y;
    private int width;
    private final Theme theme;
    private final Color categoryColor;

    private final List<ModuleButton> moduleButtons = new ArrayList<>();

    private boolean collapsed = false;
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    private float expandAnimation = 1.0f;
    private float headerHoverAnimation = 0.0f;
    private int scrollOffset = 0;
    private final int maxHeight = 300;

    private static final int HEADER_HEIGHT = 24;
    private static final int PADDING = 4;

    public Panel(String title, int x, int y, int width, Theme theme, Color categoryColor) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.theme = theme;
        this.categoryColor = categoryColor;
    }

    public void addModule(Module module) {
        moduleButtons.add(new ModuleButton(module, this, theme));
    }

    public void draw(int mouseX, int mouseY, float partialTicks) {
        // Handle dragging
        if (dragging) {
            x = mouseX - dragOffsetX;
            y = mouseY - dragOffsetY;
        }

        // Update animations
        float targetExpand = collapsed ? 0 : 1;
        expandAnimation += (targetExpand - expandAnimation) * 0.2f;

        boolean headerHovered = isMouseOverHeader(mouseX, mouseY);
        float targetHeaderHover = headerHovered ? 1 : 0;
        headerHoverAnimation += (targetHeaderHover - headerHoverAnimation) * 0.3f;

        // Calculate heights
        int contentHeight = calculateContentHeight();
        int displayHeight = Math.min(contentHeight, maxHeight);

        int totalHeight = HEADER_HEIGHT;
        if (!collapsed && !moduleButtons.isEmpty()) {
            totalHeight += (int) ((displayHeight + PADDING * 2) * expandAnimation);
        }

        // Draw shadow
        RenderUtils.drawShadow(x, y, width, totalHeight, 8, theme.getShadowColor());

        // Draw panel body
        if (!collapsed && expandAnimation > 0.1f) {
            RenderUtils.drawRoundedRect(x, y + HEADER_HEIGHT, width,
                    (int) ((displayHeight + PADDING * 2) * expandAnimation), 0, theme.getPanelColor());
            // Bottom rounded corners
            RenderUtils.drawRoundedRect(x, y + totalHeight - 6, width, 6, 4, theme.getPanelColor());
        }

        // Draw header
        drawHeader(mouseX, mouseY);

        // Draw modules if not collapsed
        if (!collapsed && expandAnimation > 0.1f) {
            drawModules(mouseX, mouseY, displayHeight, contentHeight);
        }
    }

    private int calculateContentHeight() {
        int height = 0;
        for (ModuleButton button : moduleButtons) {
            height += button.getTotalHeight() + 2;
        }
        return height;
    }

    private void drawHeader(int mouseX, int mouseY) {
        // Header background with slight hover effect
        int headerColor = theme.getHeaderColor();
        if (headerHoverAnimation > 0.01f) {
            headerColor = Theme.brighter(headerColor, 0.05f * headerHoverAnimation);
        }

        RenderUtils.drawRoundedRect(x, y, width, HEADER_HEIGHT, 4, headerColor);

        // Cyan accent line at bottom of header
        RenderUtils.drawHorizontalLine(x + 8, y + HEADER_HEIGHT - 2, width - 16, 2, theme.getAccentColor());

        // Title with Vape-style formatting
        String displayTitle = title.toUpperCase();
        fontRenderer.drawStringWithShadow(displayTitle, x + 10, y + 7, theme.getTextColor());

        // Collapse arrow
        String arrow = collapsed ? "\u25B6" : "\u25BC"; // ▶ or ▼
        int arrowColor = Theme.withAlpha(theme.getTextColor(), (int) (150 + 105 * headerHoverAnimation));
        fontRenderer.drawStringWithShadow(arrow, x + width - 14, y + 7, arrowColor);
    }

    private void drawModules(int mouseX, int mouseY, int displayHeight, int contentHeight) {
        int contentY = y + HEADER_HEIGHT + PADDING;

        boolean needsScroll = contentHeight > maxHeight;

        if (needsScroll) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtils.scissor(x, contentY, width, displayHeight);
        }

        int moduleY = contentY - scrollOffset;

        for (ModuleButton button : moduleButtons) {
            int buttonHeight = button.getTotalHeight();
            button.setPosition(x + PADDING, moduleY, width - PADDING * 2);

            // Only draw if visible
            if (moduleY + buttonHeight > contentY - 50 && moduleY < contentY + displayHeight + 50) {
                button.draw(mouseX, mouseY);
            }

            moduleY += buttonHeight + 2;
        }

        if (needsScroll) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            drawScrollBar(contentY, displayHeight, contentHeight);
        }
    }

    private void drawScrollBar(int contentY, int displayHeight, int contentHeight) {
        int scrollBarX = x + width - 4;
        int scrollBarWidth = 2;

        // Track
        RenderUtils.drawRoundedRect(scrollBarX, contentY + 2, scrollBarWidth, displayHeight - 4, 1,
                Theme.withAlpha(theme.getModuleColor(), 100));

        // Handle
        float scrollRatio = (float) displayHeight / contentHeight;
        int handleHeight = Math.max(20, (int) (displayHeight * scrollRatio));

        int maxScroll = contentHeight - displayHeight;
        float scrollProgress = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0;
        int handleY = contentY + 2 + (int) ((displayHeight - 4 - handleHeight) * scrollProgress);

        RenderUtils.drawRoundedRect(scrollBarX, handleY, scrollBarWidth, handleHeight, 1, theme.getAccentColor());
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (isMouseOverHeader(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = mouseX - x;
                dragOffsetY = mouseY - y;
            } else if (button == 1) {
                collapsed = !collapsed;
            }
            return;
        }

        if (!collapsed) {
            for (ModuleButton moduleButton : moduleButtons) {
                moduleButton.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        dragging = false;

        for (ModuleButton moduleButton : moduleButtons) {
            moduleButton.mouseReleased(mouseX, mouseY, button);
        }
    }

    public boolean keyTyped(int keyCode) {
        for (ModuleButton button : moduleButtons) {
            if (button.keyTyped(keyCode)) {
                return true;
            }
        }
        return false;
    }

    public void handleScroll(int delta) {
        if (collapsed)
            return;

        int contentHeight = calculateContentHeight();
        if (contentHeight <= maxHeight)
            return;

        int maxScroll = contentHeight - maxHeight;
        scrollOffset -= delta / 8;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + HEADER_HEIGHT;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        int contentHeight = calculateContentHeight();
        int displayHeight = Math.min(contentHeight, maxHeight);
        int height = collapsed ? HEADER_HEIGHT : HEADER_HEIGHT + displayHeight + PADDING * 2;

        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public Theme getTheme() {
        return theme;
    }

    public String getTitle() {
        return title;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
}
