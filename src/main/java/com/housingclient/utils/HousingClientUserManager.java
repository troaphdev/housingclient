package com.housingclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages detection of other HousingClient users.
 * Tracks online HC users and provides lookup methods for modules.
 */
public class HousingClientUserManager {

    private static HousingClientUserManager instance;

    // Set of UUIDs of players using HousingClient
    private final Set<UUID> housingClientUsers = ConcurrentHashMap.newKeySet();

    // Set of player names (lowercase) for name-based lookup
    private final Set<String> housingClientNames = ConcurrentHashMap.newKeySet();

    // Cache of formatted display names that should be rainbow
    private final Set<String> rainbowNameCache = ConcurrentHashMap.newKeySet();

    private HousingClientUserManager() {
        System.out.println("[HousingClient] HousingClientUserManager initialized!");
    }

    public static HousingClientUserManager getInstance() {
        if (instance == null) {
            instance = new HousingClientUserManager();
        }
        return instance;
    }

    /**
     * Check if a player is using HousingClient.
     */
    public boolean isHousingClientUser(EntityPlayer player) {
        if (player == null)
            return false;
        return housingClientUsers.contains(player.getUniqueID());
    }

    /**
     * Check if a UUID belongs to a HousingClient user.
     */
    public boolean isHousingClientUser(UUID uuid) {
        if (uuid == null)
            return false;
        return housingClientUsers.contains(uuid);
    }

    /**
     * Check if a player name belongs to a HousingClient user.
     * Used for tab list rendering where we only have the name.
     */
    public boolean isHousingClientUserByName(String name) {
        if (name == null || name.isEmpty())
            return false;
        return housingClientNames.contains(name.toLowerCase().trim());
    }

    /**
     * Get all known HousingClient user UUIDs.
     */
    public Set<UUID> getHousingClientUsers() {
        return new HashSet<>(housingClientUsers);
    }

    /**
     * Called when connecting to a new server.
     */
    public void onServerConnect() {
        housingClientUsers.clear();
        housingClientNames.clear();
    }

    /**
     * Called by the Mixin to check if a rendered string should be rainbow.
     */
    public boolean isRainbowName(String displayName) {
        if (displayName == null)
            return false;
        return rainbowNameCache.contains(displayName);
    }

    /**
     * Updates the cache of formatted keys for online HC users.
     * Should be called periodically (e.g. every tick).
     */
    public void updateRainbowNameCache() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getNetHandler() == null)
            return;

        Set<String> newCache = new HashSet<>();

        // Iterate through all online players
        for (net.minecraft.client.network.NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info.getGameProfile() != null && isHousingClientUser(info.getGameProfile().getId())) {
                // Determine the name that would be shown in tab
                String name = info.getDisplayName() != null ? info.getDisplayName().getFormattedText()
                        : net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(info.getPlayerTeam(),
                                info.getGameProfile().getName());

                newCache.add(name);
            }
        }

        rainbowNameCache.clear();
        rainbowNameCache.addAll(newCache);
    }
}
