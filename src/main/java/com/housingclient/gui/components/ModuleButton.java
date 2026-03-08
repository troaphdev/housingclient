package com.housingclient.gui.components;

import com.housingclient.gui.theme.Theme;
import com.housingclient.module.Module;
import com.housingclient.module.settings.*;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import com.housingclient.HousingClient;
import com.housingclient.module.modules.client.ClickGUIModule;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Vape V4 style module button with full cyan background when enabled
 */
public class ModuleButton {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final FontRenderer fontRenderer = mc.fontRendererObj;

    private final Module module;
    private final Panel parent;
    private final Theme theme;

    private int x, y, width;
    private static final int HEIGHT = 20;

    private boolean expanded = false;
    private boolean hovered = false;
    private boolean bindingKey = false;
    private boolean infoHovered = false;

    private static final ResourceLocation INFO_ICON = new ResourceLocation("housingclient",
            "textures/gui/moduleinfo.png");
    private static final int INFO_ICON_SIZE = 10;

    // Smooth animations
    private float enableAnimation = 0;
    private float expandAnimation = 0;
    private float hoverAnimation = 0;

    private final List<SettingComponent> settingComponents = new ArrayList<SettingComponent>();

    public ModuleButton(Module module, Panel parent, Theme theme) {
        this.module = module;
        this.parent = parent;
        this.theme = theme;

        initializeSettings();
    }

