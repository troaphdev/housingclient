package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.FriendsModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chams Module - Polygon Offset Method
 * 
 * Uses GL_POLYGON_OFFSET_FILL to render the player "in front" of walls
 * while maintaining standard GL_LEQUAL depth testing.
 * 
 * Renders entities with their normal skins visible through walls.
 */
public class ChamsModule extends Module {

    private final BooleanSetting players = new BooleanSetting("Players", "Show players through walls", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Show hostile mobs through walls", false);
    private final BooleanSetting animals = new BooleanSetting("Animals", "Show animals through walls", false);
    private final BooleanSetting friends = new BooleanSetting("Friends", "Show friends through walls", true);
    private final BooleanSetting hideNpcs = new BooleanSetting("Hide NPCs", "Don't show NPCs/bots through walls", true);
    private final NumberSetting alpha = new NumberSetting("Alpha", "Transparency of chams", 0.8, 0.1, 1.0, 0.1);

    public ChamsModule() {
        super("Chams", "See entities through walls", Category.VISUALS, ModuleMode.BOTH);
        addSetting(players);
        addSetting(mobs);
        addSetting(animals);
        addSetting(friends);
        addSetting(hideNpcs);
        addSetting(alpha);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        List<EntityLivingBase> entities = new ArrayList<>();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == mc.thePlayer)
                continue;
            if (!(entity instanceof EntityLivingBase))
                continue;
            if (shouldRenderChams(entity)) {
                entities.add((EntityLivingBase) entity);
            }
        }

        if (entities.isEmpty())
            return;

        // Sort by distance (farthest first)
        entities.sort(Comparator.comparingDouble(e -> -mc.thePlayer.getDistanceSqToEntity(e)));

        for (EntityLivingBase entity : entities) {
            renderEntity(entity, partialTicks);
        }
    }

    private void renderEntity(EntityLivingBase entity, float partialTicks) {
        float alphaVal = alpha.getFloatValue();

        // Check if entity is invisible and TrueSight is handling it
        boolean wasInvisible = false;
        float trueSightAlpha = 1.0f;
        if (entity instanceof EntityPlayer && entity.isInvisible()) {
            TrueSightModule trueSight = HousingClient.getInstance().getModuleManager().getModule(TrueSightModule.class);
            if (trueSight != null && trueSight.isEnabled()) {
                // Temporarily make visible so the renderer will actually draw the model
                entity.setInvisible(false);
                wasInvisible = true;
                // Read TrueSight's player transparency setting
                trueSightAlpha = trueSight.getPlayerAlpha();
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();

        // 1. Setup - Use Depth Range to pull entity to front
        // This keeps self-occlusion working (fixing glint) but renders in front of
        // walls
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthRange(0.0, 0.01); // Render at very front of Z-buffer

        // 3. FORCE Lighting & Brightness
        int i = mc.theWorld.getCombinedLight(entity.getPosition(), 0);
        int j = i % 65536;
        int k = i / 65536;
        // Use MAX brightness so entities are visible through walls
        net.minecraft.client.renderer.OpenGlHelper
                .setLightmapTextureCoords(net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, 240f, 240f);

        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();

        // 4. Keep textures enabled for skins and items
        GlStateManager.enableTexture2D();

        // 5. Blend for transparency
        GlStateManager.enableBlend();
        if (wasInvisible) {
            // Use TrueSight's constant-alpha blending to preserve the ghost transparency
            org.lwjgl.opengl.GL14.glBlendColor(1f, 1f, 1f, trueSightAlpha);
            GL11.glBlendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE_MINUS_CONSTANT_ALPHA);
        } else {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        // 6. Culling
        GlStateManager.enableCull();
        GlStateManager.cullFace(GL11.GL_BACK);

        // 7. Color Material
        GlStateManager.enableColorMaterial();
        GL11.glColorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);

        // Apply alpha for transparency (white = no tint)
        GlStateManager.color(1f, 1f, 1f, alphaVal);

        // Disable shadows to prevent double-shadow rendering (dark shadows)
        mc.getRenderManager().setRenderShadow(false);
        try {
            mc.getRenderManager().renderEntityStatic(entity, partialTicks, false);
        } catch (Exception ignored) {
        }
        mc.getRenderManager().setRenderShadow(true);

        // Restore State
        GL11.glDepthRange(0.0, 1.0); // Reset depth range
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        GlStateManager.disableColorMaterial();
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();

        // Reset blend mode
        if (wasInvisible) {
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL14.glBlendColor(0f, 0f, 0f, 0f);
        }
        GlStateManager.disableBlend();

        // Restore Lightmap
        net.minecraft.client.renderer.OpenGlHelper.setLightmapTextureCoords(
                net.minecraft.client.renderer.OpenGlHelper.lightmapTexUnit, (float) j, (float) k);

        // Reset Color
        GlStateManager.color(1f, 1f, 1f, 1f);

        GlStateManager.popAttrib();
        GlStateManager.popMatrix();

        // Restore invisibility if we temporarily made the entity visible
        if (wasInvisible) {
            entity.setInvisible(true);
        }
    }

    private boolean shouldRenderChams(Entity entity) {
        if (entity.getEntityId() < 0)
            return false;
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;

            // Check if we should hide NPCs/bots
            if (hideNpcs.isEnabled() && isBot(player))
                return false;

            if (FriendsModule.isFriend(player.getName()))
                return friends.isEnabled();
            return players.isEnabled();
        }
        if (entity instanceof EntityMob)
            return mobs.isEnabled();
        if (entity instanceof EntityAnimal)
            return animals.isEnabled();
        return false;
    }

    /**
     * Check if entity is a bot based on name color (White/Gray = Bot)
     * Same logic as NametagsModule
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
                if (info.getDisplayName() != null) {
                    return info.getDisplayName().getFormattedText();
                }
                return mc.ingameGUI.getTabList().getPlayerName(info);
            }
        }
        return player.getName();
    }

    @Override
    public String getDisplayInfo() {
        return null;
    }
}
