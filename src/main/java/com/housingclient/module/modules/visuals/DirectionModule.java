package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.FriendsModule;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.utils.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Direction Module (Compass HUD)
 * Renders a compass strip with direction markers (N, S, E, W)
 * Displays teammates (friends) as player heads with green borders
 */
public class DirectionModule extends Module {

    private static final int WIDTH = 160;
    private static final int HEIGHT = 24; // Increased for better spacing
    private static final int MARKER_COLOR = 0xFFFFFFFF;
    private static final int BG_COLOR = 0x66000000; // Semi-transparent black

    public DirectionModule() {
        super("Direction", "Compass HUD with teammate tracking", Category.RENDER, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // Position from Designer
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getDirX() : 5;
        int y = designer != null ? designer.getDirY() : 25;

        // Center on X for the strip (Drawing from center outward logic or just fixed
        // width)
        // Let's treat x as the center of the compass for easier positioning if user
        // drags it?
        // Or x as left edge. Standard HUD usually uses top-left. Let's use x as left
        // edge.
        int centerX = x + WIDTH / 2;

        // Draw Background
        RenderUtils.drawRoundedRect(x, y, WIDTH, HEIGHT, 4, BG_COLOR);

        // Scissor for clipping contents
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(x, y, WIDTH, HEIGHT);

        float yaw = mc.thePlayer.rotationYaw;
        float wrapYaw = MathHelper.wrapAngleTo180_float(yaw); // -180 to 180

        // 0. Draw Ticks (Every 15 degrees)
        for (int i = 0; i < 360; i += 15) {
            boolean isCardinal = (i % 90 == 0);
            boolean isOrdinal = (i % 45 == 0) && !isCardinal;
            drawTick(x, y, wrapYaw, i - 180, isCardinal, isOrdinal);
        }

        // 1. Draw Cardinals (N, E, S, W)
        drawDirection(x, y, wrapYaw, 0, "S");
        drawDirection(x, y, wrapYaw, -90, "E");
        drawDirection(x, y, wrapYaw, 90, "W");
        drawDirection(x, y, wrapYaw, 180, "N");
        drawDirection(x, y, wrapYaw, -180, "N");

        // 1.5 Draw Ordinals (NE, SE, SW, NW)
        drawOrdinal(x, y, wrapYaw, -45, "SE");
        drawOrdinal(x, y, wrapYaw, 45, "SW");
        drawOrdinal(x, y, wrapYaw, 135, "NW");
        drawOrdinal(x, y, wrapYaw, -135, "NE");

        // 2. Draw Teammates
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue;

            String name = player.getName();
            if (FriendsModule.isFriend(name)) {
                drawTeammate(x, y, wrapYaw, player);
            }
        }

        // Draw static center line (Moved to end to be on top)
        RenderUtils.drawRect(x + WIDTH / 2, y + 15, 1, 8, 0xFFFFFFFF);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Border
        RenderUtils.drawRoundedRectOutline(x, y, WIDTH, HEIGHT, 4, 0x40FFFFFF, 1.0f);
    }

    private void drawDirection(int x, int y, float playerYaw, float markerYaw, String text) {
        float angle = markerYaw - playerYaw;

        // Normalize angle to -180 to 180
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        // Field of View on the compass (how many degrees visible)
        // Let's say 90 degrees total visible on the 160px strip
        // Scale: deg -> pixels
        float pxPerDeg = (float) WIDTH / 100.0f;

        float offset = angle * pxPerDeg;

        float textX = x + WIDTH / 2 + offset;

        // Only draw if visible (with some padding)
        if (textX > x - 10 && textX < x + WIDTH + 10) {
            // Opacity fade based on distance from center?
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (MARKER_COLOR & 0x00FFFFFF);

            // Draw
            int strWidth = mc.fontRendererObj.getStringWidth(text);
            // Draw cardinal text slightly larger or different color?
            // User requested plain text.
            mc.fontRendererObj.drawStringWithShadow(text, textX - strWidth / 2.0f, y + 2, color);
        }
    }

