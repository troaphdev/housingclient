package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Loaded Players Module
 * Shows players within simulation distance (who have your entity loaded)
 */
public class LoadedPlayersModule extends Module {

    private final BooleanSetting showCountOnly = new BooleanSetting("Count Only", "Show only the player count", false);
    private final NumberSetting maxDistance = new NumberSetting("Max Distance", "Simulation distance threshold", 48.0,
            16.0, 128.0, 8.0);

    private List<EntityPlayer> loadedPlayers = new ArrayList<>();

    public LoadedPlayersModule() {
        super("Loaded Players", "Shows players within simulation distance", Category.VISUALS, ModuleMode.BOTH);

        addSetting(showCountOnly);
        addSetting(maxDistance);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        loadedPlayers.clear();
        double maxDist = maxDistance.getValue();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

            // Skip bots (white names = no color code = Hypixel bot)
            if (isBot(player))
                continue;

            double distance = mc.thePlayer.getDistanceToEntity(player);
            if (distance <= maxDist) {
                loadedPlayers.add(player);
            }
        }
    }

    private boolean isBot(EntityPlayer player) {
        String displayName = getTabDisplayName(player);
        // If no color code at all, it's a bot
        if (!displayName.contains("\u00A7")) {
            return true; // No formatting = bot
        }
        // Check if the first color code is white (§f), dark gray (§8), or red (§c)
        int idx = displayName.indexOf("\u00A7");
        if (idx + 1 < displayName.length()) {
            char code = Character.toLowerCase(displayName.charAt(idx + 1));
            if (code == 'f' || code == '8' || code == 'c') {
                return true; // White, dark gray, or red = bot
            }
        }
        return false;
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getLoadedPlayersX() : 5;
        int y = designer != null ? designer.getLoadedPlayersY() : 50;

        int maxWidth = 0;
        int totalHeight = 0;

        if (showCountOnly.isEnabled()) {
            // Just show the count
            String text = "Loaded: " + loadedPlayers.size();
            mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFFFFFFF);

            maxWidth = mc.fontRendererObj.getStringWidth(text);
            totalHeight = mc.fontRendererObj.FONT_HEIGHT;
        } else {
            // Show header
            String header = "\u00A7bLoaded Players \u00A77(" + loadedPlayers.size() + ")";
            mc.fontRendererObj.drawStringWithShadow(header, x, y, 0xFFFFFFFF);

            maxWidth = mc.fontRendererObj.getStringWidth(header);
            totalHeight += 12; // Header spacing
            y += 12;

            // Show each player with tab colors
            for (EntityPlayer player : loadedPlayers) {
                String displayName = getTabDisplayName(player);
                mc.fontRendererObj.drawStringWithShadow(displayName, x + 4, y, 0xFFFFFFFF);

                int nameWidth = mc.fontRendererObj.getStringWidth(displayName) + 4;
                if (nameWidth > maxWidth)
                    maxWidth = nameWidth;

                totalHeight += 10;
                y += 10;
            }
        }

        // Update dimensions for HUD Designer
        this.setWidth(maxWidth);
        this.setHeight(totalHeight);
    }

    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() == null)
            return player.getName();

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info != null) {
            // Try to get the display name (which includes ranks/colors)
            if (info.getDisplayName() != null) {
                return info.getDisplayName().getFormattedText();
            }
            // Fallback: Use the player list formatting (this is what tab uses)
            return mc.ingameGUI.getTabList().getPlayerName(info);
        }
        return player.getName();
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(loadedPlayers.size());
    }
}