    private void initializeSettings() {
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
                settingComponents.add(new CheckboxComponent((BooleanSetting) setting, theme));
            } else if (setting instanceof NumberSetting) {
                settingComponents.add(new SliderComponent((NumberSetting) setting, theme));
            } else if (setting instanceof ModeSetting) {
                settingComponents.add(new ModeComponent((ModeSetting) setting, theme));
            }
        }
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public void draw(int mouseX, int mouseY) {
        // Update animations with smooth easing
        float targetEnable = module.isEnabled() ? 1 : 0;
        enableAnimation += (targetEnable - enableAnimation) * 0.25f;

        float targetExpand = expanded ? 1 : 0;
        expandAnimation += (targetExpand - expandAnimation) * 0.2f;

        hovered = isMouseOver(mouseX, mouseY);
        float targetHover = hovered ? 1 : 0;
        hoverAnimation += (targetHover - hoverAnimation) * 0.3f;

        // Calculate colors based on state
        int bgColor;
        if (module.isEnabled()) {
            // Vape V4 style: Full cyan background when enabled
            bgColor = theme.getEnabledBackgroundColor();
            if (hoverAnimation > 0.01f) {
                bgColor = Theme.brighter(bgColor, 0.1f * hoverAnimation);
            }
        } else {
            // Dark background when disabled
            bgColor = theme.getModuleColor();
            if (hoverAnimation > 0.01f) {
                bgColor = Theme.blend(bgColor, theme.getModuleColorHover(), hoverAnimation);
            }
        }

        // Draw subtle glow for enabled modules
        if (enableAnimation > 0.5f) {
            RenderUtils.drawGlow(x - 1, y - 1, width + 2, HEIGHT + 2,
                    (int) (4 * enableAnimation), theme.getAccentColor());
        }

        // Draw button background
        RenderUtils.drawRoundedRect(x, y, width, HEIGHT, 4, bgColor);

        // Check tooltips setting
        com.housingclient.module.modules.client.ClickGUIModule clickGuiModule = HousingClient.getInstance()
                .getModuleManager().getModule(com.housingclient.module.modules.client.ClickGUIModule.class);
        if (clickGuiModule == null) {
            Module m = HousingClient.getInstance().getModuleManager().getModule("ClickGUI");
            if (m instanceof com.housingclient.module.modules.client.ClickGUIModule) {
                clickGuiModule = (com.housingclient.module.modules.client.ClickGUIModule) m;
            }
        }
        boolean tooltipsEnabled = clickGuiModule != null && clickGuiModule.isTooltipsEnabled();

        if (tooltipsEnabled) {
            // Draw info icon in top-left corner
            int infoX = x + 4;
            int infoY = y + (HEIGHT - INFO_ICON_SIZE) / 2;
            infoHovered = mouseX >= infoX && mouseX <= infoX + INFO_ICON_SIZE && mouseY >= infoY
                    && mouseY <= infoY + INFO_ICON_SIZE;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ZERO);
            GlStateManager.color(1f, 1f, 1f, infoHovered ? 1.0f : 0.5f);
            mc.getTextureManager().bindTexture(INFO_ICON);
            // Draw the texture scaled to INFO_ICON_SIZE (texture is larger, we scale it
            // down)
            Gui.drawScaledCustomSizeModalRect(infoX, infoY, 0, 0, 512, 512, INFO_ICON_SIZE, INFO_ICON_SIZE, 512, 512);
            GlStateManager.color(1f, 1f, 1f, 1f); // Reset color
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        } else {
            infoHovered = false;
        }

        // Draw module name (shifted right for info icon)
        String name = module.getName();
        int textColor = module.isEnabled() ? 0xFF1A1A22 : 0xFFCCCCCC; // Dark text on cyan, light on dark
        int nameX = x + 4 + INFO_ICON_SIZE + 4; // icon + padding
        if (module.isEnabled()) {
            // Enabled: dark text for contrast on cyan
            fontRenderer.drawString(name, nameX, y + 6, textColor);
        } else {
            fontRenderer.drawStringWithShadow(name, nameX, y + 6, textColor);
        }

        // Draw keybind
        String keybindText;
        if (bindingKey) {
            keybindText = "[...]";
        } else if (module.getKeybind() != 0) {
            keybindText = "[" + Keyboard.getKeyName(module.getKeybind()) + "]";
        } else {
            keybindText = "";
        }

        if (!keybindText.isEmpty()) {
            int keybindWidth = fontRenderer.getStringWidth(keybindText);
            int keybindColor = module.isEnabled() ? 0xFF2A2A35 : 0xFF666666;
            if (module.isEnabled()) {
                fontRenderer.drawString(keybindText, x + width - keybindWidth - 20, y + 6, keybindColor);
            } else {
                fontRenderer.drawStringWithShadow(keybindText, x + width - keybindWidth - 20, y + 6, keybindColor);
            }
        }

        // Draw settings indicator (three dots) if has settings
        if (!module.getSettings().isEmpty()) {
            int dotColor = module.isEnabled() ? 0xFF2A2A35 : 0xFF888888;
            int dotX = x + width - 14;
            int dotY = y + HEIGHT / 2;
            int dotSize = 2;
            int dotSpacing = 4;

            // Draw three vertical dots (⋮)
            RenderUtils.drawCircle(dotX, dotY - dotSpacing, dotSize, dotColor);
            RenderUtils.drawCircle(dotX, dotY, dotSize, dotColor);
            RenderUtils.drawCircle(dotX, dotY + dotSpacing, dotSize, dotColor);
        }

        // Draw settings if expanded
        if (expanded && expandAnimation > 0.1f) {
            drawSettings(mouseX, mouseY);
        }

        // Draw info tooltip if hovered (drawn last to be on top)
        if (infoHovered && module.getDescription() != null && !module.getDescription().isEmpty()) {
            ClickGUIModule clickGui = (ClickGUIModule) com.housingclient.HousingClient.getInstance().getModuleManager()
                    .getModule(ClickGUIModule.class);
            if (clickGui != null && clickGui.isTooltipsEnabled()) {
                drawInfoTooltip(mouseX, mouseY);
            }
        }
    }

    private void drawInfoTooltip(int mouseX, int mouseY) {
        String desc = module.getDescription();
        int tooltipWidth = fontRenderer.getStringWidth(desc) + 8;
        int tooltipHeight = 12;
        int tooltipX = mouseX + 8;
        int tooltipY = mouseY - 4;

        // Prevent going off-screen
        int screenWidth = mc.currentScreen != null ? mc.currentScreen.width : 400;
        if (tooltipX + tooltipWidth > screenWidth - 5) {
            tooltipX = mouseX - tooltipWidth - 8;
        }

        // Draw tooltip background
        RenderUtils.drawRoundedRect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 3, 0xEE1A1A20);
        RenderUtils.drawRoundedRectOutline(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 3, theme.getAccentColor(),
                1.0f);

        // Draw tooltip text
        fontRenderer.drawStringWithShadow(desc, tooltipX + 4, tooltipY + 2, 0xFFCCCCCC);
    }

    private void drawSettings(int mouseX, int mouseY) {
        int settingY = y + HEIGHT + 2;
        int settingX = x + 4;
        int settingWidth = width - 8;

        // Draw settings background
        int totalHeight = 0;
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                totalHeight += component.getHeight() + 2;
            }
        }

        if (totalHeight > 0) {
            RenderUtils.drawRoundedRect(x, y + HEIGHT, width, totalHeight + 4, 4, theme.getSettingsBackground());
        }

        for (SettingComponent component : settingComponents) {
            if (!component.getSetting().isVisible())
                continue;

            component.setPosition(settingX, settingY, settingWidth);
            component.draw(mouseX, mouseY);
            settingY += component.getHeight() + 2;
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY) && !isMouseOverSettings(mouseX, mouseY)) {
            return;
        }

        if (isMouseOver(mouseX, mouseY)) {
            // Check if clicking on three-dot menu
            if (!module.getSettings().isEmpty() && mouseX >= x + width - 20) {
                expanded = !expanded;
                return;
            }

            // Check keybind area (between module name and three dots)
            String keybindText = module.getKeybind() != 0 ? "[" + Keyboard.getKeyName(module.getKeybind()) + "]" : "";
            if (!keybindText.isEmpty()) {
                int keybindWidth = fontRenderer.getStringWidth(keybindText);
                int keybindX = x + width - keybindWidth - 20;
                if (mouseX >= keybindX && mouseX < x + width - 20) {
                    bindingKey = !bindingKey;
                    return;
                }
            }

            if (button == 0) {
                // Left click - toggle module
                module.toggle();
            } else if (button == 1) {
                // Right click - expand settings
                if (!module.getSettings().isEmpty()) {
                    expanded = !expanded;
                }
            }
            return;
        }

        // Check settings clicks
        if (expanded) {
            for (SettingComponent component : settingComponents) {
                component.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (expanded) {
            for (SettingComponent component : settingComponents) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean keyTyped(int keyCode) {
        if (bindingKey) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                module.setKeybind(0);
            } else if (keyCode != Keyboard.KEY_RETURN) {
                module.setKeybind(keyCode);
            }
            bindingKey = false;
            return true;
        }

        if (expanded) {
            for (SettingComponent component : settingComponents) {
                if (component instanceof SliderComponent) {
                    if (((SliderComponent) component).keyTyped(keyCode)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + HEIGHT;
    }

    private boolean isMouseOverSettings(int mouseX, int mouseY) {
        if (!expanded)
            return false;

        int totalHeight = 0;
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                totalHeight += component.getHeight() + 2;
            }
        }

        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y + HEIGHT && mouseY <= y + HEIGHT + totalHeight + 4;
    }

    public int getTotalHeight() {
        if (!expanded)
            return HEIGHT;

        int settingsHeight = 4; // padding
        for (SettingComponent component : settingComponents) {
            if (component.getSetting().isVisible()) {
                settingsHeight += component.getHeight() + 2;
            }
        }

        return HEIGHT + settingsHeight;
    }

    public Module getModule() {
        return module;
    }

    public boolean isBindingKey() {
        return bindingKey;
    }
}
