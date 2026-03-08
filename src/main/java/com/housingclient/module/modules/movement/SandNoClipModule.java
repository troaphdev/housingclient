package com.housingclient.module.modules.movement;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

public class SandNoClipModule extends Module {
    
    private final NumberSetting escapeSpeed = new NumberSetting("Escape Speed", "How fast to escape", 0.5, 0.1, 2.0, 0.1);
    private final BooleanSetting autoDisable = new BooleanSetting("Auto Disable", "Disable when free", true);
    private final BooleanSetting phaseUp = new BooleanSetting("Phase Up", "Phase upward to escape", true);
    private final BooleanSetting phaseDown = new BooleanSetting("Phase Down", "Phase downward if up blocked", false);
    
    private boolean isClipping = false;
    
    public SandNoClipModule() {
        super("Sand NoClip", "Phase through falling blocks when suffocating", Category.MOVEMENT, ModuleMode.BOTH);
        
        addSetting(escapeSpeed);
        addSetting(autoDisable);
        addSetting(phaseUp);
        addSetting(phaseDown);
    }
    
    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        boolean insideBlock = isInsideSolidBlock();
        
        if (insideBlock) {
            isClipping = true;
            
            // Disable collision
            mc.thePlayer.noClip = true;
            mc.thePlayer.onGround = false;
            mc.thePlayer.fallDistance = 0;
            
            // Move to escape
            if (phaseUp.isEnabled() && !isBlockAboveSolid()) {
                mc.thePlayer.motionY = escapeSpeed.getValue();
                mc.thePlayer.setPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY + escapeSpeed.getValue(),
                    mc.thePlayer.posZ
                );
            } else if (phaseDown.isEnabled() && !isBlockBelowSolid()) {
                mc.thePlayer.motionY = -escapeSpeed.getValue();
                mc.thePlayer.setPosition(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY - escapeSpeed.getValue(),
                    mc.thePlayer.posZ
                );
            }
            
            // Also try horizontal movement
            mc.thePlayer.motionX = 0;
            mc.thePlayer.motionZ = 0;
            
        } else if (isClipping) {
            // Re-enable collision
            mc.thePlayer.noClip = false;
            isClipping = false;
            
            if (autoDisable.isEnabled()) {
                setEnabled(false);
            }
        }
    }
    
    private boolean isInsideSolidBlock() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        
        // Check player's bounding box
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        
        // Check multiple positions within player
        for (double x = bb.minX; x <= bb.maxX; x += 0.5) {
            for (double y = bb.minY; y <= bb.maxY; y += 0.5) {
                for (double z = bb.minZ; z <= bb.maxZ; z += 0.5) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = mc.theWorld.getBlockState(pos).getBlock();
                    
                    if (isSuffocatingBlock(block)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private boolean isSuffocatingBlock(Block block) {
        // Check for falling blocks
        if (block instanceof BlockFalling) return true;
        
        // Check specific blocks (1.8.9 compatible)
        return block == Blocks.sand || 
               block == Blocks.gravel || 
               block == Blocks.anvil ||
               block == Blocks.dragon_egg;
    }
    
    private boolean isBlockAboveSolid() {
        BlockPos above = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2.5, mc.thePlayer.posZ);
        Block block = mc.theWorld.getBlockState(above).getBlock();
        return block.isFullBlock() && !isSuffocatingBlock(block);
    }
    
    private boolean isBlockBelowSolid() {
        BlockPos below = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 0.5, mc.thePlayer.posZ);
        Block block = mc.theWorld.getBlockState(below).getBlock();
        return block.isFullBlock() && !isSuffocatingBlock(block);
    }
    
    @Override
    protected void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.noClip = false;
        }
        isClipping = false;
    }
    
    @Override
    public String getDisplayInfo() {
        return isClipping ? "Clipping" : "Ready";
    }
}
