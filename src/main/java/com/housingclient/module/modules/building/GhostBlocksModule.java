package com.housingclient.module.modules.building;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ItemSetting;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.util.HashSet;
import java.util.Set;

/**
 * Ghost Blocks Module
 * 
 * Enable the module, then use Right Click to place ghost blocks.
 * Left Click (punch) ghost blocks to remove them.
 * Client-Side Break: Allows removing ANY block client-side.
 */
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;

public class GhostBlocksModule extends Module {

    private final BooleanSetting placeBlocks = new BooleanSetting("Place Blocks", "Right-click to place ghost blocks",
            true);
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks",
            "Left-click to remove any block client-side", false);
    private final BooleanSetting restoreOnMiddleClick = new BooleanSetting("Middle Click Restore",
            "Middle-click to restore original block state", true);

    // Block picker setting (blocks only)
    private final ItemSetting blockSetting = new ItemSetting("Ghost Block", "Block to place as ghost", true, false);

    // Default to glass if nothing selected
    private Block getSelectedBlock() {
        ItemStack stack = blockSetting.getValue();
        if (stack != null) {
            Block b = Block.getBlockFromItem(stack.getItem());
            if (b != null && b != Blocks.air)
                return b;
        }
        return Blocks.glass;
    }

    private int getSelectedMeta() {
        ItemStack stack = blockSetting.getValue();
        return stack != null ? stack.getMetadata() : 0;
    }

    // State
    private final Set<BlockPos> ghostBlocks = new HashSet<>();
    // Use Map to store original state of broken blocks
    private final java.util.Map<BlockPos, net.minecraft.block.state.IBlockState> brokenBlocks = new java.util.HashMap<>();

    private boolean wasRightClicking = false;
    private boolean wasLeftClicking = false;



    public GhostBlocksModule() {
        super("Ghost Blocks", "Creates ghost blocks", Category.MISCELLANEOUS, ModuleMode.BOTH);
        addSetting(placeBlocks);
        addSetting(breakBlocks);
        addSetting(restoreOnMiddleClick);
        addSetting(blockSetting);
    }

    @Override
    protected void onEnable() {
        ghostBlocks.clear();
        brokenBlocks.clear();

        // Register packet listener
        if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
            mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler",
                    "ghost_blocks_packet_listener", new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof S23PacketBlockChange) {
                                S23PacketBlockChange packet = (S23PacketBlockChange) msg;
                                if (ghostBlocks.contains(packet.getBlockPosition())) {
                                    return; // Ignore server block updates for our ghost blocks
                                }
                            } else if (msg instanceof S22PacketMultiBlockChange) {
                                S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) msg;
                                for (net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData data : packet
                                        .getChangedBlocks()) {
                                    if (ghostBlocks.contains(data.getPos())) {
                                        return; // Ignore packet if it touches any of our blocks
                                    }
                                }
                            }
                            super.channelRead(ctx, msg);
                        }
                    });
        }

        // Register Event Bus
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    protected void onDisable() {
        // Remove ghost blocks
        if (mc.theWorld != null) {
            for (BlockPos pos : ghostBlocks) {
                // Determine what was there before? Hard to know.
                // Best effort: set to air, or let server update fix it eventually.
                // Actually, just setting to air is safer than leaving a fake block.
                if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                    mc.theWorld.setBlockToAir(pos);
                }
            }

            // Restore broken blocks
            for (java.util.Map.Entry<BlockPos, net.minecraft.block.state.IBlockState> entry : brokenBlocks.entrySet()) {
                mc.theWorld.setBlockState(entry.getKey(), entry.getValue());
            }
        }
        ghostBlocks.clear();
        brokenBlocks.clear();

        // Remove listener
        try {
            if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null) {
                mc.getNetHandler().getNetworkManager().channel().pipeline().remove("ghost_blocks_packet_listener");
            }
        } catch (Exception e) {
        }

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public void onTick() {
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // Re-inject listener if dropped (world change)
        if (mc.getNetHandler() != null && mc.getNetHandler().getNetworkManager() != null &&
                mc.getNetHandler().getNetworkManager().channel().pipeline()
                        .get("ghost_blocks_packet_listener") == null) {
            mc.getNetHandler().getNetworkManager().channel().pipeline().addBefore("packet_handler",
                    "ghost_blocks_packet_listener", new ChannelDuplexHandler() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            if (msg instanceof S23PacketBlockChange) {
                                S23PacketBlockChange packet = (S23PacketBlockChange) msg;
                                if (ghostBlocks.contains(packet.getBlockPosition())) {
                                    return;
                                }
                            } else if (msg instanceof S22PacketMultiBlockChange) {
                                S22PacketMultiBlockChange packet = (S22PacketMultiBlockChange) msg;
                                for (net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData data : packet
                                        .getChangedBlocks()) {
                                    if (ghostBlocks.contains(data.getPos())) {
                                        return;
                                    }
                                }
                            }
                            super.channelRead(ctx, msg);
                        }
                    });
        }

        Block targetBlock = getSelectedBlock();
        int targetMeta = getSelectedMeta();

        // Persistence Check: Re-apply ghost blocks if they were removed (by server or
        // lag)
        for (BlockPos pos : ghostBlocks) {
            net.minecraft.block.state.IBlockState state = mc.theWorld.getBlockState(pos);
            if (state.getBlock() != targetBlock || targetBlock.getMetaFromState(state) != targetMeta) {
                mc.theWorld.setBlockState(pos, targetBlock.getStateFromMeta(targetMeta));
            }
        }

        // Persistence Check: Ensure Broken Blocks stay Air (unless replaced by Ghost
        // Block)
        for (BlockPos pos : brokenBlocks.keySet()) {
            // If we placed a ghost block there, don't force it to air!
            if (ghostBlocks.contains(pos))
                continue;

            if (mc.theWorld.getBlockState(pos).getBlock() != Blocks.air) {
                mc.theWorld.setBlockToAir(pos);
            }
        }
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onMouse(net.minecraftforge.client.event.MouseEvent event) {
        if (!isEnabled() || mc.currentScreen != null)
            return;

        // Middle Click: Restore Block
        if (event.button == 2 && event.buttonstate) {
            if (restoreOnMiddleClick.isEnabled()) {
                handleMiddleClick();
            }
        }

        // Right Click: Place Ghost Block
        if (event.button == 1 && event.buttonstate) {
            if (placeBlocks.isEnabled()) {
                Block targetBlock = getSelectedBlock();

                boolean placed = placeGhostBlock(targetBlock, getSelectedMeta());
                if (placed) {
                    event.setCanceled(true); // Stop vanilla from processing this click (stops C08 packet)
                }
            }
        }

        // Left Click: Remove Ghost Block OR Break Real Block
        if (event.button == 0 && event.buttonstate) {
            handleLeftClick();
        }
    }

    private void handleMiddleClick() {
        // Standard Raytrace
        MovingObjectPosition mop = mc.objectMouseOver;

        // Try to hit valid block first
        if (mop != null && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();
            if (tryRestoreBlock(hitPos))
                return;
        }

        // Custom Raytrace for Air/Broken Blocks
        // We need to raytrace ourselves because standard raytrace ignores air,
        // and our broken blocks are air.
        double reach = 5.0;
        net.minecraft.util.Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        net.minecraft.util.Vec3 lookVec = mc.thePlayer.getLook(1.0f);
        net.minecraft.util.Vec3 endPos = eyePos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach,
                lookVec.zCoord * reach);

        // Simple DDA or stepping for finding a block in our list
        // Since we only care about blocks strictly in brokenBlocks or ghostBlocks,
        // we can check blocks along the ray.

        // Step size 0.1
        net.minecraft.util.Vec3 cur = eyePos;
        net.minecraft.util.Vec3 step = lookVec.normalize();
        step = new net.minecraft.util.Vec3(step.xCoord * 0.1, step.yCoord * 0.1, step.zCoord * 0.1);

        for (int i = 0; i < reach * 10; i++) {
            cur = cur.add(step);
            BlockPos pos = new BlockPos(cur.xCoord, cur.yCoord, cur.zCoord);

            // If we hit a solid block (that wasn't caught by standard MOP??), stop.
            // Actually, just check if this pos is in our lists.
            if (tryRestoreBlock(pos))
                return;
        }
    }

    private boolean tryRestoreBlock(BlockPos pos) {
        // 1. Ghost Block? Remove it.
        if (ghostBlocks.contains(pos)) {
            mc.theWorld.setBlockToAir(pos);
            ghostBlocks.remove(pos);
            return true;
        }

        // 2. Broken Block? Restore original state.
        if (brokenBlocks.containsKey(pos)) {
            net.minecraft.block.state.IBlockState original = brokenBlocks.get(pos);
            mc.theWorld.setBlockState(pos, original);
            brokenBlocks.remove(pos);
            return true;
        }
        return false;
    }

    private boolean placeGhostBlock(Block targetBlock, int meta) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null)
            return false;

        BlockPos placePos = null;

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();
            EnumFacing side = mop.sideHit;
            placePos = hitPos.offset(side);
        } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            // Allow placing in air
            double reach = 4.0;
            double x = mc.thePlayer.posX + mc.thePlayer.getLookVec().xCoord * reach;
            double y = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() + mc.thePlayer.getLookVec().yCoord * reach;
            double z = mc.thePlayer.posZ + mc.thePlayer.getLookVec().zCoord * reach;
            placePos = new BlockPos(x, y, z);
        }

        if (placePos != null) {
            Block block = mc.theWorld.getBlockState(placePos).getBlock();
            // Only place if currently air (don't overwrite real blocks, unless it's a
            // previously broken one)
            // Or if it's already a ghost block (refreshing it)
            if ((block == Blocks.air || brokenBlocks.containsKey(placePos)) && !ghostBlocks.contains(placePos)) {
                if (brokenBlocks.containsKey(placePos)) {
                    // Restoring a broken spot with a ghost block.
                    // Technically we can leave it in brokenBlocks (as we want to suppress the real
                    // block),
                    // but visual is now Ghost.
                    // Let's just place. The onTick broken check ensures real block is gone.
                    // onTick ghost check ensures ghost block is there.
                    // Conflict?
                    // onTick Broken: "If not air, set to air".
                    // onTick Ghost: "If not ghost, set to ghost".
                    // Ghost runs first in previous code... let's see current order.
                    // Ghost runs first.
                    // If we place a ghost block on a broken block:
                    // Tick 1: Ghost setBlock(Ghost).
                    // Tick 1: Broken check: Ghost is not Air -> Set to Air!
                    // Conflict!
                    // Fix: Broken check should ignore if ghost block is there.
                }

                // Place Client-Side Ghost Block with Metadata
                mc.theWorld.setBlockState(placePos, targetBlock.getStateFromMeta(meta));
                ghostBlocks.add(placePos);
                return true;
            }
        }
        return false;
    }

    private void handleLeftClick() {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null)
            return;

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos hitPos = mop.getBlockPos();

            // Case 1: Removing our own ghost block
            if (ghostBlocks.contains(hitPos)) {
                mc.theWorld.setBlockToAir(hitPos);
                ghostBlocks.remove(hitPos);
                return;
            }

            // Case 2: Client-side Break Feature
            if (breakBlocks.isEnabled()) {
                // Store original state before breaking
                if (!brokenBlocks.containsKey(hitPos)) {
                    brokenBlocks.put(hitPos, mc.theWorld.getBlockState(hitPos));
                }

                mc.theWorld.setBlockToAir(hitPos);
                // We don't track these in ghostBlocks because we don't need to persist them
                // effectively
                // (or maybe we should? The user just wants to break them. Server might revert
                // them though.)
                // If the user wants to break them to walk through, we should probably just set
                // to air.
                // Packet interceptor won't stop server from sending block updates for these
                // unless we add them to a "ignored" list.
                // The prompt says "break blocks client-sidedly". Usually meant for getting
                // through walls.
                // If server sends update, it will reappear.
                // Let's just set to air for now. If user complains about flickering, we can add
                // to ignored list.
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        int count = ghostBlocks.size();
        return count > 0 ? String.valueOf(count) : null;
    }
}
