package com.housingclient.event;

import com.housingclient.altmanager.CookieAltManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.IChatComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Field;

/**
 * Detects ban screens and updates the alt account's ban status.
 * Uses Forge's GuiOpenEvent which fires reliably for all GUI opens,
 * avoiding mixin obfuscation issues.
 */
public class BanDetectionHandler {

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!(event.gui instanceof GuiDisconnected)) return;

        try {
            GuiDisconnected screen = (GuiDisconnected) event.gui;

            // Extract the disconnect message via reflection
            // Try named fields first (deobf), then SRG, then type-based fallback
            IChatComponent message = getField(screen, "message");
            if (message == null) message = getField(screen, "field_146304_f");
            if (message == null) message = findIChatComponentField(screen);

            if (message == null) return;

            String text = message.getUnformattedText();
            if (text == null || text.isEmpty()) return;

            String lowerText = text.toLowerCase();
            String currentUser = Minecraft.getMinecraft().getSession().getUsername();

            if (currentUser == null || currentUser.isEmpty()) return;

            CookieAltManager manager = CookieAltManager.getInstance();
            if (manager == null) return;

            CookieAltManager.AltAccount account = null;
            for (CookieAltManager.AltAccount acc : manager.getAccounts()) {
                if (acc.username != null && acc.username.equalsIgnoreCase(currentUser)) {
                    account = acc;
                    break;
                }
            }

            if (account == null) return;

            // Check for ban keywords
            boolean isBan = lowerText.contains("banned") || lowerText.contains("suspended")
                    || lowerText.contains("security alert") || lowerText.contains("account has been blocked");

            if (isBan) {
                if (lowerText.contains("temporarily") || lowerText.contains("temp")) {
                    account.setBan("TEMP_BANNED", parseExpiryToTimestamp(text));
                } else {
                    account.setBan("BANNED", 0);
                }
            } else {
                // Not a ban — clear any previous ban status if not already unbanned
                account.autoUnban();
            }
            manager.save();
        } catch (Exception e) {
            // Never crash on GUI open
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityPlayer && event.entity == Minecraft.getMinecraft().thePlayer) {
            try {
                Minecraft mc = Minecraft.getMinecraft();
                if (!mc.isSingleplayer() && mc.getCurrentServerData() != null) {
                    String ip = mc.getCurrentServerData().serverIP.toLowerCase();
                    if (ip.contains("hypixel.net")) {
                        String currentUser = mc.getSession().getUsername();
                        CookieAltManager manager = CookieAltManager.getInstance();
                        if (manager != null) {
                            for (CookieAltManager.AltAccount acc : manager.getAccounts()) {
                                if (acc.username != null && acc.username.equalsIgnoreCase(currentUser)) {
                                    acc.autoUnban();
                                    manager.save();
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private IChatComponent getField(GuiDisconnected screen, String fieldName) {
        try {
            Field f = screen.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object value = f.get(screen);
            if (value instanceof IChatComponent) {
                return (IChatComponent) value;
            }
        } catch (Exception e) {
            // Field not found with this name
        }
        return null;
    }

    private IChatComponent findIChatComponentField(GuiDisconnected screen) {
        try {
            // Search all declared fields for the IChatComponent type
            for (Field f : screen.getClass().getDeclaredFields()) {
                if (IChatComponent.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (IChatComponent) f.get(screen);
                }
            }
            // Also check superclass fields
            Class<?> superClass = screen.getClass().getSuperclass();
            while (superClass != null && superClass != Object.class) {
                for (Field f : superClass.getDeclaredFields()) {
                    if (IChatComponent.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        return (IChatComponent) f.get(screen);
                    }
                }
                superClass = superClass.getSuperclass();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private long parseExpiryToTimestamp(String text) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:(\\d+)\\s*d(?:ays?)?)?\\s*(?:(\\d+)\\s*h)?\\s*(?:(\\d+)\\s*m)?\\s*(?:(\\d+)\\s*s)?"
            );
            java.util.regex.Matcher matcher = pattern.matcher(text.replace("temporarily banned for ", ""));
            
            long totalMs = 0;
            // The matcher might match empty strings, so we iterate through matches
            // and find the one that actually captured durations
            while (matcher.find()) {
                String d = matcher.group(1);
                String h = matcher.group(2);
                String m = matcher.group(3);
                String s = matcher.group(4);
                
                boolean foundSomething = false;
                if (d != null) { totalMs += Long.parseLong(d) * 86400000L; foundSomething = true; }
                if (h != null) { totalMs += Long.parseLong(h) * 3600000L; foundSomething = true; }
                if (m != null) { totalMs += Long.parseLong(m) * 60000L; foundSomething = true; }
                if (s != null) { totalMs += Long.parseLong(s) * 1000L; foundSomething = true; }
                
                if (foundSomething) {
                    return System.currentTimeMillis() + totalMs;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }
}
