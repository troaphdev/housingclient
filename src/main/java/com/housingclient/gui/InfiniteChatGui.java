package com.housingclient.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;

import java.lang.reflect.Field;
import java.util.List;

public class InfiniteChatGui extends GuiNewChat {

    public InfiniteChatGui(Minecraft mcIn) {
        super(mcIn);
    }

    /**
     * Copy existing chat messages from another chat GUI
     */
    public void copyMessagesFrom(net.minecraft.client.gui.GuiNewChat other) {
        if (other == null)
            return;

        try {
            // Copy chatLines (full messages)
            Field chatLinesField = GuiNewChat.class.getDeclaredField("chatLines");
            chatLinesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ChatLine> sourceChatLines = (List<ChatLine>) chatLinesField.get(other);
            List<ChatLine> targetChatLines = getChatLines();
            targetChatLines.clear();
            targetChatLines.addAll(sourceChatLines);

            // Copy drawnChatLines (rendered lines)
            Field drawnField = GuiNewChat.class.getDeclaredField("drawnChatLines");
            drawnField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ChatLine> sourceDrawnLines = (List<ChatLine>) drawnField.get(other);
            List<ChatLine> targetDrawnLines = getDrawnChatLines();
            targetDrawnLines.clear();
            targetDrawnLines.addAll(sourceDrawnLines);
        } catch (Exception e) {
            // Try obfuscated names
            try {
                Field chatLinesField = GuiNewChat.class.getDeclaredField("field_146252_h");
                chatLinesField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<ChatLine> sourceChatLines = (List<ChatLine>) chatLinesField.get(other);
                getChatLines().addAll(sourceChatLines);

                Field drawnField = GuiNewChat.class.getDeclaredField("field_146253_i");
                drawnField.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<ChatLine> sourceDrawnLines = (List<ChatLine>) drawnField.get(other);
                getDrawnChatLines().addAll(sourceDrawnLines);
            } catch (Exception ex) {
                // Ignore if both fail
            }
        }
    }

    @Override
    public void printChatMessageWithOptionalDeletion(IChatComponent chatComponent, int chatLineId) {
        setInfiniteChatLine(chatComponent, chatLineId, Minecraft.getMinecraft().ingameGUI.getUpdateCounter(), false);
        // We skip logging to avoid double logging if vanilla does it elsewhere, or we
        // can log.
        // Vanilla logs in printChatMessageWithOptionalDeletion.
    }

    public void setInfiniteChatLine(IChatComponent chatComponent, int chatLineId, int updateCounter,
            boolean displayOnly) {
        if (chatLineId != 0) {
            this.deleteChatLine(chatLineId);
        }

        int i = MathHelper.floor_float((float) this.getChatWidth() / this.getChatScale());
        List<IChatComponent> list = GuiUtilRenderComponents.splitText(chatComponent, i,
                Minecraft.getMinecraft().fontRendererObj, false, false);
        boolean flag = this.getChatOpen();

        for (IChatComponent ichatcomponent : list) {
            if (flag && this.getScrollPos() > 0) {
                this.setIsScrolled(true);
                this.scroll(1);
            }

            this.getDrawnChatLines().add(0, new ChatLine(updateCounter, ichatcomponent, chatLineId));
        }

        // Limit to 10k lines instead of 100
        while (this.getDrawnChatLines().size() > 10000) {
            this.getDrawnChatLines().remove(this.getDrawnChatLines().size() - 1);
        }

        if (!displayOnly) {
            this.getChatLines().add(0, new ChatLine(updateCounter, chatComponent, chatLineId));

            while (this.getChatLines().size() > 10000) {
                this.getChatLines().remove(this.getChatLines().size() - 1);
            }
        }
    }

    @Override
    public void scroll(int amount) {
        try {
            Field field = GuiNewChat.class.getDeclaredField("scrollPos");
            field.setAccessible(true);
            int current = field.getInt(this);
            field.setInt(this, current + amount);
        } catch (Exception e) {
            try {
                Field field = GuiNewChat.class.getDeclaredField("field_146250_j");
                field.setAccessible(true);
                int current = field.getInt(this);
                field.setInt(this, current + amount);
            } catch (Exception ex) {
            }
        }
    }

    // Reflection Helpers

    @SuppressWarnings("unchecked")
    private List<ChatLine> getChatLines() {
        try {
            Field field = GuiNewChat.class.getDeclaredField("chatLines");
            field.setAccessible(true);
            return (List<ChatLine>) field.get(this);
        } catch (Exception e) {
            try {
                Field field = GuiNewChat.class.getDeclaredField("field_146252_h");
                field.setAccessible(true);
                return (List<ChatLine>) field.get(this);
            } catch (Exception ex) {
                return Lists.newArrayList();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<ChatLine> getDrawnChatLines() {
        try {
            Field field = GuiNewChat.class.getDeclaredField("drawnChatLines");
            field.setAccessible(true);
            return (List<ChatLine>) field.get(this);
        } catch (Exception e) {
            try {
                Field field = GuiNewChat.class.getDeclaredField("field_146253_i");
                field.setAccessible(true);
                return (List<ChatLine>) field.get(this);
            } catch (Exception ex) {
                return Lists.newArrayList();
            }
        }
    }

    private int getScrollPos() {
        try {
            Field field = GuiNewChat.class.getDeclaredField("scrollPos");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (Exception e) {
            try {
                Field field = GuiNewChat.class.getDeclaredField("field_146250_j");
                field.setAccessible(true);
                return field.getInt(this);
            } catch (Exception ex) {
                return 0;
            }
        }
    }

    private void setIsScrolled(boolean scrolled) {
        try {
            Field field = GuiNewChat.class.getDeclaredField("isScrolled");
            field.setAccessible(true);
            field.setBoolean(this, scrolled);
        } catch (Exception e) {
            try {
                Field field = GuiNewChat.class.getDeclaredField("field_146251_k");
                field.setAccessible(true);
                field.setBoolean(this, scrolled);
            } catch (Exception ex) {
            }
        }
    }
}
