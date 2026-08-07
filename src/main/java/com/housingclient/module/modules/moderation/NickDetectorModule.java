package com.housingclient.module.modules.moderation;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import com.housingclient.utils.MinecraftFormatting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * Nick Detector Module
 * 
 * Checks if player usernames correspond to real Minecraft accounts.
 * Only checks players with default rank colors (no rank, VIP, VIP+, MVP, MVP+ with red +).
 * Skips MVP+ with non-red + and MVP++ entirely.
 * Players whose names don't resolve via Mojang API are marked as nicked
 * with a ~ suffix in chat, tab list, and nametags.
 * 
 * Optimized: Uses thread pool and 1.2s interval for fast lookups without hitting limits.
 */
public class NickDetectorModule extends Module {

    // Cache: username -> isNicked (true = nicked, false = real)
    private final Map<String, Boolean> nickCache = new ConcurrentHashMap<>();

    // Queue of usernames to check
    private final Queue<String> lookupQueue = new ConcurrentLinkedQueue<>();

    // Track which names we've already queued to avoid duplicates
    private final Set<String> queuedNames = Collections.synchronizedSet(new HashSet<>());

    // Timing
    private long lastLookupTime = 0;
    private static final long LOOKUP_INTERVAL_MS = 1200; // 1.2 seconds between lookups (safe for Mojang limits)

    // Thread pool for API lookups (reuse threads instead of spawning new ones)
    private ExecutorService lookupExecutor;

    // Tick counter for scanning
    private int tickCounter = 0;

    public NickDetectorModule() {
        super("Nick Detector", "Detects nicked players via API", Category.MODERATION, ModuleMode.BOTH);
    }

    @Override
    protected void onEnable() {
        nickCache.clear();
        lookupQueue.clear();
        queuedNames.clear();
        lastLookupTime = 0;
        tickCounter = 0;
        lookupExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "NickDetector-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    protected void onDisable() {
        nickCache.clear();
        lookupQueue.clear();
        queuedNames.clear();
        if (lookupExecutor != null) {
            lookupExecutor.shutdownNow();
            lookupExecutor = null;
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.getNetHandler() == null) return;

        tickCounter++;

        // Scan for new players every 2 seconds (40 ticks)
        if (tickCounter % 40 == 0) {
            scanTabList();
        }

        // Process lookup queue
        long now = System.currentTimeMillis();
        if (!lookupQueue.isEmpty() && now - lastLookupTime >= LOOKUP_INTERVAL_MS) {
            String name = lookupQueue.poll();
            if (name != null && lookupExecutor != null && !lookupExecutor.isShutdown()) {
                lastLookupTime = now;
                final String nameFinal = name;
                lookupExecutor.submit(() -> {
                    boolean nicked = checkIfNicked(nameFinal);
                    nickCache.put(nameFinal.toLowerCase(), nicked);
                });
            }
        }
    }

    /**
     * Append ~ to nicked player names in chat messages.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (!isEnabled()) return;
        if (event.message == null) return;
        // Only modify type 0 (standard chat) and type 1 (system messages)
        if (event.type == 2) return;

        String formattedText = event.message.getFormattedText();
        boolean modified = false;

        // Check each nicked player and append ~ after their name in the message
        for (Map.Entry<String, Boolean> entry : nickCache.entrySet()) {
            if (!entry.getValue()) continue; // Not nicked
            String nickedName = entry.getKey();

            String markedText = MinecraftFormatting.appendAfterIgnoreCase(
                    formattedText, nickedName, "\u00A7c~");
            if (!markedText.equals(formattedText)) {
                formattedText = markedText;
                modified = true;
            }
        }

        if (modified) {
            event.message = new ChatComponentText(formattedText);
        }
    }

    /**
     * Scan the tab list for players we haven't checked yet.
     * Only queues players with default rank colors that nicked players could have.
     */
    private void scanTabList() {
        if (mc.getNetHandler() == null) return;

        Collection<NetworkPlayerInfo> players = mc.getNetHandler().getPlayerInfoMap();
        for (NetworkPlayerInfo info : players) {
            String name = info.getGameProfile().getName();
            if (name == null || name.isEmpty()) continue;

            // Skip self
            if (name.equals(mc.thePlayer.getName())) continue;

            // Skip already cached or queued
            String lower = name.toLowerCase();
            if (nickCache.containsKey(lower) || queuedNames.contains(lower)) continue;

            // Get formatted display name from tab
            String displayName = "";
            if (info.getDisplayName() != null) {
                displayName = info.getDisplayName().getFormattedText();
            } else {
                displayName = ScorePlayerTeam.formatPlayerName(
                        info.getPlayerTeam(), info.getGameProfile().getName());
            }

            // No formatting = bot, skip
            if (!displayName.contains("\u00A7")) continue;

            // Check rank color — only queue players with default rank colors
            if (!shouldCheckPlayer(displayName, name)) continue;

            // Queue for lookup
            queuedNames.add(lower);
            lookupQueue.add(name);
        }
    }

