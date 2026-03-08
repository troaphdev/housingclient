package com.housingclient.utils;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class PlayerUtils {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    public static boolean isMoving() {
        return mc.thePlayer != null && (mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0);
    }
    
    public static boolean isOnGround() {
        return mc.thePlayer != null && mc.thePlayer.onGround;
    }
    
    public static boolean isInLiquid() {
        if (mc.thePlayer == null) return false;
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }
    
    public static boolean isInAir() {
        return mc.thePlayer != null && !mc.thePlayer.onGround && !isInLiquid();
    }
    
    public static boolean isSuffocating() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        
        BlockPos headPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Block block = mc.theWorld.getBlockState(headPos).getBlock();
        
        return block.getMaterial().isSolid() && !block.getMaterial().isLiquid();
    }
    
    public static boolean isInsideBlock() {
        if (mc.thePlayer == null || mc.theWorld == null) return false;
        
        BlockPos playerPos = new BlockPos(mc.thePlayer);
        Block block = mc.theWorld.getBlockState(playerPos).getBlock();
        
        return block.getMaterial().isSolid();
    }
    
    public static boolean isFalling() {
        return mc.thePlayer != null && mc.thePlayer.motionY < -0.1 && !mc.thePlayer.onGround;
    }
    
    public static double getSpeed() {
        if (mc.thePlayer == null) return 0;
        return Math.sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ);
    }
    
    public static void setSpeed(double speed) {
        if (mc.thePlayer == null) return;
        
        double yaw = getDirection();
        mc.thePlayer.motionX = -Math.sin(yaw) * speed;
        mc.thePlayer.motionZ = Math.cos(yaw) * speed;
    }
    
    public static double getDirection() {
        if (mc.thePlayer == null) return 0;
        
        float yaw = mc.thePlayer.rotationYaw;
        
        if (mc.thePlayer.moveForward < 0) {
            yaw += 180;
        }
        
        float forward = 1;
        if (mc.thePlayer.moveForward < 0) {
            forward = -0.5f;
        } else if (mc.thePlayer.moveForward > 0) {
            forward = 0.5f;
        }
        
        if (mc.thePlayer.moveStrafing > 0) {
            yaw -= 90 * forward;
        }
        if (mc.thePlayer.moveStrafing < 0) {
            yaw += 90 * forward;
        }
        
        return Math.toRadians(yaw);
    }
    
    public static float[] getRotationsToEntity(Entity entity) {
        if (mc.thePlayer == null) return new float[]{0, 0};
        
        double x = entity.posX - mc.thePlayer.posX;
        double y = (entity.posY + entity.getEyeHeight()) - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double z = entity.posZ - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(y, dist) * 180 / Math.PI);
        
        return new float[]{yaw, pitch};
    }
    
    public static float[] getRotationsToPos(double x, double y, double z) {
        if (mc.thePlayer == null) return new float[]{0, 0};
        
        double diffX = x - mc.thePlayer.posX;
        double diffY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = z - mc.thePlayer.posZ;
        
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) (Math.atan2(diffZ, diffX) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(diffY, dist) * 180 / Math.PI);
        
        return new float[]{
                MathHelper.wrapAngleTo180_float(yaw),
                MathHelper.clamp_float(pitch, -90, 90)
        };
    }
    
    public static float[] smoothRotation(float[] current, float[] target, float speed) {
        float yawDiff = MathHelper.wrapAngleTo180_float(target[0] - current[0]);
        float pitchDiff = target[1] - current[1];
        
        float yawStep = Math.min(Math.abs(yawDiff), speed) * Math.signum(yawDiff);
        float pitchStep = Math.min(Math.abs(pitchDiff), speed) * Math.signum(pitchDiff);
        
        return new float[]{
                MathHelper.wrapAngleTo180_float(current[0] + yawStep),
                MathHelper.clamp_float(current[1] + pitchStep, -90, 90)
        };
    }
    
    public static double getDistanceToPos(double x, double y, double z) {
        if (mc.thePlayer == null) return 0;
        return mc.thePlayer.getDistance(x, y, z);
    }
    
    public static double getDistanceToEntity(Entity entity) {
        if (mc.thePlayer == null) return 0;
        return mc.thePlayer.getDistanceToEntity(entity);
    }
    
    public static BlockPos getBlockBelow() {
        if (mc.thePlayer == null) return null;
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
    }
    
    public static boolean canPlaceBlock(BlockPos pos) {
        if (mc.theWorld == null) return false;
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block.getMaterial() == Material.air || block.getMaterial().isReplaceable();
    }
    
    public static EnumFacing getPlaceSide(BlockPos pos) {
        if (mc.theWorld == null) return null;
        
        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighbor = pos.offset(facing);
            Block block = mc.theWorld.getBlockState(neighbor).getBlock();
            if (block != Blocks.air && !block.getMaterial().isReplaceable()) {
                return facing;
            }
        }
        return null;
    }
    
    public static int getHeldItemSlot() {
        if (mc.thePlayer == null) return -1;
        return mc.thePlayer.inventory.currentItem;
    }
    
    public static ItemStack getHeldItem() {
        if (mc.thePlayer == null) return null;
        return mc.thePlayer.getHeldItem();
    }
    
    public static void setHeldItemSlot(int slot) {
        if (mc.thePlayer != null && slot >= 0 && slot < 9) {
            mc.thePlayer.inventory.currentItem = slot;
        }
    }
}

