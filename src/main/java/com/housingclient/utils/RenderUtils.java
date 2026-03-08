package com.housingclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Rendering utilities - Fixed for Minecraft 1.8.9
 */
public class RenderUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // Stable Matrix rendering state
    private static boolean originalViewBobbing;
    private static java.lang.reflect.Method setupCameraTransformMethod;

    /**
     * Enables a stable camera matrix for 3D rendering.
     * Use this when you need to render 3D elements (tracers, ESP) that shouldn't
     * wobble with view bobbing.
     * MUST call disableStableMatrix() afterwards.
     * 
     * FIX: Uses orientCamera instead of setupCameraTransform to preserve Projection
     * Matrix (FOV).
     * This prevents entities from sliding/shaking relative to the world geometry
     * when bobbing is enabled.
     * 
     * FIX 2: Uses pushMatrix/popMatrix to isolate the stable matrix state.
     */
    public static void enableStableMatrix(float partialTicks) {
        originalViewBobbing = mc.gameSettings.viewBobbing;
        if (!originalViewBobbing)
            return;

        mc.gameSettings.viewBobbing = false;

        try {
            // Isolates the matrix changes so they don't affect subsequent render calls
            // (ESP, etc.)
            GlStateManager.pushMatrix();

            // Switch to ModelView matrix to reset camera orientation
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.loadIdentity();

            if (setupCameraTransformMethod == null) {
                // Try deobfuscated name first (dev environment)
                try {
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("setupCameraTransform", float.class, int.class);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace(); // Print error to console so we know if it fails
                    // Try SRG name (production environment)
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("func_78479_a", float.class, int.class);
                }
                setupCameraTransformMethod.setAccessible(true);
            }
            setupCameraTransformMethod.invoke(mc.entityRenderer, partialTicks, 0);

        } catch (Exception e) {
            e.printStackTrace(); // Print error to console so we know if it fails
        }
    }

    /**
     * Disables the stable camera matrix and restores original view bobbing
     * settings.
     */
    public static void disableStableMatrix() {
        if (mc.gameSettings.viewBobbing != originalViewBobbing) {
            mc.gameSettings.viewBobbing = originalViewBobbing;
            GlStateManager.popMatrix(); // Restore original matrix
        }
    }

    /**
     * Enables a view matrix that allows bobbing but resets to camera origin.
     * Use this for Tracers where you want the origin at the crosshair (Camera
     * 0,0,0)
     * but the end point to match the bobbed world entities.
     */
    public static void enableBobbedViewMatrix(float partialTicks) {
        try {
            GlStateManager.pushMatrix();

            if (setupCameraTransformMethod == null) {
                // Try deobfuscated name first (dev environment)
                try {
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("setupCameraTransform", float.class, int.class);
                } catch (NoSuchMethodException e) {
                    // Try SRG name (production environment)
                    setupCameraTransformMethod = net.minecraft.client.renderer.EntityRenderer.class
                            .getDeclaredMethod("func_78479_a", float.class, int.class);
                }
                setupCameraTransformMethod.setAccessible(true);
            }
            // Invoke with pass 2 (matches renderWorldPass) -> Sets up Matrix + FOV with
            // Bobbing
            setupCameraTransformMethod.invoke(mc.entityRenderer, partialTicks, 2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void disableBobbedViewMatrix() {
        GlStateManager.popMatrix();
    }

    /**
     * Projects a 3D world position to 2D screen coordinates.
     * Uses the current GL matrices (so it respects bobbing, FOV, etc).
     */
    // Reusable buffers for projection to avoid allocation spam
    private static final java.nio.FloatBuffer modelview = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final java.nio.FloatBuffer projection = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private static final java.nio.IntBuffer viewport = org.lwjgl.BufferUtils.createIntBuffer(16);
    private static final java.nio.FloatBuffer screenCoords = org.lwjgl.BufferUtils.createFloatBuffer(3);

    /**
     * Projects a 3D world position to 2D screen coordinates.
     * Uses the current GL matrices (so it respects bobbing, FOV, etc).
     */
    public static double[] toScreen(double x, double y, double z) {
        modelview.clear();
        projection.clear();
        viewport.clear();
        screenCoords.clear();

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        // Reset reads to 0, keeping limit at capacity (16)
        // This prevents crash if glGetFloat doesn't move position (flip would set
        // limit=0)
        modelview.rewind();
        projection.rewind();
        viewport.rewind();

        boolean result = org.lwjgl.util.glu.GLU.gluProject((float) x, (float) y, (float) z, modelview, projection,
                viewport, screenCoords);

        if (result) {
            return new double[] { screenCoords.get(0), mc.displayHeight - screenCoords.get(1), screenCoords.get(2) };
        }
        return null;
    }

    public static void draw2DLine(double x1, double y1, double x2, double y2, int color, float width) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth(); // Ensure line draws on top
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // Setup 2D Ortho for drawing on screen
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, mc.displayWidth, mc.displayHeight, 0, 0, 1000);

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity(); // Reset view matrix to screen space

        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(width);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        wr.pos(x1, y1, 0).endVertex();
        wr.pos(x2, y2, 0).endVertex();
        tessellator.draw();

        // Restore
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.enableDepth(); // Restore depth
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    // ==================== Basic 2D Shapes ====================

    public static void drawRect(double x, double y, double width, double height, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        wr.pos(x, y + height, 0).endVertex();
        wr.pos(x + width, y + height, 0).endVertex();
        wr.pos(x + width, y, 0).endVertex();
        wr.pos(x, y, 0).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Draw rounded rectangle using GL_POLYGON with proper Counter-Clockwise winding
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color) {
        if (radius <= 0 || radius > Math.min(width, height) / 2) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        // Ensure culling is disabled so we see it regardless of winding, but we'll
        // still do CCW
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        // GL_POLYGON handles convex shapes perfectly
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

        // Walk perimeter Counter-Clockwise (CCW)
        // 1. Top-Right Corner (360 -> 270)
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(360 - i); // 360, 355, ... 270
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // 2. Top-Left Corner (270 -> 180)
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(270 - i); // 270, 265, ... 180
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // 3. Bottom-Left Corner (180 -> 90)
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(180 - i); // 180, 175, ... 90
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // 4. Bottom-Right Corner (90 -> 0)
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(90 - i); // 90, 85, ... 0
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull(); // Restore culling
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw rounded rectangle with specific corners rounded
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color,
            boolean tl, boolean tr, boolean br, boolean bl) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

        // 1. Top-Right Corner (360 -> 270)
        if (tr) {
            for (int i = 0; i <= 90; i += 5) {
                double angle = Math.toRadians(360 - i);
                wr.pos(x + width - radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x + width, y, 0).endVertex();
        }

        // 2. Top-Left Corner (270 -> 180)
        if (tl) {
            for (int i = 0; i <= 90; i += 5) {
                double angle = Math.toRadians(270 - i);
                wr.pos(x + radius + Math.cos(angle) * radius, y + radius + Math.sin(angle) * radius, 0).endVertex();
            }
        } else {
            wr.pos(x, y, 0).endVertex();
        }

        // 3. Bottom-Left Corner (180 -> 90)
        if (bl) {
            for (int i = 0; i <= 90; i += 5) {
                double angle = Math.toRadians(180 - i);
                wr.pos(x + radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x, y + height, 0).endVertex();
        }

        // 4. Bottom-Right Corner (90 -> 0)
        if (br) {
            for (int i = 0; i <= 90; i += 5) {
                double angle = Math.toRadians(90 - i);
                wr.pos(x + width - radius + Math.cos(angle) * radius, y + height - radius + Math.sin(angle) * radius, 0)
                        .endVertex();
            }
        } else {
            wr.pos(x + width, y + height, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw a rectangle with only Left corners rounded.
     */
    public static void drawLeftRoundedRect(double x, double y, double width, double height, double radius, int color) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        // Ensure culling is disabled
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

        // Top-Left Corner (270 -> 180) - CCW
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(270 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Bottom-Left Corner (180 -> 90) - CCW
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(180 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Bottom-Right (Square)
        wr.pos(x + width, y + height, 0).endVertex();

        // Top-Right (Square)
        wr.pos(x + width, y, 0).endVertex();

        tessellator.draw();

        // Restore
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Draw a rectangle with only Bottom corners rounded.
     */
    public static void drawBottomRoundedRect(double x, double y, double width, double height, double radius,
            int color) {
        if (radius <= 0) {
            drawRect(x, y, width, height, color);
            return;
        }

        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_POLYGON, DefaultVertexFormats.POSITION);

        // Top-Right (Square)
        wr.pos(x + width, y, 0).endVertex();

        // Top-Left (Square)
        wr.pos(x, y, 0).endVertex();

        // Bottom-Left Corner (180 -> 90) - CCW
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(180 - i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Bottom-Right Corner (90 -> 0) - CCW
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(90 - i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawFilledArcInternal(WorldRenderer wr, Tessellator tessellator,
            double cx, double cy, double radius, int startAngle, int endAngle) {
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();

        for (int i = startAngle; i <= endAngle; i += 5) {
            double angle = Math.toRadians(i);
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();
    }

    public static void drawFilledArc(double cx, double cy, double radius, int startAngle, int endAngle) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        // We need to set the color from the arc drawing context?
        // Wait, the previous method didn't take color?
        // Ah, the internal one uses current color state.
        // But the previous implementation 'drawFilledArc' called
        // 'drawFilledArcInternal'.
        // Let's see how 'drawFilledArcInternal' gets color. It doesn't.
        // It relies on caller setting color.
        // But drawFilledArc wrapper DOESN'T set color!
        // So the user must set color before calling drawFilledArc?
        // The previous usage in drawRoundedRect set color before calling
        // drawFilledArcInternal.

        drawFilledArcInternal(wr, tessellator, cx, cy, radius, startAngle, endAngle);

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRoundedBorderedRect(double x, double y, double width, double height,
            double radius, int fillColor, int borderColor, float borderWidth) {
        drawRoundedRect(x, y, width, height, radius, fillColor);
        if (borderWidth > 0) {
            drawRoundedRectOutline(x, y, width, height, radius, borderColor, borderWidth);
        }
    }

    public static void drawAnimatedGradientBorder(double x, double y, double width, double height, double radius,
            float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);

        long time = System.currentTimeMillis();
        // Total perimeter (approx) to normalize progress
        double perimeter = 2 * (width + height);
        double currentDist = 0;

        // --- Top Edge ---
        // Top Edge (Left to Right)
        for (double ix = radius; ix <= width - radius; ix += 5) {
            drawVertexWithColor(x + ix, y, currentDist + (ix - radius), perimeter, time, wr);
        }
        currentDist += (width - 2 * radius);

        // Top-Right Corner
        for (int i = 270; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            double px = x + width - radius + Math.cos(angle) * radius;
            double py = y + radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 270) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

        // Right Edge (Top to Bottom)
        for (double iy = radius; iy <= height - radius; iy += 5) {
            drawVertexWithColor(x + width, y + iy, currentDist + (iy - radius), perimeter, time, wr);
        }
        currentDist += (height - 2 * radius);

        // Bottom-Right Corner
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(i);
            double px = x + width - radius + Math.cos(angle) * radius;
            double py = y + height - radius + Math.sin(angle) * radius;
            double arcProgress = (i / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

        // Bottom Edge (Right to Left)
        for (double ix = width - radius; ix >= radius; ix -= 5) {
            drawVertexWithColor(x + ix, y + height, currentDist + (width - radius - ix), perimeter, time, wr);
        }
        currentDist += (width - 2 * radius);

        // Bottom-Right Corner (actually Bottom-Left)
        for (int i = 90; i <= 180; i += 5) {
            double angle = Math.toRadians(i);
            double px = x + radius + Math.cos(angle) * radius;
            double py = y + height - radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 90) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }
        currentDist += (Math.PI * radius / 2.0);

        // Left Edge (Bottom to Top)
        for (double iy = height - radius; iy >= radius; iy -= 5) {
            drawVertexWithColor(x, y + iy, currentDist + (height - radius - iy), perimeter, time, wr);
        }
        currentDist += (height - 2 * radius);

        // Top-Left Corner
        for (int i = 180; i <= 270; i += 5) {
            double angle = Math.toRadians(i);
            double px = x + radius + Math.cos(angle) * radius;
            double py = y + radius + Math.sin(angle) * radius;
            double arcProgress = ((i - 180) / 90.0) * (Math.PI * radius / 2.0);
            drawVertexWithColor(px, py, currentDist + arcProgress, perimeter, time, wr);
        }

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawVertexWithColor(double x, double y, double dist, double perimeter, long time,
            WorldRenderer wr) {
        float progress = (float) (dist / perimeter);
        // Animation speed
        float animSpeed = 3000f;
        float offset = (time % (long) animSpeed) / animSpeed;

        float hue = (progress - offset);
        if (hue < 0)
            hue += 1.0f;

        // Brand colors: Cyan (0.5) to Purple (0.75)
        // Triangle wave to oscillate smoothly
        float subHue = hue * 2;
        if (subHue > 1)
            subHue = 2 - subHue;

        float finalHue = 0.5f + (subHue * 0.25f); // 0.5 to 0.75

        int color = Color.HSBtoRGB(finalHue, 0.8f, 1.0f);

        float alpha = 1.0f;
        float r = (color >> 16 & 0xFF) / 255.0F;
        float g = (color >> 8 & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        wr.pos(x, y, 0).color(r, g, b, alpha).endVertex();
    }

    public static void drawRoundedRectOutline(double x, double y, double width, double height,
            double radius, int color, float lineWidth) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);
        GL11.glLineWidth(lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION);

        // Bottom-Right Corner (0 - 90 degrees)
        for (int i = 0; i <= 90; i += 5) {
            double angle = Math.toRadians(i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Bottom-Left Corner (90 - 180 degrees)
        for (int i = 90; i <= 180; i += 5) {
            double angle = Math.toRadians(i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + height - radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Top-Left Corner (180 - 270 degrees)
        for (int i = 180; i <= 270; i += 5) {
            double angle = Math.toRadians(i);
            wr.pos(x + radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        // Top-Right Corner (270 - 360 degrees)
        for (int i = 270; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            wr.pos(x + width - radius + Math.cos(angle) * radius,
                    y + radius + Math.sin(angle) * radius, 0).endVertex();
        }

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawCircle(double cx, double cy, double radius, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        if (alpha == 0)
            alpha = 1.0f;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
        wr.pos(cx, cy, 0).endVertex();
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            wr.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawGlow(double x, double y, double width, double height, int glowSize, int glowColor) {
        int baseAlpha = (glowColor >> 24) & 0xFF;
        if (baseAlpha == 0)
            baseAlpha = 60;

        for (int i = glowSize; i > 0; i--) {
            float progress = (float) i / glowSize;
            int alpha = (int) (baseAlpha * (1 - progress) * 0.6f);
            int color = (alpha << 24) | (glowColor & 0x00FFFFFF);
            drawRoundedRect(x - i, y - i, width + i * 2, height + i * 2, 4, color);
        }
    }

    // ==================== Gradient Effects ====================

    public static void drawGradientRect(double x, double y, double width, double height,
            int startColor, int endColor, boolean horizontal) {
        float startAlpha = (startColor >> 24 & 0xFF) / 255.0F;
        float startRed = (startColor >> 16 & 0xFF) / 255.0F;
        float startGreen = (startColor >> 8 & 0xFF) / 255.0F;
        float startBlue = (startColor & 0xFF) / 255.0F;

        float endAlpha = (endColor >> 24 & 0xFF) / 255.0F;
        float endRed = (endColor >> 16 & 0xFF) / 255.0F;
        float endGreen = (endColor >> 8 & 0xFF) / 255.0F;
        float endBlue = (endColor & 0xFF) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        if (horizontal) {
            wr.pos(x, y + height, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            wr.pos(x + width, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        } else {
            wr.pos(x, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y + height, 0).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            wr.pos(x + width, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            wr.pos(x, y, 0).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        }

        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static int blendColors(int color1, int color2, float ratio) {
        float ir = 1.0f - ratio;
        int a = (int) (((color1 >> 24) & 0xFF) * ir + ((color2 >> 24) & 0xFF) * ratio);
        int r = (int) (((color1 >> 16) & 0xFF) * ir + ((color2 >> 16) & 0xFF) * ratio);
        int g = (int) (((color1 >> 8) & 0xFF) * ir + ((color2 >> 8) & 0xFF) * ratio);
        int b = (int) ((color1 & 0xFF) * ir + (color2 & 0xFF) * ratio);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void scissor(int x, int y, int width, int height) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        int screenHeight = mc.displayHeight;
        GL11.glScissor(x * scale, screenHeight - (y + height) * scale, width * scale, height * scale);
    }

    public static void scissorCustomScale(int x, int y, int width, int height, double scaleFactor) {
        int screenHeight = mc.displayHeight;
        int scX = (int) (x * scaleFactor);
        int scY = (int) (screenHeight - (y + height) * scaleFactor);
        int scW = (int) (width * scaleFactor);
        int scH = (int) (height * scaleFactor);
        GL11.glScissor(scX, scY, scW, scH);
    }

    // ==================== 3D Rendering ====================

    public static void drawBlockESP(BlockPos pos, Color color, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double x = pos.getX() - rm.viewerPosX;
        double y = pos.getY() - rm.viewerPosY;
        double z = pos.getZ() - rm.viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        drawOutlinedBox(bb, color, lineWidth);
    }

    public static void drawBlockESPFilled(BlockPos pos, Color fillColor, Color outlineColor, float lineWidth) {
        RenderManager rm = mc.getRenderManager();
        double x = pos.getX() - rm.viewerPosX;
        double y = pos.getY() - rm.viewerPosY;
        double z = pos.getZ() - rm.viewerPosZ;
        AxisAlignedBB bb = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);
        drawFilledBox(bb, fillColor);
        drawOutlinedBox(bb, outlineColor, lineWidth);
    }

    public static void drawOutlinedBox(AxisAlignedBB bb, Color color, float lineWidth) {
        float alpha = color.getAlpha() / 255.0F;
        float red = color.getRed() / 255.0F;
        float green = color.getGreen() / 255.0F;
        float blue = color.getBlue() / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GlStateManager.disableLighting();

        // Bottom
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        // Top
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        // Verticals
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        tessellator.draw();

        GL11.glLineWidth(1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawFilledBox(AxisAlignedBB bb, int color) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);

        // All 6 faces
        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        wr.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        wr.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        wr.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        wr.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();

        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawFilledBox(AxisAlignedBB bb, Color color) {
        drawFilledBox(bb, color.getRGB());
    }

    public static void drawNametagAt(String text, double x, double y, double z) {
        RenderManager rm = mc.getRenderManager();

        double renderX = x - rm.viewerPosX;
        double renderY = y - rm.viewerPosY;
        double renderZ = z - rm.viewerPosZ;

        float scale = 0.02666667f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(renderX, renderY, renderZ);
        GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);

        int textWidth = mc.fontRendererObj.getStringWidth(text);
        drawRect(-textWidth / 2 - 2, -2, textWidth + 4, mc.fontRendererObj.FONT_HEIGHT + 4, 0x80000000);

        GlStateManager.enableTexture2D();
        mc.fontRendererObj.drawString(text, -textWidth / 2, 0, 0xFFFFFFFF);

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
            int color, float lineWidth) {
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.color(red, green, blue, alpha);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
        GlStateManager.disableLighting();
        wr.pos(x1, y1, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0f);
        GlStateManager.popMatrix();
    }

    public static Vec3 getCameraPos() {
        modelview.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        modelview.rewind();
        float m0 = modelview.get(0), m1 = modelview.get(1), m2 = modelview.get(2);
        float m4 = modelview.get(4), m5 = modelview.get(5), m6 = modelview.get(6);
        float m8 = modelview.get(8), m9 = modelview.get(9), m10 = modelview.get(10);
        float tx = modelview.get(12), ty = modelview.get(13), tz = modelview.get(14);

        double x = -(m0 * tx + m1 * ty + m2 * tz);
        double y = -(m4 * tx + m5 * ty + m6 * tz);
        double z = -(m8 * tx + m9 * ty + m10 * tz);
        return new Vec3(x, y, z);
    }

    /**
     * Gets the camera position in World Space with an offset relative to the Camera
     * View.
     * 
     * @param zOffset Forward offset (negative is forward, positive is backward)
     */
    public static Vec3 getCameraPos(double zOffset) {
        modelview.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        modelview.rewind();
        float m0 = modelview.get(0), m1 = modelview.get(1), m2 = modelview.get(2);
        float m4 = modelview.get(4), m5 = modelview.get(5), m6 = modelview.get(6);
        float m8 = modelview.get(8), m9 = modelview.get(9), m10 = modelview.get(10);
        float tx = modelview.get(12), ty = modelview.get(13), tz = modelview.get(14);

        // Camera Pos = -R^T * T
        double x = -(m0 * tx + m1 * ty + m2 * tz);
        double y = -(m4 * tx + m5 * ty + m6 * tz);
        double z = -(m8 * tx + m9 * ty + m10 * tz);

        // Add Offset: R^T * (0, 0, offset)
        // x += m2 * offset
        // y += m6 * offset
        // z += m10 * offset
        x += m2 * zOffset;
        y += m6 * zOffset;
        z += m10 * zOffset;

        return new Vec3(x, y, z);
    }

    public static void drawTracer(Entity entity, float partialTicks, int color, float lineWidth, Vec3 startPos) {
        RenderManager rm = mc.getRenderManager();
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - rm.viewerPosX;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - rm.viewerPosY;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - rm.viewerPosZ;

        // Draw from calculated start (Camera) to Entity
        drawLine(startPos.xCoord, startPos.yCoord, startPos.zCoord, x, y + entity.height / 2, z, color, lineWidth);
    }

    // Legacy support if needed, but we should use the one above
    public static void drawTracer(Entity entity, float partialTicks, int color, float lineWidth) {
        drawTracer(entity, partialTicks, color, lineWidth, getCameraPos());
    }

    public static void drawTracer(double x, double y, double z, Color color, float lineWidth) {
        // In 3D render space after modelview matrix is set, (0,0,0) is the camera
        // position
        // which corresponds to the crosshair center on screen
        drawLine(0, 0, 0, x, y, z, color.getRGB(), lineWidth);
    }

    // ==================== Utility Methods ====================

    public static void drawShadow(double x, double y, double width, double height, int shadowSize, int shadowColor) {
        int baseAlpha = (shadowColor >> 24) & 0xFF;
        if (baseAlpha == 0)
            baseAlpha = 40;

        for (int i = shadowSize; i > 0; i--) {
            float progress = (float) (shadowSize - i) / shadowSize;
            int alpha = (int) (baseAlpha * (1 - progress * progress));
            int color = (alpha << 24) | (shadowColor & 0x00FFFFFF);
            drawRoundedRect(x - i, y - i, width + i * 2, height + i * 2, 8 + i, color);
        }
    }

    public static void drawHorizontalLine(double x, double y, double width, double height, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawVerticalLine(double x, double y, double height, double width, int color) {
        drawRect(x, y, width, height, color);
    }

    public static void drawPill(double x, double y, double width, double height, int color) {
        double radius = height / 2;
        drawRoundedRect(x, y, width, height, radius, color);
    }

    public static void drawBorderedRect(double x, double y, double width, double height,
            double borderWidth, int fillColor, int borderColor) {
        drawRect(x, y, width, height, borderColor);
        drawRect(x + borderWidth, y + borderWidth, width - borderWidth * 2, height - borderWidth * 2, fillColor);
    }

    public static ScaledResolution getScaledResolution() {
        return new ScaledResolution(mc);
    }

    public static void drawGlowingText(FontRenderer fr, String text, int x, int y) {
        // Strip formatting just in case, though we force bold usually
        String clean = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);

        long time = System.currentTimeMillis();

        // Per-character Gradient + Glow
        int currentX = x;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            String ch = String.valueOf(c);

            // Calculate Color (Cyan -> Blue/Purple)
            double offset = (i * 0.1) + (time / 1000.0);
            double val = Math.sin(offset); // -1..1
            // Map to hue ~0.5 (Cyan) to ~0.7 (Purple)
            float hue = 0.5f + (float) (val * 0.2f);
            int color = Color.HSBtoRGB(hue, 0.7f, 1.0f);

            // Bloom / Glow Effect -> 8-Pass Circular Blur (High Quality)
            // We draw at 8 sub-pixel offsets to create a smooth "smear" roughly 0.5px
            // around the text.
            // This eliminates visible "ghost" pixels while creating a rich, soft aura.

            int glowAlpha = 0x15000000; // ~8% alpha per pass (x8 passes = ~64% max density)
            int glowColor = (color & 0x00FFFFFF) | glowAlpha;

            // Radius 0.5f (Tight glow)
            // 1. Cardinals
            fr.drawString("\u00A7l" + ch, currentX, y - 0.5f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX, y + 0.5f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX - 0.5f, y, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.5f, y, glowColor, false);

            // 2. Diagonals (approx 0.35f to maintain ~0.5 distance)
            fr.drawString("\u00A7l" + ch, currentX - 0.35f, y - 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.35f, y - 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX - 0.35f, y + 0.35f, glowColor, false);
            fr.drawString("\u00A7l" + ch, currentX + 0.35f, y + 0.35f, glowColor, false);

            // Main Text (Bold)
            fr.drawString("\u00A7l" + ch, currentX, y, color, false);

            // Advance cursor
            currentX += fr.getStringWidth("\u00A7l" + ch);
        }
    }

    // ==================== Rainbow Color Utilities ====================

    /**
     * Get an animated rainbow color.
     * 
     * @param offset Offset to create variation between different elements (0.0 -
     *               1.0)
     * @return Animated rainbow Color
     */
    public static Color getRainbowColor(float offset) {
        long time = System.currentTimeMillis();
        float speed = 2000f; // Full cycle every 2 seconds
        float hue = ((time % (long) speed) / speed + offset) % 1.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    /**
     * Get an animated rainbow color with custom speed.
     * 
     * @param offset  Offset to create variation (0.0 - 1.0)
     * @param speedMs Time in ms for a full rainbow cycle
     * @return Animated rainbow Color
     */
    public static Color getRainbowColor(float offset, float speedMs) {
        long time = System.currentTimeMillis();
        float hue = ((time % (long) speedMs) / speedMs + offset) % 1.0f;
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    /**
     * Get a rainbow color for a specific position in a gradient.
     * Used for gradient effects across lines or outlines.
     * 
     * @param progress  Position in gradient (0.0 - 1.0)
     * @param animSpeed Animation speed in ms
     * @return Rainbow Color at that position
     */
    public static Color getRainbowGradient(double progress, float animSpeed) {
        long time = System.currentTimeMillis();
        float offset = (time % (long) animSpeed) / animSpeed;
        float hue = (float) ((progress + offset) % 1.0);
        return Color.getHSBColor(hue, 0.8f, 1.0f);
    }

    /**
     * Get rainbow color as RGB int for a specific position.
     * 
     * @param progress Position in gradient (0.0 - 1.0)
     * @return RGB int value
     */
    public static int getRainbowRGB(double progress) {
        return getRainbowGradient(progress, 2000f).getRGB();
    }

    /**
     * Draw a 3D line with rainbow gradient effect.
     * Used for tracers to HC users.
     */
    public static void drawRainbowLine(double x1, double y1, double z1, double x2, double y2, double z2,
            float lineWidth, float animSpeed) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        GlStateManager.disableLighting();

        // Start color
        Color startColor = getRainbowGradient(0.0, animSpeed);
        // End color
        Color endColor = getRainbowGradient(0.5, animSpeed);

        wr.pos(x1, y1, z1).color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), 255).endVertex();
        wr.pos(x2, y2, z2).color(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), 255).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GL11.glLineWidth(1.0f);
        GlStateManager.popMatrix();
    }

    /**
     * Draw an outlined box with animated seamless rainbow gradient.
     * Uses GL_LINE_LOOP for each face to create seamless gradient flow.
     */
    public static void drawRainbowOutlinedBox(AxisAlignedBB bb, float lineWidth, float animSpeed) {
        long time = System.currentTimeMillis();
        float offset = (time % (long) animSpeed) / animSpeed;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glLineWidth(lineWidth);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer wr = tessellator.getWorldRenderer();

        // Draw bottom face loop with seamless gradient
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        addRainbowVertex(wr, bb.minX, bb.minY, bb.minZ, offset + 0.0f);
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.minZ, offset + 0.0833f);
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.maxZ, offset + 0.1666f);
        addRainbowVertex(wr, bb.minX, bb.minY, bb.maxZ, offset + 0.25f);
        tessellator.draw();

        // Draw top face loop with seamless gradient
        wr.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.minZ, offset + 0.333f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.minZ, offset + 0.416f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.maxZ, offset + 0.5f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.maxZ, offset + 0.583f);
        tessellator.draw();

        // Draw vertical edges connecting top and bottom
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        // Edge 1
        addRainbowVertex(wr, bb.minX, bb.minY, bb.minZ, offset + 0.666f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.minZ, offset + 0.75f);
        // Edge 2
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.minZ, offset + 0.75f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.minZ, offset + 0.833f);
        // Edge 3
        addRainbowVertex(wr, bb.maxX, bb.minY, bb.maxZ, offset + 0.833f);
        addRainbowVertex(wr, bb.maxX, bb.maxY, bb.maxZ, offset + 0.916f);
        // Edge 4
        addRainbowVertex(wr, bb.minX, bb.minY, bb.maxZ, offset + 0.916f);
        addRainbowVertex(wr, bb.minX, bb.maxY, bb.maxZ, offset + 1.0f);
        tessellator.draw();

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GL11.glLineWidth(1.0f);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void addRainbowVertex(WorldRenderer wr, double x, double y, double z, float hueOffset) {
        float hue = hueOffset % 1.0f;
        Color c = Color.getHSBColor(hue, 0.8f, 1.0f);
        wr.pos(x, y, z).color(c.getRed(), c.getGreen(), c.getBlue(), 255).endVertex();
    }

    /**
     * Draws a string with a rainbow wave effect.
     * 
     * @param text   The text to draw
     * @param x      The x position
     * @param y      The y position
     * @param shadow Whether to draw shadow
     */
    public static int drawRainbowString(String text, float x, float y, boolean shadow) {
        FontRenderer fr = mc.fontRendererObj;
        float currentX = x;

        // Strip color codes to avoid rendering artifacts like '§a'
        String cleanText = net.minecraft.util.EnumChatFormatting.getTextWithoutFormattingCodes(text);
        if (cleanText == null)
            cleanText = "";

        // Time-based offset for animation
        long time = System.currentTimeMillis();
        float hueSpeed = 3000f; // Slower speed = faster cycle
        float timeOffset = (time % (long) hueSpeed) / hueSpeed;

        for (int i = 0; i < cleanText.length(); i++) {
            char c = cleanText.charAt(i);
            String charStr = String.valueOf(c);

            // Calculate hue based on position and time
            // Position offset makes it a wave
            float hue = timeOffset - (i * 0.05f);
            if (hue < 0)
                hue += 1.0f;

            int color = Color.HSBtoRGB(hue, 0.8f, 1.0f);

            if (shadow) {
                fr.drawStringWithShadow(charStr, currentX, y, color);
            } else {
                fr.drawString(charStr, (int) currentX, (int) y, color);
            }

            currentX += fr.getStringWidth(charStr);
        }

        return (int) currentX;
    }
}