    /**
     * Determine if a player should be checked for nicking based on their rank color.
     * 
     * Only check:
     * - No rank (gray §7 username)
     * - VIP (lime green §a [VIP])
     * - VIP+ (lime green §a with gold §6 +)
     * - MVP (light blue §b [MVP])
     * - MVP+ with RED + only (§b MVP with §c +)
     * 
     * Skip:
     * - MVP+ with any non-red + color (custom rank color = real MVP+)
     * - MVP++ (two +'s = real player)
     */
    private boolean shouldCheckPlayer(String displayName, String playerName) {
        // Strip spaces for easier parsing
        String stripped = displayName;

        // Check for MVP++ first — always skip (has two +'s)
        // MVP++ format: §b[MVP§X++§b]  where X is any color code
        if (stripped.contains("++")) {
            return false; // MVP++ — never nicked, skip
        }

        // Find the player's name color code (the color code right before the player name)
        int nameIdx = stripped.indexOf(playerName);
        if (nameIdx < 0) return false; // Can't find name in display, skip

        // Find the color code immediately before the player name
        char nameColorCode = '7'; // default gray
        for (int i = nameIdx - 1; i >= 1; i--) {
            if (stripped.charAt(i - 1) == '\u00A7') {
                char code = Character.toLowerCase(stripped.charAt(i));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    nameColorCode = code;
                    break;
                }
            }
        }

        // Gray (§7) = no rank → check
        if (nameColorCode == '7') {
            return true;
        }

        // Green (§a) = VIP or VIP+ → check
        if (nameColorCode == 'a') {
            return true;
        }

        // Aqua/Light Blue (§b) = MVP or MVP+ → need to check + color
        if (nameColorCode == 'b') {
            // Check if this is MVP+ (has a + in the rank tag)
            // Look for the rank bracket section before the name: [MVP+]
            String beforeName = stripped.substring(0, nameIdx);

            // Find if there's a + in the rank
            int plusIdx = beforeName.lastIndexOf('+');
            if (plusIdx < 0) {
                // Plain MVP (no +) → check
                return true;
            }

            // MVP+ — check the color of the +
            // Find the color code right before the +
            for (int i = plusIdx - 1; i >= 1; i--) {
                if (stripped.charAt(i - 1) == '\u00A7') {
                    char plusColor = Character.toLowerCase(stripped.charAt(i));
                    if ((plusColor >= '0' && plusColor <= '9') || (plusColor >= 'a' && plusColor <= 'f')) {
                        // Red (§c) = default MVP+ color → check (could be nicked)
                        if (plusColor == 'c') {
                            return true;
                        }
                        // Any other color = custom rank color → real MVP+, skip
                        return false;
                    }
                    break;
                }
            }
            // Couldn't determine + color, default to checking
            return true;
        }

        // Any other name color (gold, red, dark colors, etc.) = custom/special rank → skip
        return false;
    }

    /**
     * Check if a username exists as a real Minecraft account via Mojang API.
     * Returns true if the name does NOT resolve (i.e., nicked).
     */
    private boolean checkIfNicked(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "HousingClient/1.0");

            int code = conn.getResponseCode();

            if (code == 200) {
                // Account exists — read the response to verify
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && line.contains("\"id\"")) {
                        return false; // Real account
                    }
                }
                return false;
            } else if (code == 204 || code == 404) {
                // No account with this name
                return true; // Nicked
            } else {
                // API error — don't mark as nicked
                System.out.println("[NickDetector] API returned " + code + " for " + username);
                return false;
            }
        } catch (Exception e) {
            System.out.println("[NickDetector] Error checking " + username + ": " + e.getMessage());
            return false; // Don't mark as nicked on error
        }
    }

    /**
     * Check if a player is nicked (for use by other modules/renderers).
     */
    public boolean isNicked(String username) {
        Boolean cached = nickCache.get(username.toLowerCase());
        return cached != null && cached;
    }

    @Override
    public String getDisplayInfo() {
        long nickedCount = nickCache.values().stream().filter(b -> b).count();
        if (nickedCount > 0) {
            return nickedCount + " nicked";
        }
        return null;
    }
}
