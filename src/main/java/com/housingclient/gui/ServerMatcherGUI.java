package com.housingclient.gui;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.exploit.ServerMatcherModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Simple vanilla-style GUI for entering target server ID
 */
public class ServerMatcherGUI extends GuiScreen {

    private GuiTextField serverIdField;
    private final ServerMatcherModule module;

    public ServerMatcherGUI(ServerMatcherModule module) {
        this.module = module;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        serverIdField = new GuiTextField(0, fontRendererObj, centerX - 75, centerY - 10, 150, 20);
        serverIdField.setMaxStringLength(20);
        serverIdField.setFocused(true);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;

        // Title
        drawCenteredString(fontRendererObj, "\u00A7bServer Matcher", centerX, centerY - 50, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "\u00A77Enter target server ID:", centerX, centerY - 30, 0xFFFFFFFF);

        // Text field
        serverIdField.drawTextBox();

        // Instructions
        drawCenteredString(fontRendererObj, "\u00A7aPress ENTER to start matching", centerX, centerY + 20, 0xFFFFFFFF);
        drawCenteredString(fontRendererObj, "\u00A77Press ESC to cancel", centerX, centerY + 35, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            module.cancel();
            mc.displayGuiScreen(null);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            String serverId = serverIdField.getText().trim();
            if (!serverId.isEmpty()) {
                module.startMatching(serverId);
                mc.displayGuiScreen(null);
            }
            return;
        }

        serverIdField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        serverIdField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
