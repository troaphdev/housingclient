package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.FriendsModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ColorSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.HousingClientUserManager;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.network.NetworkPlayerInfo;

public class NametagsModule extends Module {

    private final NumberSetting scale = new NumberSetting("Scale", "Size of nametags", 1.0, 0.5, 3.0, 0.1);
    private final BooleanSetting showHealth = new BooleanSetting("Show Health", "Display player health", true);
    private final BooleanSetting showDistance = new BooleanSetting("Show Distance", "Display distance to player", true);
    private final BooleanSetting background = new BooleanSetting("Background", "Show background behind name", true);
    private final BooleanSetting friendHighlight = new BooleanSetting("Friend Highlight", "Highlight friends in green",
            true);
    private final ColorSetting friendColor = new ColorSetting("Friend Color", "Color for friends",
            new Color(0, 255, 100));
    private final BooleanSetting armor = new BooleanSetting("Show Armor", "Display armor above name", true);
    private final BooleanSetting invisibles = new BooleanSetting("Invisibles", "Show invisible player nametags", true);
    private final BooleanSetting dynamicScaling = new BooleanSetting("Dynamic Scaling", "Keep constant size on screen",
            false);

    // Track which entities we're rendering custom nametags for
    private static Set<Integer> customNametagEntities = new HashSet<>();

    public NametagsModule() {
        super("Nametags", "Enhanced player nametags", Category.VISUALS, ModuleMode.BOTH);

        addSetting(scale);
        addSetting(showHealth);
        addSetting(showDistance);
        addSetting(background);
        addSetting(friendHighlight);
        addSetting(friendColor);
        addSetting(armor);
        addSetting(invisibles);
        addSetting(dynamicScaling);
    }

    /**
     * Cancel vanilla nametag rendering for ALL entities when our module is enabled
     * This prevents the vanilla nametag from overlapping with our custom one
     */
    @SubscribeEvent
    public void onRenderSpecials(RenderLivingEvent.Specials.Pre<?> event) {
        if (!isEnabled())
            return;

        Entity entity = event.entity;

        // Cancel vanilla nametags ONLY for other players
        // This allows ArmorStands (holograms) and mobs to show their vanilla nametags
        if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
            event.setCanceled(true);
        }
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        customNametagEntities.clear();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;
            if (player.isInvisible() && !invisibles.isEnabled())
                continue;

            // Use NPC detection to skip fake players
            if (isBot(player))
                continue;

