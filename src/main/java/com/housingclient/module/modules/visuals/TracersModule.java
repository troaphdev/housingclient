package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.FriendsModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ColorSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.HousingClientUserManager;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;

public class TracersModule extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", "Trace to players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Trace to hostile mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", "Trace to passive animals", false);
    private final BooleanSetting npcs = new BooleanSetting("NPCs", "Trace to NPCs", false);
    private final NumberSetting width = new NumberSetting("Width", "Line width", 1.0, 0.5, 5.0, 0.5);

    private final BooleanSetting rankColors = new BooleanSetting("Rank Colors", "Color tracers by Hypixel rank", true);
    private final BooleanSetting roleColors = new BooleanSetting("Role Colors", "Color tracers by Hypixel role", false);

    private boolean lastRank = true;
    private boolean lastRole = false;

    // Colors
    private final ColorSetting playerColor = new ColorSetting("Player Color", "Color for players",
            new Color(255, 50, 50));
    private final ColorSetting mobColor = new ColorSetting("Mob Color", "Color for mobs", new Color(255, 100, 100));
    private final ColorSetting animalColor = new ColorSetting("Animal Color", "Color for animals",
            new Color(100, 255, 100));
    private final ColorSetting npcColor = new ColorSetting("NPC Color", "Color for NPCs", new Color(255, 200, 0));

    public TracersModule() {
        super("Tracers", "Draw lines to entities", Category.VISUALS, ModuleMode.BOTH);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(npcs);
        addSetting(width);
        addSetting(rankColors);
        addSetting(roleColors);
        addSetting(playerColor);
        addSetting(mobColor);
        addSetting(animalColor);
        addSetting(npcColor);
    }

    @Override
    public void onTick() {
        if (rankColors.isEnabled() && !lastRank) {
            roleColors.setValue(false);
        }
        if (roleColors.isEnabled() && !lastRole) {
            rankColors.setValue(false);
        }
        lastRank = rankColors.isEnabled();
        lastRole = roleColors.isEnabled();
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        // Use standard matrix (with bobbing) so entities are stable relative to lines
        // But calculate precise camera position so lines start at the crosshair
        net.minecraft.util.Vec3 cameraPos = RenderUtils.getCameraPos();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer)
                continue;

            if (shouldRender(entity)) {
                // Check if this is a HousingClient user - always use rainbow (no toggle)
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (HousingClientUserManager.getInstance().isHousingClientUser(player)) {
                        renderRainbowTracer(entity, partialTicks, cameraPos);
                        continue;
                    }
                }

                Color color = getColor(entity);
                renderTracer(entity, color, partialTicks, cameraPos);
            }
        }

    }

    private boolean shouldRender(Entity entity) {
        // Exclude FreeCam camera entity (has negative entity ID)
        if (entity.getEntityId() < 0)
            return false;

        if (entity instanceof EntityPlayer) {
            // Check for Bot (Red or White name)
            EntityPlayer player = (EntityPlayer) entity;
            Color color = null;
            if (rankColors.isEnabled()) {
                color = extractColorFromName(getTabDisplayName(player), player.getName());
            }
            // If we can't determine color (disabled setting), fallback to playerColor, so
            // assume Player.
            // But usually we want to filter NPCs. Let's run extraction regardless for
            // checking.
            if (color == null) {
                color = extractColorFromName(getTabDisplayName(player), player.getName());
            }

            // White (255, 255, 255) OR Red (255, 85, 85) OR Null -> Bot/NPC
            boolean isBot = color == null ||
                    (color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255) ||
                    (color.getRed() == 255 && color.getGreen() == 85 && color.getBlue() == 85);

            if (isBot)
                return npcs.isEnabled();
            return players.isEnabled();
        }
        if (entity instanceof EntityMob)
            return mobs.isEnabled();
        if (entity instanceof EntityAnimal)
            return animals.isEnabled();
        if (entity instanceof EntityVillager || entity instanceof EntityArmorStand)
            return npcs.isEnabled();
        return false;
    }

    private Color getColor(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // Check if friend first
            if (FriendsModule.isFriend(entity.getName()))
                return new Color(0, 255, 100);

            // 1. Role Colors - Prioritize suffix/last color
            if (roleColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                Color roleColor = extractRoleColor(displayName);
                if (roleColor != null) {
                    return roleColor;
                }
            }

            // 2. Rank Colors - Prefix/First color
            if (rankColors.isEnabled()) {
                // Try Scoreboard Team first (Most accurate for server teams/colors)
                net.minecraft.scoreboard.Team team = player.getTeam();
                if (team instanceof ScorePlayerTeam) {
                    ScorePlayerTeam sbTeam = (ScorePlayerTeam) team;
                    String prefix = sbTeam.getColorPrefix();
                    Color teamColor = extractColorFromPrefix(prefix);
                    if (teamColor != null) {
                        return teamColor;
                    }
                }

                // Fallback to Tab List Display Name
                String displayName = getTabDisplayName(player);
                Color nameColor = extractColorFromName(displayName, player.getName());
                if (nameColor != null) {
                    return nameColor;
                }
            }

            return playerColor.getValue();
        }
        if (entity instanceof EntityMob)
            return mobColor.getValue();
        if (entity instanceof EntityAnimal)
            return animalColor.getValue();
        return npcColor.getValue();
    }

    private Color extractColorFromPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty())
            return null;
        // Find last color code in prefix
        for (int i = prefix.length() - 2; i >= 0; i--) {
            if (prefix.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(prefix.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

    /**
     * Extract the first valid color code from a formatted name
     */
    private Color extractColorFromName(String displayName, String playerName) {
        if (displayName == null)
            return null;

        // Strategy 1: Find name and scan backwards
        if (playerName != null && !playerName.isEmpty()) {
            int nameIndex = displayName.indexOf(playerName);
            if (nameIndex > -1) {
                for (int i = nameIndex - 1; i >= 0; i--) {
                    if (displayName.charAt(i) == '\u00A7' && i + 1 < displayName.length()) {
                        char code = Character.toLowerCase(displayName.charAt(i + 1));
                        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                            // Ignore Gray for ranks if found immediately before name (sometimes brackets
                            // are gray)
                            // But usually rank color is before brackets.
                            // Let's rely on standard extraction.
                            return getColorFromCode(code);
                        }
                    }
                }
            }
        }

        // Strategy 2: First valid color (skipping brackets)
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                // Skip Gray (7) and Dark Gray (8) - common for brackets
                // Skip Reset (r) and formatting (k-o)
                if ((code >= '0' && code <= '6') || code == '9' || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

    private Color getColorFromCode(char code) {
        switch (code) {
            case '0':
                return new Color(0, 0, 0);
            case '1':
                return new Color(0, 0, 170);
            case '2':
                return new Color(0, 170, 0);
            case '3':
                return new Color(0, 170, 170);
            case '4':
                return new Color(170, 0, 0);
            case '5':
                return new Color(170, 0, 170);
            case '6':
                return new Color(255, 170, 0);
            case '7':
                return new Color(170, 170, 170);
            case '8':
                return new Color(85, 85, 85);
            case '9':
                return new Color(85, 85, 255);
            case 'a':
                return new Color(85, 255, 85);
            case 'b':
                return new Color(85, 255, 255);
            case 'c':
                return new Color(255, 85, 85);
            case 'd':
                return new Color(255, 85, 255);
            case 'e':
                return new Color(255, 255, 85);
            case 'f':
                return new Color(255, 255, 255);
            default:
                return null;
        }
    }

    private Color extractRoleColor(String displayName) {
        if (displayName == null)
            return null;
        // Scan backwards from end for first color code
        for (int i = displayName.length() - 2; i >= 0; i--) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                    return getColorFromCode(code);
                }
            }
        }
        return null;
    }

    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null) {
                if (info.getDisplayName() != null) {
                    return info.getDisplayName().getFormattedText();
                }
                return mc.ingameGUI.getTabList().getPlayerName(info);
            }
        }
        return player.getName();
    }

    private void renderTracer(Entity entity, Color color, float partialTicks, net.minecraft.util.Vec3 cameraPos) {
        // Use 3D rendering with robust start point (offset forward from camera)
        net.minecraft.util.Vec3 startPos = RenderUtils.getCameraPos(-0.2);
        RenderUtils.drawTracer(entity, partialTicks, color.getRGB(), width.getFloatValue(), startPos);
    }

    private void renderRainbowTracer(Entity entity, float partialTicks, net.minecraft.util.Vec3 cameraPos) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

        // Use robust start point
        net.minecraft.util.Vec3 startPos = RenderUtils.getCameraPos(-0.2);

        // Draw Rainbow Line (Center to Entity) in World Space using calculated Camera
        // Pos
        RenderUtils.drawRainbowLine(startPos.xCoord, startPos.yCoord, startPos.zCoord, x, y + entity.height / 2, z,
                width.getFloatValue(), 2000f);
    }
}
