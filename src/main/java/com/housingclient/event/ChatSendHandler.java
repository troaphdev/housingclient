package com.housingclient.event;

import com.housingclient.command.ChatCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

public class ChatSendHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private long lastTime = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyboardInput(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat))
            return;

        if (System.currentTimeMillis() - lastTime < 100)
            return;

        // Check if Enter was pressed
        if (Keyboard.getEventKey() != Keyboard.KEY_RETURN && Keyboard.getEventKey() != Keyboard.KEY_NUMPADENTER)
            return;
        if (!Keyboard.getEventKeyState())
            return;

        try {
            GuiChat guiChat = (GuiChat) event.gui;
            GuiTextField inputField = getInputField(guiChat);

            if (inputField != null) {
                String message = inputField.getText().trim();

                if (ChatCommandHandler.isCommand(message)) {
                    // Handle command
                    boolean handled = ChatCommandHandler.handleChatMessage(message);

                    if (handled) {
                        lastTime = System.currentTimeMillis();
                        // Cancel the event and close chat
                        event.setCanceled(true);
                        mc.displayGuiScreen(null);
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    private GuiTextField getInputField(GuiChat guiChat) {
        // Try different field names (obfuscated and deobfuscated)
        String[] fieldNames = { "inputField", "field_146415_a", "a" };

        for (String fieldName : fieldNames) {
            try {
                Field field = GuiChat.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return (GuiTextField) field.get(guiChat);
            } catch (Exception e) {
                // Try next field name
            }
        }

        return null;
    }
}
