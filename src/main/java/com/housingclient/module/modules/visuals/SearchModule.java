package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ColorSetting;
import com.housingclient.module.settings.ItemSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Search Module
 * 
 * Highlights specific blocks in the world using an item picker.
 * Users select a block from the item picker GUI (same as Bypass Blacklist).
 */
public class SearchModule extends Module {

    private final ItemSetting blockToSearch = new ItemSetting("Block", "Block to search for", true, false);
    private final NumberSetting range = new NumberSetting("Range", "Search range in blocks", 32, 8, 100);
    private final BooleanSetting filled = new BooleanSetting("Filled", "Render filled boxes", true);
    private final BooleanSetting outline = new BooleanSetting("Outline", "Render outlines", true);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", "Draw lines to blocks", false);
    private final ColorSetting highlightColor = new ColorSetting("Color", "Highlight color",
            new Color(255, 200, 0, 150));
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "Width of outline", 2.0, 0.5, 5.0, 0.5);

    // Use CopyOnWriteArraySet for thread-safe iteration without flicker
    private final Set<BlockPos> foundBlocks = new CopyOnWriteArraySet<>();
    private int scanTick = 0;
    private int lastBlockId = -1;
    private int lastMeta = -1;

    public SearchModule() {
        super("Search", "Highlight blocks in the world", Category.VISUALS, ModuleMode.BOTH);

        addSetting(blockToSearch);
        addSetting(range);
        addSetting(filled);
        addSetting(outline);
        addSetting(tracers);
        addSetting(highlightColor);
        addSetting(lineWidth);
    }

    @Override
    protected void onEnable() {
        foundBlocks.clear();
        lastBlockId = -1;
        lastMeta = -1;
    }

    @Override
    protected void onDisable() {
        foundBlocks.clear();
    }

    /**
     * Get the target block ID and metadata from the ItemSetting.
     * Returns null if no valid block is selected.
     */
    private int[] getTargetBlockInfo() {
        ItemStack stack = blockToSearch.getValue();
        if (stack == null || stack.getItem() == null)
            return null;

        if (!(stack.getItem() instanceof ItemBlock))
            return null;

        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null)
            return null;

        int id = Block.getIdFromBlock(block);
        int meta = stack.getMetadata();
        return new int[] { id, meta };
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        int[] info = getTargetBlockInfo();
        if (info == null) {
            if (!foundBlocks.isEmpty()) {
                foundBlocks.clear();
            }
            return;
        }

        int currentBlockId = info[0];
        int currentMeta = info[1];

        // Check if settings changed - requires full rescan
        if (currentBlockId != lastBlockId || currentMeta != lastMeta) {
            lastBlockId = currentBlockId;
            lastMeta = currentMeta;
            foundBlocks.clear();
            scanSlice = 0; // Reset incremental scan
        }

        // Incremental scanning: scan 1/10th of the area each tick (10 ticks = full
        // scan)
        scanTick++;
        if (scanTick >= 2) { // Every 2 ticks, scan a slice
            scanTick = 0;
            scanSliceIncremental();
            scanSlice++;
            if (scanSlice >= 10) {
                scanSlice = 0; // Loop back
            }
        }
    }

    private int scanSlice = 0;

    /**
     * Scan 1/10th of the search area (one X-slice) to spread load across ticks
     */
    private void scanSliceIncremental() {
        int[] info = getTargetBlockInfo();
        if (info == null)
            return;

        int targetId = info[0];
        int targetMeta = info[1];

        if (targetId == 0)
            return; // Skip air

        Block targetBlock = Block.getBlockById(targetId);
        if (targetBlock == null)
            return;

        int r = range.getIntValue();
        BlockPos playerPos = mc.thePlayer.getPosition();

        // Calculate which X slice to scan this tick (0-9)
        int sliceWidth = (2 * r + 1) / 10;
        if (sliceWidth < 1)
            sliceWidth = 1;

        int startX = -r + (scanSlice * sliceWidth);
        int endX = startX + sliceWidth;
        if (scanSlice == 9)
            endX = r + 1; // Last slice gets remainder

        // Only cleanup out-of-range blocks once per full cycle (at slice 0)
        // This prevents flicker by not constantly removing and re-adding
        if (scanSlice == 0) {
            BlockPos pPos = playerPos;
            int rr = r;
            foundBlocks.removeIf(pos -> {
                double dist = pos.distanceSq(pPos);
                return dist > rr * rr * 1.5; // Remove blocks that are way out of range
            });
        }

        for (int x = startX; x < endX; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = -r / 2; y <= r / 2; y++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
                    Block block = state.getBlock();

                    if (Block.getIdFromBlock(block) == targetId) {
                        if (block.getMetaFromState(state) == targetMeta) {
                            foundBlocks.add(pos); // Set handles duplicates automatically
                        }
                    } else {
                        // Remove block if it no longer matches (block was broken)
                        foundBlocks.remove(pos);
                    }
                }
            }
        }
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (foundBlocks.isEmpty())
            return;

        Color color = highlightColor.getValue();
        Color outlineColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / 2);

        float baseLineWidth = lineWidth.getFloatValue();

        // Get player interpolated position for accurate distance calculation
        double playerX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks;
        double playerY = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks;
        double playerZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks;

        // Limit rendering to max 500 blocks to prevent FPS drops
        int renderCount = 0;
        int maxRender = 500;

        for (BlockPos pos : foundBlocks) {
            if (renderCount >= maxRender)
                break;

            // Calculate distance for line width scaling
            double dx = pos.getX() + 0.5 - playerX;
            double dy = pos.getY() + 0.5 - playerY;
            double dz = pos.getZ() + 0.5 - playerZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            // Don't render blocks too far away for performance
            if (distance > range.getIntValue())
                continue;

            // Scale line width based on distance (thinner when far, consistent visual
            // thickness)
            float scaledLineWidth = (float) Math.max(0.5, baseLineWidth * (8.0 / Math.max(distance, 1.0)));
            // Cap at reasonable max
            scaledLineWidth = Math.min(scaledLineWidth, baseLineWidth * 4);

            if (filled.isEnabled()) {
                RenderUtils.drawBlockESPFilled(pos, fillColor, outlineColor, scaledLineWidth);
            } else if (outline.isEnabled()) {
                RenderUtils.drawBlockESP(pos, outlineColor, scaledLineWidth);
            }

            if (tracers.isEnabled()) {
                double x = pos.getX() + 0.5 - mc.getRenderManager().viewerPosX;
                double y = pos.getY() + 0.5 - mc.getRenderManager().viewerPosY;
                double z = pos.getZ() + 0.5 - mc.getRenderManager().viewerPosZ;
                RenderUtils.drawTracer(x, y, z, outlineColor, scaledLineWidth);
            }

            renderCount++;
        }
    }

    @Override
    public String getDisplayInfo() {
        int[] info = getTargetBlockInfo();
        if (info == null)
            return "None";

        Block block = Block.getBlockById(info[0]);
        String blockName = block != null ? block.getLocalizedName() : "Unknown";
        return foundBlocks.size() + " (" + blockName + ")";
    }
}