            customNametagEntities.add(player.getEntityId());
        }
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

            // Exclude FreeCam camera entity (has negative entity ID)
            if (player.getEntityId() < 0)
                continue;

            if (player.isInvisible() && !invisibles.isEnabled())
                continue;

            // Check HideHykiaEntities
            if (com.housingclient.module.modules.render.HideHykiaEntitiesModule.shouldHide(player))
                continue;

            // Only skip clearly fake entities (NPC detection)
            // Real players have valid game profiles with UUIDs
            if (isBot(player))
                continue;

            renderNametag(player, partialTicks);
        }
    }

    /**
     * Check if entity is likely an NPC, not a real player
     * Real players have proper game profiles and are in the player tab list
     */
    /**
     * Check if entity is a bot based on name color (White/Gray = Bot)
     * User request: "whos names are white" -> Bot.
     * We accept 0-9, a-e (Colors). We reject f (White) and null.
     */
    private boolean isBot(EntityPlayer player) {
        String name = getTabDisplayName(player);
        // Check for valid color codes (0-9, a-e). 'f' is white.
        for (int i = 0; i < name.length() - 1; i++) {
            if (name.charAt(i) == '\u00A7') {
                char code = Character.toLowerCase(name.charAt(i + 1));
                // If it has a color code that is NOT white (f) AND NOT Red (c)
                // Red = NPC/Bot per request
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'e')) {
                    if (code == 'c')
                        continue; // Skip Red (treat as bot color)
                    return false; // Has valid player color -> Not a bot
                }
            }
        }
        return true; // No color, only white, or only red -> Bot
    }

    /**
     * Get the display name from tab list with colors and ranks
     */
    private String getTabDisplayName(EntityPlayer player) {
        if (mc.getNetHandler() != null) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
            if (info != null) {
                // Try to get the display name (which includes ranks/colors from tab)
                if (info.getDisplayName() != null) {
                    return info.getDisplayName().getFormattedText();
                }
                // Fallback: Use the player list formatting
                return mc.ingameGUI.getTabList().getPlayerName(info);
            }
        }
        return player.getName();
    }

    private void renderNametag(EntityPlayer player, float partialTicks) {
        double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
        double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;

        x -= mc.getRenderManager().viewerPosX;
        y -= mc.getRenderManager().viewerPosY;
        z -= mc.getRenderManager().viewerPosZ;

        // Build nametag text
        StringBuilder text = new StringBuilder();

        // Check if friend
        boolean isFriend = FriendsModule.isFriend(player.getName());

        if (isFriend && friendHighlight.isEnabled()) {
            text.append("\u00A7a"); // Green for friends
            text.append(player.getName());
        } else {
            // Use tab list name with colors and ranks
            String tabName = getTabDisplayName(player);
            text.append(tabName);
        }

        if (showHealth.isEnabled()) {
            float health = player.getHealth();
            String healthColor;
            if (health > 15)
                healthColor = "\u00A7a";
            else if (health > 10)
                healthColor = "\u00A7e";
            else if (health > 5)
                healthColor = "\u00A76";
            else
                healthColor = "\u00A7c";

            text.append(" ").append(healthColor).append(String.format("%.1f", health)).append("\u00A7c\u2764");
        }

        if (showDistance.isEnabled()) {
            double dist = mc.thePlayer.getDistanceToEntity(player);
            text.append(" \u00A77[").append(String.format("%.1f", dist)).append("m]");
        }

        // Render
        GlStateManager.pushMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.translate(x, y + player.height + 0.5, z);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0, 1, 0);
        GlStateManager.rotate(mc.getRenderManager().playerViewX, 1, 0, 0);

        float baseScale = (float) (scale.getValue() * 0.025f);

        if (dynamicScaling.isEnabled()) {
            // Use INTERPOLATED position for smoother scaling
            double xDist = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
                    - mc.getRenderManager().viewerPosX;
            double yDist = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
                    - mc.getRenderManager().viewerPosY;
            double zDist = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
                    - mc.getRenderManager().viewerPosZ;

            // Distance to nametag position
            double dist = Math.sqrt(
                    xDist * xDist + (yDist + player.getEyeHeight()) * (yDist + player.getEyeHeight()) + zDist * zDist);

            // Scale linearly with distance so nametags appear constant screen-size.
            // At refDist (4 blocks), nametag is at base size.
            // At 2x refDist, nametag world-size doubles → same screen size.
            float refDist = 4.0f;
            baseScale *= (float) (Math.max(dist, 1.0) / refDist);
        }

        GlStateManager.scale(-baseScale, -baseScale, baseScale);

        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        String displayText = text.toString();
        int textWidth = mc.fontRendererObj.getStringWidth(displayText);

        // Check if this is a HousingClient user - draw rainbow outline (always active)
        boolean isHousingClientUser = HousingClientUserManager.getInstance().isHousingClientUser(player);

        if (background.isEnabled()) {
            int bgColor = isFriend && friendHighlight.isEnabled() ? 0x40008000 : 0x80000000;
            RenderUtils.drawRect(-textWidth / 2 - 2, -2, textWidth + 4, 12, bgColor);
        }

        // Draw rainbow outline for HousingClient users (always active, no toggle)
        if (isHousingClientUser) {
            // Match the background rect: RenderUtils.drawRect(-textWidth / 2 - 2, -2,
            // textWidth + 4, 12, bgColor)
            // The outline should wrap exactly around this background
            float bgX = -textWidth / 2 - 2;
            float bgY = -2;
            float bgWidth = textWidth + 4;
            float bgHeight = 12;
            drawRainbowOutline(bgX, bgY, bgWidth, bgHeight, 1.0f);
        }

        mc.fontRendererObj.drawStringWithShadow(displayText, -textWidth / 2, 0, -1);

        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    protected void onDisable() {
        customNametagEntities.clear();
    }

    /**
     * Check if we should hide vanilla nametag for this entity
     */
    public static boolean shouldHideVanillaNametag(Entity entity) {
        NametagsModule module = HousingClient.getInstance().getModuleManager().getModule(NametagsModule.class);
        if (module == null || !module.isEnabled())
            return false;
        return customNametagEntities.contains(entity.getEntityId());
    }

    /**
     * Draw an animated seamless rainbow outline around a 2D rectangle.
     * Uses simple filled rectangles for reliability across all GL states.
     */
    private void drawRainbowOutline(float x, float y, float width, float height, float borderWidth) {
        long time = System.currentTimeMillis();
        float speed = 2000f;
        float offset = (time % (long) speed) / speed;

        float b = borderWidth;
        float totalPerimeter = 2 * width + 2 * height;
        int segments = 64; // High segment count for smooth gradient

        // Top border
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            float x1 = x + t1 * width;
            float x2 = x + t2 * width;
            float hue = ((t1 + t2) / 2 * width / totalPerimeter + offset) % 1.0f;
            Color col = Color.getHSBColor(hue, 0.8f, 1.0f);
            int color = (255 << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
            RenderUtils.drawRect(x1, y - b, x2 - x1, b, color);
        }

        // Right border
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            float y1 = y + t1 * height;
            float y2 = y + t2 * height;
            float hue = ((width + (t1 + t2) / 2 * height) / totalPerimeter + offset) % 1.0f;
            Color col = Color.getHSBColor(hue, 0.8f, 1.0f);
            int color = (255 << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
            RenderUtils.drawRect(x + width, y1, b, y2 - y1, color);
        }

        // Bottom border
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            float x1 = x + width - t1 * width;
            float x2 = x + width - t2 * width;
            float hue = ((width + height + (t1 + t2) / 2 * width) / totalPerimeter + offset) % 1.0f;
            Color col = Color.getHSBColor(hue, 0.8f, 1.0f);
            int color = (255 << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
            RenderUtils.drawRect(Math.min(x1, x2), y + height, Math.abs(x2 - x1), b, color);
        }

        // Left border
        for (int i = 0; i < segments; i++) {
            float t1 = (float) i / segments;
            float t2 = (float) (i + 1) / segments;
            float y1 = y + height - t1 * height;
            float y2 = y + height - t2 * height;
            float hue = ((2 * width + height + (t1 + t2) / 2 * height) / totalPerimeter + offset) % 1.0f;
            Color col = Color.getHSBColor(hue, 0.8f, 1.0f);
            int color = (255 << 24) | (col.getRed() << 16) | (col.getGreen() << 8) | col.getBlue();
            RenderUtils.drawRect(x - b, Math.min(y1, y2), b, Math.abs(y2 - y1), color);
        }

        // Corners
        Color tlCol = Color.getHSBColor(offset % 1.0f, 0.8f, 1.0f);
        int tlColor = (255 << 24) | (tlCol.getRed() << 16) | (tlCol.getGreen() << 8) | tlCol.getBlue();
        RenderUtils.drawRect(x - b, y - b, b, b, tlColor);

        Color trCol = Color.getHSBColor((width / totalPerimeter + offset) % 1.0f, 0.8f, 1.0f);
        int trColor = (255 << 24) | (trCol.getRed() << 16) | (trCol.getGreen() << 8) | trCol.getBlue();
        RenderUtils.drawRect(x + width, y - b, b, b, trColor);

        Color brCol = Color.getHSBColor(((width + height) / totalPerimeter + offset) % 1.0f, 0.8f, 1.0f);
        int brColor = (255 << 24) | (brCol.getRed() << 16) | (brCol.getGreen() << 8) | brCol.getBlue();
        RenderUtils.drawRect(x + width, y + height, b, b, brColor);

        Color blCol = Color.getHSBColor(((2 * width + height) / totalPerimeter + offset) % 1.0f, 0.8f, 1.0f);
        int blColor = (255 << 24) | (blCol.getRed() << 16) | (blCol.getGreen() << 8) | blCol.getBlue();
        RenderUtils.drawRect(x - b, y + height, b, b, blColor);
    }
}