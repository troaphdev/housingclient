package com.housingclient.module.modules.movement;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class ScaffoldModule extends Module {

    private final BooleanSetting tower = new BooleanSetting("Tower", "Build upward while jumping", true);
    private final BooleanSetting telly = new BooleanSetting("Telly",
            "Constant jumping while scaffolding (natural movement)", false);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", "Switch to blocks automatically", true);
    private final BooleanSetting silentRotate = new BooleanSetting("Silent Rotate", "Server-side rotations only", true);
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", "Ticks between placements", 1, 0, 5);
    private final NumberSetting extend = new NumberSetting("Extend", "Extend placement distance", 0, 0, 3);

    private int prevSlot = -1;
    private int ticksSincePlaced = 0;
    private float serverYaw, serverPitch;

    public ScaffoldModule() {
        super("Scaffold", "Automatically place blocks under you", Category.MOVEMENT, ModuleMode.BOTH);

        addSetting(tower);
        addSetting(telly);
        addSetting(autoSwitch);
        addSetting(silentRotate);
        addSetting(placeDelay);
        addSetting(extend);
    }

    @Override
    protected void onEnable() {
        prevSlot = -1;
        ticksSincePlaced = 0;
        if (mc.thePlayer != null) {
            serverYaw = mc.thePlayer.rotationYaw;
            serverPitch = mc.thePlayer.rotationPitch;
        }
    }

    @Override
    protected void onDisable() {
        // Restore slot if we switched
        if (prevSlot != -1 && mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = prevSlot;
            prevSlot = -1;
        }
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

        ticksSincePlaced++;

        // Check place delay
        if (ticksSincePlaced < placeDelay.getIntValue())
            return;

        // Find block slot
        int blockSlot = findBlockSlot();
        if (blockSlot == -1)
            return;

        // Get the block position to place at
        BlockPos below = getBlockBelow();
        if (below == null)
            return;

        // Check if we need to place
        Block blockBelow = mc.theWorld.getBlockState(below).getBlock();
        if (blockBelow != Blocks.air)
            return;

        // Find a face to place against
        PlaceInfo placeInfo = findPlaceInfo(below);
        if (placeInfo == null)
            return;

        // Switch to block slot if needed
        if (autoSwitch.isEnabled() && mc.thePlayer.inventory.currentItem != blockSlot) {
            if (prevSlot == -1) {
                prevSlot = mc.thePlayer.inventory.currentItem;
            }
            mc.thePlayer.inventory.currentItem = blockSlot;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(blockSlot));
        }

        // Calculate rotations
        float[] rotations = getRotationsToBlock(placeInfo.pos, placeInfo.face);

        // Send server-side rotation packet (silent)
        if (silentRotate.isEnabled()) {
            mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(
                    rotations[0], rotations[1], mc.thePlayer.onGround));
        } else {
            // Client-side rotation
            mc.thePlayer.rotationYaw = rotations[0];
            mc.thePlayer.rotationPitch = rotations[1];
        }

        // Place the block
        Vec3 hitVec = new Vec3(
                placeInfo.pos.getX() + 0.5 + placeInfo.face.getFrontOffsetX() * 0.5,
                placeInfo.pos.getY() + 0.5 + placeInfo.face.getFrontOffsetY() * 0.5,
                placeInfo.pos.getZ() + 0.5 + placeInfo.face.getFrontOffsetZ() * 0.5);

        mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.getHeldItem(),
                placeInfo.pos,
                placeInfo.face,
                hitVec);

        mc.thePlayer.swingItem();
        ticksSincePlaced = 0;

        // Telly mode - constantly jump while moving forward for natural movement
        if (telly.isEnabled() && mc.thePlayer.onGround && mc.thePlayer.moveForward > 0) {
            mc.thePlayer.jump();
        }
        // Tower - move up while jumping
        else if (tower.isEnabled() && mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.onGround) {
            mc.thePlayer.motionY = 0.42;
        }
    }

    private BlockPos getBlockBelow() {
        double x = mc.thePlayer.posX;
        double z = mc.thePlayer.posZ;

        // Extend in movement direction
        if (extend.getIntValue() > 0) {
            float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
            double extendDist = extend.getValue();

            if (mc.thePlayer.moveForward > 0) {
                x -= Math.sin(yaw) * extendDist;
                z += Math.cos(yaw) * extendDist;
            } else if (mc.thePlayer.moveForward < 0) {
                x += Math.sin(yaw) * extendDist;
                z -= Math.cos(yaw) * extendDist;
            }
        }

        return new BlockPos(x, mc.thePlayer.posY - 1, z);
    }

    private PlaceInfo findPlaceInfo(BlockPos target) {
        // Check all faces around the target
        EnumFacing[] faces = { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST,
                EnumFacing.EAST };

        for (EnumFacing face : faces) {
            BlockPos neighbor = target.offset(face);
            Block block = mc.theWorld.getBlockState(neighbor).getBlock();

            if (block != Blocks.air && block.isFullCube()) {
                return new PlaceInfo(neighbor, face.getOpposite());
            }
        }

        return null;
    }

    private int findBlockSlot() {
        // First check current slot
        ItemStack current = mc.thePlayer.getHeldItem();
        if (current != null && current.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock) current.getItem()).getBlock();
            if (isValidBlock(block)) {
                return mc.thePlayer.inventory.currentItem;
            }
        }

        // Search hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                Block block = ((ItemBlock) stack.getItem()).getBlock();
                if (isValidBlock(block)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean isValidBlock(Block block) {
        return block != Blocks.tnt &&
                block != Blocks.chest &&
                block != Blocks.trapped_chest &&
                block != Blocks.sand &&
                block != Blocks.gravel &&
                block != Blocks.anvil &&
                block.isFullCube();
    }

    private float[] getRotationsToBlock(BlockPos pos, EnumFacing face) {
        double x = pos.getX() + 0.5 + face.getFrontOffsetX() * 0.5;
        double y = pos.getY() + 0.5 + face.getFrontOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + face.getFrontOffsetZ() * 0.5;

        double diffX = x - mc.thePlayer.posX;
        double diffY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double diffZ = z - mc.thePlayer.posZ;

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        return new float[] {
                MathHelper.wrapAngleTo180_float(yaw),
                MathHelper.clamp_float(pitch, -90, 90)
        };
    }

    private static class PlaceInfo {
        BlockPos pos;
        EnumFacing face;

        PlaceInfo(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }
    }

    @Override
    public String getDisplayInfo() {
        int blocks = countBlocks();
        return blocks > 0 ? String.valueOf(blocks) : null;
    }

    private int countBlocks() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock) {
                count += stack.stackSize;
            }
        }
        return count;
    }
}
