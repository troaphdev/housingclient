package com.housingclient.module.modules.client;

import com.housingclient.gui.CustomGuiNewChat;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.client.gui.*;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Chat Module (formerly Infinite Chat)
 * Increases the chat history limit by replacing the vanilla GuiNewChat.
 * Also bypasses the 100-character input limit.
 */
public class ChatModule extends Module {

    private final NumberSetting maxLines = new NumberSetting("Max Lines", "Maximum chat lines to keep", 5000, 500,
            10000);
    private final BooleanSetting customTabCompletion = new BooleanSetting(
            "Custom Tab Completion", "Enable player name tab completion for custom commands", true);
    private final BooleanSetting rightClickCopy = new BooleanSetting(
            "Right Click Copy", "Right click a chat message to copy it to clipboard", true);

    private GuiNewChat originalChat;
    private CustomGuiNewChat customChat;

    // Reflection fields for GuiChat input field
    private static final String[] INPUT_FIELD_NAMES = new String[] { "inputField", "field_146415_a", "a" };

    public ChatModule() {
        super("Chat", "Increases chat history and input limit", Category.CLIENT, ModuleMode.BOTH);
        addSetting(maxLines);
        addSetting(customTabCompletion);
        addSetting(rightClickCopy);
        setEnabled(true);
    }

    /**
     * Check if custom tab completion for slash commands is enabled
     */
    public boolean isCustomTabCompletionEnabled() {
        return customTabCompletion.isEnabled();
    }

    @Override
    protected void onEnable() {
        if (mc.ingameGUI == null)
            return;

        // Replace GuiNewChat
        originalChat = mc.ingameGUI.getChatGUI();
        if (!(originalChat instanceof CustomGuiNewChat)) {
            customChat = new CustomGuiNewChat(mc);
            updateCustomChatSettings();

            // Migrate existing lines to prevent chat wipe
            migrateChatLines(originalChat, customChat);

            setPersistentChatGUI(customChat);
        }
    }

    @Override
    public void onTick() {
        if (customChat != null) {
            updateCustomChatSettings();
        }

        // Enforce replacement (some mods might reset it)
        if (mc.ingameGUI != null && isEnabled()) {
            if (!(mc.ingameGUI.getChatGUI() instanceof CustomGuiNewChat)) {
                if (customChat == null)
                    customChat = new CustomGuiNewChat(mc);
                updateCustomChatSettings();
                originalChat = mc.ingameGUI.getChatGUI(); // Update original so we can restore later
                migrateChatLines(originalChat, customChat);
                setPersistentChatGUI(customChat);
            }
        }
    }

    private void updateCustomChatSettings() {
        if (customChat != null) {
            customChat.setMaxLines(maxLines.getIntValue());
        }
    }

    @Override
    protected void onDisable() {
        if (originalChat != null && mc.ingameGUI != null) {
            setPersistentChatGUI(originalChat);
        }
    }

    private void migrateChatLines(GuiNewChat oldChat, CustomGuiNewChat newChat) {
        try {
            Field chatLinesField = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h");
            Field drawnChatLinesField = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines",
                    "field_146253_i");
            chatLinesField.setAccessible(true);
            drawnChatLinesField.setAccessible(true);

            List<ChatLine> oldLines = (List<ChatLine>) chatLinesField.get(oldChat);
            List<ChatLine> oldDrawn = (List<ChatLine>) drawnChatLinesField.get(oldChat);

            List<ChatLine> newLines = (List<ChatLine>) chatLinesField.get(newChat);
            List<ChatLine> newDrawn = (List<ChatLine>) drawnChatLinesField.get(newChat);

            newLines.clear();
            newLines.addAll(oldLines);

            newDrawn.clear();
            newDrawn.addAll(oldDrawn);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setPersistentChatGUI(GuiNewChat chat) {
        try {
            // field_73840_e is persistantChatGUI
            Field field = ReflectionHelper.findField(GuiIngame.class, "persistantChatGUI", "field_73840_e");
            field.setAccessible(true);

            // Remove final modifier
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

            field.set(mc.ingameGUI, chat);
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.gui instanceof GuiChat) {
            GuiChat guiChat = (GuiChat) event.gui;
            try {
                Field inputField = ReflectionHelper.findField(GuiChat.class, INPUT_FIELD_NAMES);
                inputField.setAccessible(true);
                GuiTextField textField = (GuiTextField) inputField.get(guiChat);
                if (textField != null) {
                    textField.setMaxStringLength(5000); // Bypass 100 char limit
                }
            } catch (Exception e) {
                // Fail silently
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(maxLines.getIntValue());
    }

    /**
     * Check if right-click copy is enabled.
     */
    public boolean isRightClickCopyEnabled() {
        return isEnabled() && rightClickCopy.isEnabled();
    }
}
