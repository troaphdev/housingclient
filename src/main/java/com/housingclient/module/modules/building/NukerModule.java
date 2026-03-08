package com.housingclient.module.modules.building;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Nuker Module - Packet Throttle & Secure Mode
 * 
 * FIXES:
 * 1. "25% Registration" -> Caused by Packet Rate Limiting (10+ packets/tick).
 * - FIX: Swing ONLY ONCE per batch (reduced by 50%).
 * 
 * 2. Secure Mode:
 * - Validates server acceptance by limiting to 1 block/tick.
 * - Sends STOP_DESTROY_BLOCK for perfect transaction.
 */
public class NukerModule extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "What to break",
            "All", "All", "Flat");
    private final ModeSetting regMode = new ModeSetting("Registration", "Packet Logic",
            "Fast", "Fast", "Secure");

    private final NumberSetting range = new NumberSetting("Range", "Break radius", 4.5, 1.0, 6.0, 0.5);

    private final BooleanSetting showStats = new BooleanSetting("Chat Stats", "Show Efficiency % in chat", false);
    private final BooleanSetting disableOnKick = new BooleanSetting("Disable on Kick", "Auto-disable on disconnect",
            true);
    private final BooleanSetting onHold = new BooleanSetting("On Hold", "Active only while holding key", false);

    // Public state for PacketHandler
    private float targetYaw;
    private float targetPitch;
    private boolean isSpoofing;

    // Internal state for Tick Synchronization
    private List<BlockPos> tickTargets = new ArrayList<>();

    // Stats
    private final Queue<PendingBreak> pendingBreaks = new LinkedList<>();
    private int totalPacketsSent = 0;
    private int blocksBroken = 0;
    private long lastStatTime = 0;
    private int packetsSinceLast = 0;
    private int blocksSinceLast = 0;

    private static class PendingBreak {
        BlockPos pos;
        long timestamp;

        public PendingBreak(BlockPos pos) {
            this.pos = pos;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public NukerModule() {
        super("Nuker", "Breaks blocks around you", Category.MISCELLANEOUS, ModuleMode.BOTH);
        setBlatant(true);
        addSetting(mode);
        addSetting(regMode);
        addSetting(range);
        addSetting(showStats);
        addSetting(disableOnKick);
        addSetting(onHold);
    }

    // --- Getters for PacketHandler ---
    public boolean isSpoofing() {
        return false; // Silent mode removed (always off)
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (isEnabled() && disableOnKick.isEnabled()) {
            setEnabled(false);
            ChatUtils.sendClientMessage("\u00A7c[Nuker] Auto-disabled due to world change.");
        }
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null) {
            setEnabled(false);
            return;
        }
        if (!mc.thePlayer.capabilities.isCreativeMode) {
            ChatUtils.sendClientMessage("\u00A7c[Nuker] Creative mode required.");
            setEnabled(false);
        }

        isSpoofing = false;
        totalPacketsSent = 0;
        blocksBroken = 0;
        lastStatTime = System.currentTimeMillis();
        pendingBreaks.clear();
        tickTargets.clear();

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
        isSpoofing = false;
        tickTargets.clear();

        if (showStats.isEnabled()) {
            printStats(true);
        }
    }

    @Override
    public void onTick() {
        if (onHold.isEnabled() && isEnabled()) {
            if (!Keyboard.isKeyDown(getKeybind())) {
                toggle();
            }
        }
    }

    /**
     * Phase 1: Preparation (Pre-Packet)
     */
    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null)
            return;
        if (event.entity != mc.thePlayer)
            return;

        updateStats();

        isSpoofing = false;
        tickTargets.clear();

        List<BlockPos> targets = findBlocksToBreak();
        if (targets.isEmpty())
            return;

        // 1. Pick Primary Target
        BlockPos primary = targets.get(0);

        // 2. Calculate Rotation
        EnumFacing facing = getStrictFacing(primary);
        float[] rotations = calculateRotation(primary, facing);

        targetYaw = rotations[0];
        targetPitch = rotations[1];
        isSpoofing = true; // Signals PacketHandler to override Rotation

        // Visual Sync (Silent removed, always visual)
        mc.thePlayer.rotationYaw = targetYaw;
        mc.thePlayer.rotationPitch = targetPitch;

        // 3. Store targets for Phase 2
        this.tickTargets = targets;
    }

    /**
     * Phase 2: Execution (Post-Packet)
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || event.phase != TickEvent.Phase.END)
            return;
        if (event.player != mc.thePlayer)
            return;

        if (!isSpoofing || tickTargets.isEmpty())
            return;

        int limit = 5; // Hardcoded speed (was default) or perhaps user wanted max? "remove speed
                       // slider since it doesnt do anything"
                       // Kept at 5 as reasonable default since setting is gone.
        List<BlockPos> finalBreaks = new ArrayList<>();

        // 1. VALIDATION PHASE (Read-Only)
        for (BlockPos pos : tickTargets) {
            if (finalBreaks.size() >= limit)
                break;

            if (canReachWithRotation(pos, targetYaw, targetPitch)) {
                finalBreaks.add(pos);
            }
        }

        // 2. EXECUTION PHASE (Write)
        if (!finalBreaks.isEmpty()) {
            // Optimization: Swing only ONCE per tick.
            mc.thePlayer.swingItem();
        }

        boolean secure = regMode.getValue().equals("Secure");
        int executed = 0;

        for (BlockPos pos : finalBreaks) {
            // Secure mode limit: 1 block per tick
            if (secure && executed >= 1)
                break;

            breakBlock(pos, secure);
            executed++;
        }

        tickTargets.clear();
        isSpoofing = false;
    }

    private void breakBlock(BlockPos pos, boolean secure) {
        EnumFacing facing = getStrictFacing(pos);

        // Packet 1: Start Destroy (Creative Break)
        mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, facing));

        // Packet 2: Stop Destroy (Transaction Close)
        if (secure) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, facing));
        }

        // Prediction always on
        mc.theWorld.setBlockToAir(pos);

        totalPacketsSent++;
        packetsSinceLast++;
        pendingBreaks.add(new PendingBreak(pos));
    }

    private boolean canReachWithRotation(BlockPos pos, float yaw, float pitch) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 look = getVectorForRotation(pitch, yaw);
        double dist = range.getValue();
        Vec3 end = eyes.addVector(look.xCoord * dist, look.yCoord * dist, look.zCoord * dist);

        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(eyes, end, false, false, true);

        if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return result.getBlockPos().equals(pos);
        }
        return false;
    }

    // --- Stats ---

    private void updateStats() {
        long now = System.currentTimeMillis();

        Iterator<PendingBreak> it = pendingBreaks.iterator();
        while (it.hasNext()) {
            PendingBreak pending = it.next();
            if (mc.theWorld.isAirBlock(pending.pos)) {
                blocksBroken++;
                blocksSinceLast++;
                it.remove();
            } else if (now - pending.timestamp > 1000) {
                it.remove();
            }
        }

        if (showStats.isEnabled() && now - lastStatTime > 1000) {
            printStats(false);
            lastStatTime = now;
            packetsSinceLast = 0;
            blocksSinceLast = 0;
        }
    }

    private void printStats(boolean finalReport) {
        if (totalPacketsSent == 0 && packetsSinceLast == 0)
            return;

        double efficiency = 0.0;
        double bps = 0.0;

        if (finalReport) {
            if (totalPacketsSent > 0)
                efficiency = (double) blocksBroken / totalPacketsSent * 100.0;
            ChatUtils.sendClientMessage(String.format("\u00A7e[Nuker] Final: %d/%d",
                    blocksBroken, totalPacketsSent));
        } else {
            if (packetsSinceLast > 0) {
                bps = (double) blocksSinceLast;
                ChatUtils.sendClientMessage(String.format("\u00A7a[Nuker] Speed: %.1f BPS | Batch: %d",
                        bps, 5));
            }
        }
    }

    // --- Helpers ---

    private Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    private List<BlockPos> findBlocksToBreak() {
        List<BlockPos> blocks = new ArrayList<>();
        int r = (int) Math.ceil(range.getValue());
        BlockPos playerPos = mc.thePlayer.getPosition();
        String currentMode = mode.getValue();

        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                for (int y = r; y >= -r; y--) {
                    BlockPos pos = playerPos.add(x, y, z);

                    if (!isValidBlock(pos))
                        continue;

                    double dist = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (dist > range.getValue())
                        continue;

                    switch (currentMode) {
                        case "Flat":
                            if (pos.getY() < Math.floor(mc.thePlayer.posY))
                                continue;
                            break;
                    }
                    blocks.add(pos);
                }
            }
        }
        blocks.sort((a, b) -> Double.compare(mc.thePlayer.getDistanceSq(a), mc.thePlayer.getDistanceSq(b)));
        return blocks;
    }

    private boolean isValidBlock(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        return block != Blocks.air && !block.getMaterial().isLiquid() && block != Blocks.fire;
    }

    private float[] calculateRotation(BlockPos pos, EnumFacing facing) {
        double x = pos.getX() + 0.5 + facing.getFrontOffsetX() * 0.5;
        double y = pos.getY() + 0.5 + facing.getFrontOffsetY() * 0.5;
        double z = pos.getZ() + 0.5 + facing.getFrontOffsetZ() * 0.5;

        if (facing.getAxis() == EnumFacing.Axis.X)
            x -= facing.getFrontOffsetX() * 0.1;
        if (facing.getAxis() == EnumFacing.Axis.Y)
            y -= facing.getFrontOffsetY() * 0.1;
        if (facing.getAxis() == EnumFacing.Axis.Z)
            z -= facing.getFrontOffsetZ() * 0.1;

        double dX = x - mc.thePlayer.posX;
        double dY = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dZ = z - mc.thePlayer.posZ;

        double dist = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float) (Math.atan2(dZ, dX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) -(Math.atan2(dY, dist) * 180.0 / Math.PI);

        return new float[] { yaw, MathHelper.clamp_float(pitch, -90.0f, 90.0f) };
    }

    private EnumFacing getStrictFacing(BlockPos pos) {
        Vec3 eyesPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 blockCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 diff = eyesPos.subtract(blockCenter);

        double absX = Math.abs(diff.xCoord);
        double absY = Math.abs(diff.yCoord);
        double absZ = Math.abs(diff.zCoord);

        if (absY >= absX && absY >= absZ)
            return diff.yCoord > 0 ? EnumFacing.UP : EnumFacing.DOWN;
        else if (absX >= absZ)
            return diff.xCoord > 0 ? EnumFacing.EAST : EnumFacing.WEST;
        else
            return diff.zCoord > 0 ? EnumFacing.SOUTH : EnumFacing.NORTH;
    }

    @Override
    public String getDisplayInfo() {
        return null;
    }
}
