package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.command.ChatCommandHandler;
import com.housingclient.module.modules.client.ChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends GuiScreen {

    // Store completions for cycling through multiple matches
    private static List<String> currentCompletions = new ArrayList<>();
    private static int completionIndex = 0;
    private static String originalPrefix = "";
    private static String lastTextBeforeTab = "";

    /**
     * Intercept keyTyped to provide custom tab completion for slash commands.
     * Uses SRG method name func_73869_a for keyTyped in GuiScreen.
     */
    @Inject(method = "func_73869_a", at = @At("HEAD"), cancellable = true)
    private void onKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        // Check for TAB (15) processing
        if (keyCode == 15) {
            GuiTextField inputField = getInputField();
            if (inputField == null) {
                return;
            }

            String text = inputField.getText();

            // Handle client commands (. prefix)
            if (ChatCommandHandler.isCommand(text)) {
                if (handleDotCommandTab(inputField, ChatCommandHandler.getCleanCommand(text).trim())) {
                    ci.cancel();
                }
                return;
            }

            // Handle any slash command with arguments
            if (text.startsWith("/")) {
                if (handleSlashCommandTab(inputField, text)) {
                    ci.cancel();
                }
            }
        }
    }

    /**
     * Intercept mouse clicks to handle right-click copy in chat.
     * SRG: func_73864_a (mouseClicked)
     */
    @Inject(method = "func_73864_a", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        // Right-click = button 1
        if (mouseButton != 1)
            return;

        try {
            ChatModule chatModule = HousingClient.getInstance().getModuleManager().getModule(ChatModule.class);
            if (chatModule == null || !chatModule.isRightClickCopyEnabled())
                return;

            String lineText = getChatLineAtMouse(mouseY);
            if (lineText != null && !lineText.isEmpty()) {
                // Copy to system clipboard silently
                StringSelection selection = new StringSelection(lineText);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                ci.cancel(); // Consume the click
            }
        } catch (Throwable t) {
            // Fail silently
        }
    }

    /**
     * Get the full plain text of the chat line at the given mouse Y position.
     * 
     * Uses the mouseY from GuiScreen.mouseClicked which is already in scaled
     * coordinates (Y from top). This avoids DPI/scale mismatches that occur
     * when using Mouse.getY() with mods like DpiFix.
     * 
     * The chat renders at: (screenHeight - 48) + 20 = screenHeight - 28 from top.
     * So the chat origin (bottom of most recent line) is at Y = screenHeight - 28.
     * Lines grow upward: line 0 at Y, line 1 at Y-9, etc.
     * But GuiChat also has a 14px input bar at the bottom.
     */
    private String getChatLineAtMouse(int mouseY) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            GuiNewChat chatGui = mc.ingameGUI.getChatGUI();
            if (!chatGui.getChatOpen())
                return null;

            float chatScale = chatGui.getChatScale();

            // Empirically calibrated chat bottom offset.
            // MC renders chat at (scaledHeight-48)+20 = scaledHeight-28 from top,
            // but handleMouseInput's -1 and chatScale interactions shift things.
            // Tested: height-14 = 3 lines off, height-28 = 1.5 lines off.
            // Extrapolating: height-41 should be correct.
            int chatAreaBottom = this.height - 41;
            int distFromBottom = chatAreaBottom - mouseY;

            // Apply chat scale
            int k = net.minecraft.util.MathHelper.floor_float((float) distFromBottom / chatScale);

            if (k < 0)
                return null;

            // Get drawn chat lines and scroll pos via reflection
            Field drawnField = null;
            Field scrollField = null;
            try {
                drawnField = GuiNewChat.class.getDeclaredField("drawnChatLines");
            } catch (NoSuchFieldException e) {
                drawnField = GuiNewChat.class.getDeclaredField("field_146253_i");
            }
            try {
                scrollField = GuiNewChat.class.getDeclaredField("scrollPos");
            } catch (NoSuchFieldException e) {
                scrollField = GuiNewChat.class.getDeclaredField("field_146250_j");
            }
            drawnField.setAccessible(true);
            scrollField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<ChatLine> drawnChatLines = (List<ChatLine>) drawnField.get(chatGui);
            int scrollPos = scrollField.getInt(chatGui);

            // Check Y is within visible chat area
            int visibleLines = Math.min(chatGui.getLineCount(), drawnChatLines.size());
            int fontHeight = mc.fontRendererObj.FONT_HEIGHT;
            if (k >= fontHeight * visibleLines + visibleLines)
                return null;

            // Compute line index
            int lineIndex = k / fontHeight + scrollPos;

            if (lineIndex < 0 || lineIndex >= drawnChatLines.size())
                return null;

            ChatLine chatLine = drawnChatLines.get(lineIndex);
            if (chatLine == null || chatLine.getChatComponent() == null)
                return null;

            // Get text and strip all § color/formatting codes
            String text = chatLine.getChatComponent().getUnformattedText();
            text = text.replaceAll("\u00A7.", "");
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the inputField via reflection.
     */
    private GuiTextField getInputField() {
        try {
            GuiChat guiChat = (GuiChat) (Object) this;
            Field field = null;

            try {
                field = GuiChat.class.getDeclaredField("inputField");
            } catch (NoSuchFieldException e) {
                field = GuiChat.class.getDeclaredField("field_146415_a");
            }

            field.setAccessible(true);
            return (GuiTextField) field.get(guiChat);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean handleDotCommandTab(GuiTextField inputField, String text) {
        String[] parts = text.split(" ");
        String lastWord = parts.length > 0 ? parts[parts.length - 1] : "";
        if (text.endsWith(" "))
            lastWord = "";

        List<String> matches = new ArrayList<>();

        if (parts.length <= 1 && !text.endsWith(" ")) {
            matches.addAll(ChatCommandHandler.getCommandSuggestions(text));
        } else {
            matches.addAll(getPlayerNameMatches(lastWord));
        }

        if (!matches.isEmpty()) {
            applyCompletion(inputField, text, lastWord, matches);
            return true;
        }
        return false;
    }

    private boolean handleSlashCommandTab(GuiTextField inputField, String text) {
        String[] parts = text.split(" ");

        // Only provide completions if we're past the command itself
        if (parts.length < 2 && !text.endsWith(" ")) {
            return false; // Let vanilla handle command name completion
        }

        String lastWord = parts.length > 0 ? parts[parts.length - 1] : "";
        if (text.endsWith(" "))
            lastWord = "";

        List<String> matches = getPlayerNameMatches(lastWord);

        if (!matches.isEmpty()) {
            applyCompletion(inputField, text, lastWord, matches);
            return true;
        }
        return false;
    }

    /**
     * Apply completion directly to the text field.
     * If there's one match, complete it immediately.
     * If there are multiple, cycle through them on subsequent Tab presses.
     */
    private void applyCompletion(GuiTextField inputField, String text, String prefix, List<String> matches) {
        Collections.sort(matches, String.CASE_INSENSITIVE_ORDER);

        // Check if this is a continuation (same prefix, cycling through)
        String textBeforeLastWord = text.substring(0, text.length() - prefix.length());

        if (text.equals(lastTextBeforeTab) && !currentCompletions.isEmpty()) {
            // Cycling through existing completions
            completionIndex = (completionIndex + 1) % currentCompletions.size();
        } else {
            // New completion request
            currentCompletions = new ArrayList<>(matches);
            completionIndex = 0;
            originalPrefix = prefix;
        }

        // Apply the completion
        String completion = currentCompletions.get(completionIndex);
        String newText = textBeforeLastWord + completion;

        inputField.setText(newText);
        inputField.setCursorPositionEnd();

        // Store for cycling detection
        lastTextBeforeTab = newText;
    }

    private List<String> getPlayerNameMatches(String prefix) {
        List<String> matches = new ArrayList<>();
        String lowerPrefix = prefix.toLowerCase();

        if (Minecraft.getMinecraft().getNetHandler() != null) {
            for (NetworkPlayerInfo info : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                if (info.getGameProfile() != null) {
                    String name = info.getGameProfile().getName();
                    if (name.toLowerCase().startsWith(lowerPrefix)) {
                        matches.add(name);
                    }
                }
            }
        }
        return matches;
    }
}
