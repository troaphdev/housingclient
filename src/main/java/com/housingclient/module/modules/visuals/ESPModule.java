package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.FriendsModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.HousingClientUserManager;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;

public class ESPModule extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", "Show players", true);
    private final BooleanSetting npcs = new BooleanSetting("NPCs", "Show NPCs (villagers, armor stands)", false);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Show hostile mobs", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", "Show passive animals", false);
    private final BooleanSetting self = new BooleanSetting("Self", "Show yourself", false);

    private final BooleanSetting healthBar = new BooleanSetting("Health Bar", "Show health bar", false);
    private final BooleanSetting rankColors = new BooleanSetting("Rank Colors", "Use player rank colors", true);
    private final BooleanSetting roleColors = new BooleanSetting("Role Colors", "Use player role colors (last color)",
            false);

    private boolean lastRank = true;
    private boolean lastRole = false;

    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of ESP lines", 2.0, 0.5, 5.0, 0.5);

    public ESPModule() {
        super("ESP", "See entities through walls", Category.VISUALS, ModuleMode.BOTH);

        addSetting(players);
        addSetting(npcs);
        addSetting(mobs);
        addSetting(animals);
        addSetting(self);
        addSetting(healthBar);
        addSetting(rankColors);
        addSetting(roleColors);
        addSetting(lineWidth);
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

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!shouldRender(entity))
                continue;

            // Check if this is a HousingClient user - always use rainbow (no toggle)
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                if (HousingClientUserManager.getInstance().isHousingClientUser(player)) {
                    renderRainbowESP(entity, partialTicks);
                    continue;
                }
            }

            Color color = getColorForEntity(entity);
            renderESP(entity, color, partialTicks);
        }
    }

    private boolean shouldRender(Entity entity) {
        // Exclude FreeCam camera entity (has negative entity ID)
        if (entity.getEntityId() < 0) {
            return false;
        }

        // Check HideHykiaEntities
        if (com.housingclient.module.modules.render.HideHykiaEntitiesModule.shouldHide(entity)) {
            return false;
        }

        // Self check first
        if (entity == mc.thePlayer) {
            return self.isEnabled();
        }

        // Use if-else chain so each entity only matches ONE category
        // Priority: Players > NPCs > Mobs > Animals
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            // Check Bot (White Name OR Red Name)
            Color color = extractColorFromName(getTabDisplayName(player), player.getName());
            // White (255, 255, 255) OR Red (255, 85, 85) -> Bot/NPC
            boolean isBot = color == null ||
                    (color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255) ||
                    (color.getRed() == 255 && color.getGreen() == 85 && color.getBlue() == 85);

            if (isBot) {
                return npcs.isEnabled(); // Managed by "NPCs" setting
            }

            return players.isEnabled();
        } else if (entity instanceof EntityArmorStand || entity instanceof EntityVillager) {
            return npcs.isEnabled();
        } else if (entity instanceof EntityMob) {
            return mobs.isEnabled();
        } else if (entity instanceof EntityAnimal) {
            return animals.isEnabled();
        }

        return false;
    }

    private Color getColorForEntity(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // Check if friend
            if (FriendsModule.isFriend(player.getName())) {
                return new Color(0, 255, 100); // Green for friends
            }

            // Use name color if enabled - get from tab list like LoadedPlayersModule
            if (rankColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                // Find first valid color code (0-9, a-f), skip formatting codes (k,l,m,n,o,r)
                Color nameColor = extractColorFromName(displayName, player.getName());
                if (nameColor != null) {
                    return nameColor;
                }
            }
            if (roleColors.isEnabled()) {
                String displayName = getTabDisplayName(player);
                Color roleColor = extractRoleColor(displayName);
                if (roleColor != null) {
                    return roleColor;
                }
            }

            return new Color(255, 50, 50); // Red for players
        }
        if (entity instanceof EntityVillager || entity instanceof EntityArmorStand) {
            return new Color(255, 200, 0); // Orange for NPCs
        }
        if (entity instanceof EntityMob) {
            return new Color(255, 100, 100); // Light red for mobs
        }
        if (entity instanceof EntityAnimal) {
            return new Color(100, 255, 100); // Light green for animals
        }
        return new Color(255, 255, 255); // White default
    }

    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() == null)
            return player.getName();

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
        if (info != null) {
            // Try to get the display name (which includes ranks/colors from tab)
            if (info.getDisplayName() != null) {
                return info.getDisplayName().getFormattedText();
            }
            // Fallback: Use the player list formatting (this is what tab uses)
            return mc.ingameGUI.getTabList().getPlayerName(info);
        }
        return player.getName();
    }

    /**
     * Extract the first valid color code from a formatted name
     * Valid colors are 0-9 and a-f, skip formatting codes (k,l,m,n,o,r)
     * Also skip gray (7) and dark gray (8) as these are used for brackets on
     * Hypixel
     */
    /**
     * Extract the color for the player.
     * Strategy 1: Find the player's name in the string and get the last color code
     * before it.
     * Strategy 2: Fallback to first valid color (skipping gray/dark gray to avoid
     * brackets).
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

                        // DEBUG: Print found color logic
                        // System.out.println("ESP DEBUG: Name=" + playerName + " Display=" +
                        // displayName + " FoundCode=" + code);

                        if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                            return getColorFromCode(code);
                        }
                    }
                }
            } else {
                // System.out.println("ESP DEBUG: Name " + playerName + " NOT FOUND in " +
                // displayName);
            }
        }

        // Strategy 2: First valid color (skipping brackets)
        for (int i = 0; i < displayName.length() - 1; i++) {
            if (displayName.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(displayName.charAt(i + 1));
                // Check if it's a color code (0-9, a-f), not a formatting code (k,l,m,n,o,r)
                // Also skip gray (7) and dark gray (8) as these are used for brackets on
                // Hypixel
                if ((code >= '0' && code <= '6') || code == '9' || (code >= 'a' && code <= 'f')) {
                    Color color = getColorFromCode(code);
                    if (color != null) {
                        return color;
                    }
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

    private void renderESP(Entity entity, Color color, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        bb = bb.offset(-entity.posX, -entity.posY, -entity.posZ).offset(x, y, z);

        // Draw outlined box
        float distance = mc.thePlayer.getDistanceToEntity(entity);
        float scaledWidth = lineWidth.getFloatValue();

        // Make lines thinner as they get further away to reduce clutter
        // At 10 blocks: 100% width. At 100 blocks: ~10% width (clamped to min 0.5)
        if (distance > 10) {
            scaledWidth = Math.max(0.1f, scaledWidth * (10f / distance));
        }

        RenderUtils.drawOutlinedBox(bb, color, scaledWidth);

        // Health bar
        if (healthBar.isEnabled() && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            float healthPercent = living.getHealth() / living.getMaxHealth();

            // Draw health bar to the left of entity
            double barX = bb.minX - 0.15;
            double barHeight = bb.maxY - bb.minY;
            double currentHealthHeight = barHeight * healthPercent;

            Color healthColor;
            if (healthPercent > 0.66f)
                healthColor = new Color(0, 255, 0, 200);
            else if (healthPercent > 0.33f)
                healthColor = new Color(255, 255, 0, 200);
            else
                healthColor = new Color(255, 0, 0, 200);

            // Background bar
            AxisAlignedBB bgBar = new AxisAlignedBB(barX - 0.08, bb.minY, bb.minZ, barX, bb.maxY, bb.minZ + 0.08);
            RenderUtils.drawFilledBox(bgBar, new Color(0, 0, 0, 150));

            // Health bar
            AxisAlignedBB hpBar = new AxisAlignedBB(barX - 0.06, bb.minY + 0.02, bb.minZ + 0.02,
                    barX - 0.02, bb.minY + currentHealthHeight - 0.02, bb.minZ + 0.06);
            RenderUtils.drawFilledBox(hpBar, healthColor);
        }
    }

    /**
     * Render ESP with animated rainbow gradient for HousingClient users.
     * This effect is always active and cannot be disabled.
     */
    private void renderRainbowESP(Entity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb = entity.getEntityBoundingBox();
        bb = bb.offset(-entity.posX, -entity.posY, -entity.posZ).offset(x, y, z);

        // Draw rainbow outlined box
        float distance = mc.thePlayer.getDistanceToEntity(entity);
        float scaledWidth = lineWidth.getFloatValue();

        // Scale line width with distance
        if (distance > 10) {
            scaledWidth = Math.max(0.5f, scaledWidth * (10f / distance));
        }

        // Use rainbow rendering - 2 second cycle
        RenderUtils.drawRainbowOutlinedBox(bb, scaledWidth, 2000f);

        // Health bar still uses normal colors
        if (healthBar.isEnabled() && entity instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) entity;
            float healthPercent = living.getHealth() / living.getMaxHealth();

            double barX = bb.minX - 0.15;
            double barHeight = bb.maxY - bb.minY;
            double currentHealthHeight = barHeight * healthPercent;

            Color healthColor;
            if (healthPercent > 0.66f)
                healthColor = new Color(0, 255, 0, 200);
            else if (healthPercent > 0.33f)
                healthColor = new Color(255, 255, 0, 200);
            else
                healthColor = new Color(255, 0, 0, 200);

            AxisAlignedBB bgBar = new AxisAlignedBB(barX - 0.08, bb.minY, bb.minZ, barX, bb.maxY, bb.minZ + 0.08);
            RenderUtils.drawFilledBox(bgBar, new Color(0, 0, 0, 150));

            AxisAlignedBB hpBar = new AxisAlignedBB(barX - 0.06, bb.minY + 0.02, bb.minZ + 0.02,
                    barX - 0.02, bb.minY + currentHealthHeight - 0.02, bb.minZ + 0.06);
            RenderUtils.drawFilledBox(hpBar, healthColor);
        }
    }
}
