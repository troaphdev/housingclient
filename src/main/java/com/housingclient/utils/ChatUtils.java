package com.housingclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ChatUtils {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    // Use unicode escape for section symbol to avoid encoding issues
    public static final String PREFIX = "\u00A78[\u00A7b\u00A7lHousing\u00A73\u00A7lClient\u00A78] \u00A7r";
    
    public static void sendClientMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + message));
        }
    }
    
    public static void sendClientMessageNoPrefix(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
    
    public static void sendClientMessage(IChatComponent component) {
        if (mc.thePlayer != null) {
            mc.thePlayer.addChatMessage(component);
        }
    }
    
    public static void sendServerMessage(String message) {
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage(message);
        }
    }
    
    public static void sendCommand(String command) {
        if (!command.startsWith("/")) {
            command = "/" + command;
        }
        sendServerMessage(command);
    }
    
    public static String formatColorCodes(String text) {
        return text.replace("&", "\u00A7");
    }
    
    public static String stripColorCodes(String text) {
        return text.replaceAll("\u00A7[0-9a-fk-or]", "");
    }
}
