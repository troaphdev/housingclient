package com.housingclient.event;

import com.housingclient.command.ChatCommandHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * This is called from a mixin or event to intercept outgoing chat messages.
     * Returns true if the message was handled (command), false to send normally.
     */
    public static boolean onChatSend(String message) {
        if (ChatCommandHandler.isCommand(message)) {
            return ChatCommandHandler.handleChatMessage(message);
        }
        return false;
    }
}
