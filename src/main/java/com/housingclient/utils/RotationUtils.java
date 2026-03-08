package com.housingclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class RotationUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public static float[] getRotations(BlockPos pos) {
        return getRotations(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    public static float[] getRotations(double x, double y, double z) {
        double diffX = x - mc.thePlayer.posX;
        double diffY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = z - mc.thePlayer.posZ;
        double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);

        float yaw = (float) (Math.atan2(diffZ, diffX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) (-(Math.atan2(diffY, dist) * 180.0D / Math.PI));

        return new float[] {
                mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
                mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)
        };
    }

    public static void faceBlock(BlockPos pos) {
        float[] rotations = getRotations(pos);
        mc.thePlayer.rotationYaw = rotations[0];
        mc.thePlayer.rotationPitch = rotations[1];
    }
}
