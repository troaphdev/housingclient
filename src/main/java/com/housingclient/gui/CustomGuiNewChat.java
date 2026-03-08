package com.housingclient.gui;

import com.housingclient.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

public class CustomGuiNewChat extends GuiNewChat {

    private final Minecraft mc;
    private int maxLines = 5000;
    private boolean debug = false;

    // Reflection fields for list access
    private Field chatLinesField;
    private Field drawnChatLinesField;
    private Field scrollPosField;
    private Field isScrolledField;

    public CustomGuiNewChat(Minecraft mcIn) {
        super(mcIn);
        this.mc = mcIn;
        initFields();
    }

    public void setMaxLines(int lines) {
        this.maxLines = lines;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private void initFields() {
        try {
            chatLinesField = ReflectionHelper.findField(GuiNewChat.class, "chatLines", "field_146252_h");
            chatLinesField.setAccessible(true);
            drawnChatLinesField = ReflectionHelper.findField(GuiNewChat.class, "drawnChatLines", "field_146253_i");
            drawnChatLinesField.setAccessible(true);
            scrollPosField = ReflectionHelper.findField(GuiNewChat.class, "scrollPos", "field_146250_j");
            scrollPosField.setAccessible(true);
            isScrolledField = ReflectionHelper.findField(GuiNewChat.class, "isScrolled", "field_146251_k");
            isScrolledField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void printChatMessage(IChatComponent chatComponent) {
        this.printChatMessageWithOptionalDeletion(chatComponent, 0);
    }

    @Override
    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
        setChatLineCustom(chatComponent, chatLineId, mc.ingameGUI.getUpdateCounter(), false);
    }

    @Override
    public void refreshChat() {

        try {
            if (chatLinesField == null)
                initFields();

            @SuppressWarnings("unchecked")
            List<ChatLine> chatLines = (List<ChatLine>) chatLinesField.get(this);
            @SuppressWarnings("unchecked")
            List<ChatLine> drawnChatLines = (List<ChatLine>) drawnChatLinesField.get(this);

            drawnChatLines.clear();
            resetScroll();

            for (int i = chatLines.size() - 1; i >= 0; --i) {
                ChatLine chatline = chatLines.get(i);
                setChatLineCustom(chatline.getChatComponent(), chatline.getChatLineID(), chatline.getUpdatedCounter(),
                        true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setChatLineCustom(IChatComponent chatComponent, int chatLineId, int updateCounter,
            boolean displayOnly) {
        if (chatLineId != 0) {
            deleteChatLine(chatLineId);
        }

        int i = MathHelper.floor_float((float) getChatWidth() / getChatScale());
        List<IChatComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i, mc.fontRendererObj, false,
                false);

        try {
            @SuppressWarnings("unchecked")
            List<ChatLine> chatLines = (List<ChatLine>) chatLinesField.get(this);
            @SuppressWarnings("unchecked")
            List<ChatLine> drawnChatLines = (List<ChatLine>) drawnChatLinesField.get(this);

            int scrollPos = scrollPosField.getInt(this);
            boolean isChatOpen = getChatOpen();

            for (IChatComponent ichatcomponent : list) {
                if (isChatOpen && scrollPos > 0) {
                    isScrolledField.setBoolean(this, true);
                    scroll(1);
                }
                drawnChatLines.add(0, new ChatLine(updateCounter, ichatcomponent, chatLineId));
            }

            if (!displayOnly) {
                chatLines.add(0, new ChatLine(updateCounter, chatComponent, chatLineId));
            }

            // Enforce limit logic
            while (drawnChatLines.size() > maxLines * 5) {
                drawnChatLines.remove(drawnChatLines.size() - 1);
            }

            while (chatLines.size() > maxLines) {
                chatLines.remove(chatLines.size() - 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
