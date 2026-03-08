package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.RenderUtils;
import net.minecraft.tileentity.*;
import net.minecraft.util.BlockPos;

import java.awt.Color;

public class StorageESPModule extends Module {

    private final BooleanSetting chests = new BooleanSetting("Chests", "Show regular chests", true);
    private final BooleanSetting trappedChests = new BooleanSetting("Trapped Chests", "Show trapped chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", "Show ender chests", true);
    private final BooleanSetting hoppers = new BooleanSetting("Hoppers", "Show hoppers", false);
    private final BooleanSetting droppers = new BooleanSetting("Droppers", "Show droppers/dispensers", false);

    private final NumberSetting range = new NumberSetting("Range", "Render range", 64, 10, 128);
    private final NumberSetting lineWidth = new NumberSetting("Line Width", "ESP line width", 2.0, 0.5, 5.0, 0.5);
    private final BooleanSetting filled = new BooleanSetting("Filled", "Fill boxes with color", true);

    // Fixed colors
    private static final Color CHEST_COLOR = new Color(255, 165, 0); // Orange
    private static final Color TRAPPED_COLOR = new Color(255, 50, 50); // Red
    private static final Color ENDER_COLOR = new Color(200, 0, 255); // Purple
    private static final Color HOPPER_COLOR = new Color(100, 100, 100); // Gray

    public StorageESPModule() {
        super("StorageESP", "Highlight storage blocks", Category.VISUALS, ModuleMode.BOTH);

        addSetting(chests);
        addSetting(trappedChests);
        addSetting(enderChests);
        addSetting(hoppers);
        addSetting(droppers);
        addSetting(range);
        addSetting(lineWidth);
        addSetting(filled);
    }

    @Override
    public void onRender3D(float partialTicks) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        double maxRange = range.getValue();
        float baseWidth = lineWidth.getFloatValue();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            BlockPos pos = tileEntity.getPos();

            // Check range
            double distanceSq = mc.thePlayer.getDistanceSq(pos);
            if (distanceSq > maxRange * maxRange)
                continue;

            Color color = getColorForTileEntity(tileEntity);
            if (color == null)
                continue;

            // Scale line width based on distance (like ESPModule)
            float dist = (float) Math.sqrt(distanceSq);
            float scaledWidth = baseWidth;
            if (dist > 10) {
                scaledWidth = Math.max(0.1f, scaledWidth * (10f / dist));
            }

            // Render ESP
            if (filled.isEnabled()) {
                Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
                RenderUtils.drawBlockESPFilled(pos, fillColor, color, scaledWidth);
            } else {
                RenderUtils.drawBlockESP(pos, color, scaledWidth);
            }
        }
    }

    private Color getColorForTileEntity(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tileEntity;
            if (chest.getChestType() == 1) {
                // Trapped chest
                if (trappedChests.isEnabled()) {
                    return TRAPPED_COLOR;
                }
            } else {
                if (chests.isEnabled()) {
                    return CHEST_COLOR;
                }
            }
        } else if (tileEntity instanceof TileEntityEnderChest) {
            if (enderChests.isEnabled()) {
                return ENDER_COLOR;
            }
        } else if (tileEntity instanceof TileEntityHopper) {
            if (hoppers.isEnabled()) {
                return HOPPER_COLOR;
            }
        } else if (tileEntity instanceof TileEntityDispenser || tileEntity instanceof TileEntityDropper) {
            if (droppers.isEnabled()) {
                return HOPPER_COLOR;
            }
        }

        return null;
    }
}
