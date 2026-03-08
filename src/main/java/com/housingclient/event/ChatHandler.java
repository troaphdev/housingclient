package com.housingclient.event;

import com.housingclient.HousingClient;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatHandler {
    
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        // Chat handling is done in HousingDetector
    }
}

