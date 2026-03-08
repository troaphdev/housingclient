package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrueSightModule extends Module {

    private final BooleanSetting showBarriers = new BooleanSetting("Barriers", "Show barrier blocks", true);
    private final BooleanSetting showInvisible = new BooleanSetting("Invisible Players", "Show invisible players",
            true);
    private final BooleanSetting showInvisibleNametags = new BooleanSetting("Invisible Nametags",
            "Show nametags of invisible players", true);

    private final NumberSetting barrierRange = new NumberSetting("Barrier Range", "Range to show barriers", 16, 4, 100);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of ESP lines", 1.5, 0.5, 5.0, 0.5);
    private final NumberSetting barrierAlpha = new NumberSetting("Barrier Transparency",
            "Transparency of barriers (0-100%)",
            50, 0, 100, 5);
    private final NumberSetting playerAlpha = new NumberSetting("Player Transparency",
            "Transparency of invisible players (0-100%)", 50, 0, 100, 5);

    // Concurrency-safe set for rendering
    private final Set<BlockPos> barriers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Map to track barriers per chunk for efficient updates
    private final Map<Long, List<BlockPos>> chunkBarrierMap = new ConcurrentHashMap<>();

    private int currentChunkX = 0;
    private int currentChunkZ = 0;
    private static final int CHUNKS_PER_TICK = 5;

    private int displayList = -1;
    private boolean needsUpdate = true;

    // Rebuild cooldown — only recompile display list once every N ticks
    private static final int REBUILD_COOLDOWN_TICKS = 10;
    private int ticksSinceLastRebuild = 0;
    private boolean pendingRebuild = false;

    public TrueSightModule() {
        super("TrueSight", "See barriers and invisible players", Category.VISUALS, ModuleMode.BOTH);

        addSetting(showBarriers);
        addSetting(showInvisible);
        addSetting(showInvisibleNametags);
        addSetting(barrierRange);
        addSetting(lineWidth);
        addSetting(barrierAlpha);
        addSetting(playerAlpha);
    }

    @Override
    public void onEnable() {
        barriers.clear();
        chunkBarrierMap.clear();
        currentChunkX = -10;
        needsUpdate = true;
        pendingRebuild = false;
        ticksSinceLastRebuild = REBUILD_COOLDOWN_TICKS; // allow immediate first build
    }

    @Override
    public void onDisable() {
        if (displayList != -1) {
            org.lwjgl.opengl.GL11.glDeleteLists(displayList, 1);
            displayList = -1;
        }
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        ticksSinceLastRebuild++;

        if (!showBarriers.isEnabled()) {
            if (!barriers.isEmpty()) {
                barriers.clear();
                chunkBarrierMap.clear();
                needsUpdate = true;
            }
            return;
        }

        int range = barrierRange.getIntValue();
        int chunkRange = (range + 15) / 16;

        int playerChunkX = mc.thePlayer.chunkCoordX;
        int playerChunkZ = mc.thePlayer.chunkCoordZ;

        // Reset scanning if out of bounds
        if (currentChunkX < -chunkRange || currentChunkX > chunkRange) {
            currentChunkX = -chunkRange;
            cleanupFarChunks(playerChunkX, playerChunkZ, chunkRange);
        }
        if (currentChunkZ < -chunkRange || currentChunkZ > chunkRange)
            currentChunkZ = -chunkRange;

        boolean changed = false;

        // Process chunks
        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            int scanX = playerChunkX + currentChunkX;
            int scanZ = playerChunkZ + currentChunkZ;

            if (scanChunk(scanX, scanZ, range)) {
                changed = true;
            }

            // Increment Z
            currentChunkZ++;
            if (currentChunkZ > chunkRange) {
                currentChunkZ = -chunkRange;
                currentChunkX++;
                if (currentChunkX > chunkRange) {
                    break;
                }
            }
        }

        if (changed) {
            pendingRebuild = true;
        }

        // Only allow display list rebuild after cooldown
        if (pendingRebuild && ticksSinceLastRebuild >= REBUILD_COOLDOWN_TICKS) {
            needsUpdate = true;
            pendingRebuild = false;
            ticksSinceLastRebuild = 0;
        }
    }

    private boolean scanChunk(int px, int pz, int range) {
        if (!mc.theWorld.getChunkProvider().chunkExists(px, pz)) {
            return false;
        }

        long key = getChunkKey(px, pz);

        Chunk chunk = mc.theWorld.getChunkFromChunkCoords(px, pz);
        List<BlockPos> newList = new ArrayList<>();

        int pY = (int) mc.thePlayer.posY;
        int minY = Math.max(0, pY - range / 2);
        int maxY = Math.min(255, pY + range / 2);

        // Scan the chunk
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (chunk.getBlock(x, y, z) == Blocks.barrier) {
                        newList.add(new BlockPos(px * 16 + x, y, pz * 16 + z));
                    }
                }
            }
        }

        List<BlockPos> oldList = chunkBarrierMap.get(key);

        // Proper change detection — compare actual contents
        boolean hasChanged;
        if (oldList == null) {
            hasChanged = !newList.isEmpty();
        } else if (newList.size() != oldList.size()) {
            hasChanged = true;
        } else {
            // Same size — compare sets for actual content equality
            hasChanged = !new HashSet<>(newList).equals(new HashSet<>(oldList));
        }

        if (!hasChanged)
            return false;

        // Update Maps/Sets ATOMICALLY (prevent flickering)
        if (newList.isEmpty()) {
            if (oldList != null) {
                barriers.removeAll(oldList);
                chunkBarrierMap.remove(key);
            }
        } else {
            // Add new ones FIRST
            barriers.addAll(newList);
            // Remove ones that are in old but NOT in new
            if (oldList != null) {
                Set<BlockPos> newSet = new HashSet<>(newList);
                List<BlockPos> toRemove = new ArrayList<>();
                for (BlockPos p : oldList) {
                    if (!newSet.contains(p))
                        toRemove.add(p);
                }
                barriers.removeAll(toRemove);
            }
            chunkBarrierMap.put(key, newList);
        }

        return true;
    }

    private void cleanupFarChunks(int playerX, int playerZ, int chunkRange) {
        Iterator<Long> it = chunkBarrierMap.keySet().iterator();
        while (it.hasNext()) {
            long key = it.next();
            int z = (int) key;
            int x = (int) (key >> 32);

            if (Math.abs(x - playerX) > chunkRange + 2 || Math.abs(z - playerZ) > chunkRange + 2) {
                List<BlockPos> list = chunkBarrierMap.get(key);
                if (list != null)
                    barriers.removeAll(list);
                it.remove();
            }
        }
    }

    private long getChunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    /** Pack block coordinates into a single long for fast HashSet lookups */
    private static long packCoords(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (y & 0xFFF) << 26) | (z & 0x3FFFFFF);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        // Render barriers
        if (showBarriers.isEnabled()) {
            // Setup GL state once
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            net.minecraft.client.renderer.GlStateManager.pushMatrix();
            net.minecraft.client.renderer.GlStateManager.enableCull();
            net.minecraft.client.renderer.GlStateManager.enableDepth();
            net.minecraft.client.renderer.GlStateManager.depthMask(true);

            double d0 = mc.getRenderManager().viewerPosX;
            double d1 = mc.getRenderManager().viewerPosY;
            double d2 = mc.getRenderManager().viewerPosZ;

            // Compile Display List if needed
            if (displayList == -1 || needsUpdate) {
                if (displayList == -1)
                    displayList = org.lwjgl.opengl.GL11.glGenLists(1);

                org.lwjgl.opengl.GL11.glNewList(displayList, org.lwjgl.opengl.GL11.GL_COMPILE);
                renderBarriersInternal(); // Draw to list
                org.lwjgl.opengl.GL11.glEndList();

                needsUpdate = false;
            }

            // Draw Display List
            net.minecraft.client.renderer.GlStateManager.translate(-d0, -d1, -d2);
            mc.getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.locationBlocksTexture);

            // Set blend props
            float bAlpha = (float) (barrierAlpha.getValue() / 100.0);
            org.lwjgl.opengl.GL14.glBlendColor(1f, 1f, 1f, bAlpha);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_CONSTANT_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_CONSTANT_ALPHA);

            if (barriers.size() > 0 && displayList != -1) {
                org.lwjgl.opengl.GL11.glCallList(displayList);
            }

            // Teardown
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            net.minecraft.client.renderer.GlStateManager.tryBlendFuncSeparate(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                    org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            org.lwjgl.opengl.GL14.glBlendColor(0f, 0f, 0f, 0f);
            net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            net.minecraft.client.renderer.GlStateManager.popMatrix();
            net.minecraft.client.renderer.GlStateManager.disableBlend();
        }

        // Render invisible players as Ghosts
        if (showInvisible.isEnabled()) {
            try {
                List<EntityPlayer> players = new ArrayList<>(mc.theWorld.playerEntities);
                for (EntityPlayer player : players) {
                    if (player == mc.thePlayer)
                        continue;
                    if (!player.isInvisible())
                        continue;

                    renderInvisiblePlayer(player, partialTicks);
                }
            } catch (Exception ignored) {
                // Guard against ConcurrentModificationException
            }
        }
    }

    private void renderBarriersInternal() {
        // Snapshot the barrier set into a packed-coordinate HashSet for O(1) lookups
        // without ConcurrentHashMap overhead or BlockPos allocation per neighbor check
        Set<Long> packedSet = new HashSet<>(barriers.size() * 2);
        List<BlockPos> snapshot = new ArrayList<>(barriers.size());
        try {
            for (BlockPos pos : barriers) {
                snapshot.add(pos);
                packedSet.add(packCoords(pos.getX(), pos.getY(), pos.getZ()));
            }
        } catch (Exception ignored) {
            // ConcurrentHashMap iteration is weakly consistent, but guard just in case
        }

        if (snapshot.isEmpty())
            return;

        // Get Red Wool Texture
        net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = mc.getBlockRendererDispatcher()
                .getBlockModelShapes().getTexture(Blocks.wool.getStateFromMeta(14));

        net.minecraft.client.renderer.Tessellator tessellator = net.minecraft.client.renderer.Tessellator
                .getInstance();
        net.minecraft.client.renderer.WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        worldrenderer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_TEX);

        for (BlockPos pos : snapshot) {
            int bx = pos.getX();
            int by = pos.getY();
            int bz = pos.getZ();

            // Smart Culling using packed-coordinate lookups (no BlockPos allocation)
            boolean hasWest = packedSet.contains(packCoords(bx - 1, by, bz));
            boolean hasEast = packedSet.contains(packCoords(bx + 1, by, bz));
            boolean hasDown = packedSet.contains(packCoords(bx, by - 1, bz));
            boolean hasUp = packedSet.contains(packCoords(bx, by + 1, bz));
            boolean hasNorth = packedSet.contains(packCoords(bx, by, bz - 1));
            boolean hasSouth = packedSet.contains(packCoords(bx, by, bz + 1));

            // Skip fully enclosed barriers (all 6 faces hidden)
            if (hasWest && hasEast && hasDown && hasUp && hasNorth && hasSouth)
                continue;

            double offset = 0.002;
            double x = bx;
            double y = by;
            double z = bz;

            double minU = sprite.getMinU();
            double maxU = sprite.getMaxU();
            double minV = sprite.getMinV();
            double maxV = sprite.getMaxV();

            double minX = hasWest ? x : x + offset;
            double maxX = hasEast ? x + 1 : x + 1 - offset;
            double minY = hasDown ? y : y + offset;
            double maxY = hasUp ? y + 1 : y + 1 - offset;
            double minZ = hasNorth ? z : z + offset;
            double maxZ = hasSouth ? z + 1 : z + 1 - offset;

            // DOWN
            if (!hasDown) {
                worldrenderer.pos(maxX, minY, maxZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(maxX, minY, minZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(minX, minY, minZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(minX, minY, maxZ).tex(minU, minV).endVertex();
            }
            // UP
            if (!hasUp) {
                worldrenderer.pos(maxX, maxY, minZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(maxX, maxY, maxZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(minX, maxY, maxZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(minX, maxY, minZ).tex(minU, minV).endVertex();
            }
            // NORTH
            if (!hasNorth) {
                worldrenderer.pos(minX, maxY, minZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(minX, minY, minZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(maxX, minY, minZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(maxX, maxY, minZ).tex(minU, minV).endVertex();
            }
            // SOUTH
            if (!hasSouth) {
                worldrenderer.pos(maxX, maxY, maxZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(maxX, minY, maxZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(minX, minY, maxZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(minX, maxY, maxZ).tex(minU, minV).endVertex();
            }
            // WEST
            if (!hasWest) {
                worldrenderer.pos(minX, maxY, maxZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(minX, minY, maxZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(minX, minY, minZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(minX, maxY, minZ).tex(minU, minV).endVertex();
            }
            // EAST
            if (!hasEast) {
                worldrenderer.pos(maxX, maxY, minZ).tex(maxU, minV).endVertex();
                worldrenderer.pos(maxX, minY, minZ).tex(maxU, maxV).endVertex();
                worldrenderer.pos(maxX, minY, maxZ).tex(minU, maxV).endVertex();
                worldrenderer.pos(maxX, maxY, maxZ).tex(minU, minV).endVertex();
            }
        }

        tessellator.draw();
    }

    private void renderInvisiblePlayer(EntityPlayer player, float partialTicks) {
        // Temporarily make player visible for rendering
        player.setInvisible(false);

        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        net.minecraft.client.renderer.GlStateManager.enableBlend();

        // Enable Culling and Depth Mask for players too
        net.minecraft.client.renderer.GlStateManager.enableCull();
        net.minecraft.client.renderer.GlStateManager.enableDepth();
        net.minecraft.client.renderer.GlStateManager.depthMask(true);

        // Use constant alpha blending to force transparency on the entity model
        float pAlpha = (float) (playerAlpha.getValue() / 100.0);
        org.lwjgl.opengl.GL14.glBlendColor(1f, 1f, 1f, pAlpha);
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_CONSTANT_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_CONSTANT_ALPHA);

        try {
            mc.getRenderManager().renderEntityStatic(player, partialTicks, false);
        } catch (Exception e) {
            // Prevent crash if rendering fails
        }

        // Reset blend func
        org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Reset blend color to default (0,0,0,0)
        org.lwjgl.opengl.GL14.glBlendColor(0f, 0f, 0f, 0f);

        // RESET COLOR TO WHITE TO PREVENT BLEEDING
        net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        net.minecraft.client.renderer.GlStateManager.disableBlend();
        net.minecraft.client.renderer.GlStateManager.popMatrix();

        // Restore invisibility
        player.setInvisible(true);

        // Draw nametag manually if enabled and not already handled by
        // renderEntityStatic (which usually doesn't for invisible)
        if (showInvisibleNametags.isEnabled()) {
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
                    - mc.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
                    - mc.getRenderManager().viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
                    - mc.getRenderManager().viewerPosZ;

            String text = "\u00A7c[GHOST] \u00A7f" + player.getName();
            RenderUtils.drawNametagAt(text, x, y + player.height + 0.5, z);
        }
    }

    @Override
    public String getDisplayInfo() {
        int count = barriers.size();
        try {
            List<EntityPlayer> players = new ArrayList<>(mc.theWorld.playerEntities);
            for (EntityPlayer p : players) {
                if (p.isInvisible() && p != mc.thePlayer) {
                    count++;
                }
            }
        } catch (Exception ignored) {
            // Guard against ConcurrentModificationException
        }
        return count > 0 ? String.valueOf(count) : null;
    }

    /**
     * Returns the player transparency as a 0.0-1.0 float.
     * Used by ChamsModule to match the transparency when rendering invisible
     * players through walls.
     */
    public float getPlayerAlpha() {
        return (float) (playerAlpha.getValue() / 100.0);
    }
}