    private void drawTick(int x, int y, float playerYaw, float markerYaw, boolean isCardinal, boolean isOrdinal) {
        float angle = markerYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;
        float tickX = x + WIDTH / 2 + offset;

        if (tickX > x - 2 && tickX < x + WIDTH + 2) {
            // Opacity fade
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (0xAAAAAA & 0x00FFFFFF); // Light gray ticks

            // Draw Line
            // Small tick height vs large tick
            int h = isCardinal ? 7 : (isOrdinal ? 5 : 3);
            int yPos = y + HEIGHT - h - 1;
            RenderUtils.drawRect((int) tickX, yPos, 1, h, color);
        }
    }

    /**
     * Helper to draw text for Ordinals (NE, SE, etc)
     */
    private void drawOrdinal(int x, int y, float playerYaw, float markerYaw, String text) {
        float angle = markerYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;
        float textX = x + WIDTH / 2 + offset;

        if (textX > x - 10 && textX < x + WIDTH + 10) {
            int alpha = 255;
            float dist = Math.abs(offset);
            if (dist > (WIDTH / 2 - 20)) {
                alpha = (int) Math.max(0, 255 * (1.0f - (dist - (WIDTH / 2 - 20)) / 20.0f));
            }
            int color = (alpha << 24) | (0xCCCCCC & 0x00FFFFFF); // Slightly darker white for ordinals

            // Smaller text? Standard font is one size.
            // Just draw it.
            int strWidth = mc.fontRendererObj.getStringWidth(text);
            mc.fontRendererObj.drawStringWithShadow(text, textX - strWidth / 2.0f, y + 3, color);
        }
    }

    private void drawTeammate(int x, int y, float playerYaw, EntityPlayer teammate) {
        double dX = teammate.posX - mc.thePlayer.posX;
        double dZ = teammate.posZ - mc.thePlayer.posZ;

        // Calculate angle to player
        // atan2 returns radians -PI to PI. 0 is +Z (South)?
        // Minecraft Yaw: 0 = South, 90 = West, 180 = North, -90 = East
        // Math.atan2(z, x): 0 is +X.
        // We need to convert standard math angle to MC Yaw.

        double degrees = Math.toDegrees(Math.atan2(dZ, dX)) - 90; // Convert to MC Yaw space
        float teammateYaw = (float) degrees;

        float angle = teammateYaw - playerYaw;
        while (angle < -180)
            angle += 360;
        while (angle >= 180)
            angle -= 360;

        float pxPerDeg = (float) WIDTH / 100.0f;
        float offset = angle * pxPerDeg;

        float headX = x + WIDTH / 2 + offset;
        float headSize = 12;

        if (headX > x - headSize && headX < x + WIDTH + headSize) {
            // Bind Skin
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(teammate.getUniqueID());
            ResourceLocation skin = (info != null) ? info.getLocationSkin()
                    : new ResourceLocation("textures/entity/steve.png"); // Fallback

            mc.getTextureManager().bindTexture(skin);
            GlStateManager.color(1, 1, 1, 1);

            // Draw Head (8x8 from 64x64 texture usually, face is 8,8,8,8)
            // Standard Gui.drawScaledCustomSizeModalRect
            Gui.drawScaledCustomSizeModalRect((int) (headX - headSize / 2), y + 3, 8, 8, 8, 8, (int) headSize,
                    (int) headSize, 64, 64);
            // Draw Hat/Overlay (40,8 is start of hat, 8x8 size)
            Gui.drawScaledCustomSizeModalRect((int) (headX - headSize / 2), y + 3, 40, 8, 8, 8, (int) headSize,
                    (int) headSize, 64, 64);

            // Green Border
            RenderUtils.drawRoundedRectOutline((int) (headX - headSize / 2), y + 3, (int) headSize, (int) headSize, 0,
                    0xFF00FF00, 1.0f);
        }
    }

    @Override
    public String getDisplayInfo() {
        return null; // No text for arraylist
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }
}
