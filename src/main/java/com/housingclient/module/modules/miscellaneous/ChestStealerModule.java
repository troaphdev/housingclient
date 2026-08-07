package com.housingclient.module.modules.miscellaneous;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.Random;

/**
 * Chest Stealer Module
 * 
 * Automatically takes items from chests with configurable delay.
 * Anti-cheat safe: uses shift-click with randomized human-like delay.
 */
public class ChestStealerModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between steals", 3.0, 1.0, 10.0, 1.0);
    private final NumberSetting startDelay = new NumberSetting("Start Delay", "Ticks before starting", 5.0, 0.0, 20.0, 1.0);

    private int tickCounter = 0;
    private int startCounter = 0;
    private boolean started = false;
    private int nextDelay = 0;
    private final Random random = new Random();

    public ChestStealerModule() {
        super("Chest Stealer", "Automatically takes items from chests", Category.MISCELLANEOUS, ModuleMode.BOTH);

        addSetting(delay);
        addSetting(startDelay);
    }

    @Override
    protected void onEnable() {
        tickCounter = 0;
        startCounter = 0;
        started = false;
        calculateNextDelay();
    }

    private void calculateNextDelay() {
        int baseDelay = Math.max(1, delay.getIntValue());
        
        // Add random jitter (-1 to +2 ticks) to make it human-like
        int jitter = random.nextInt(4) - 1; 
        
        // 10% chance to pause briefly to simulate reading/moving eyes
        if (random.nextFloat() < 0.10f) {
            jitter += random.nextInt(6) + 4; // Add 4-9 extra ticks of delay
        }

        nextDelay = Math.max(1, baseDelay + jitter);
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        // Only operate when a chest GUI is open
        if (!(mc.currentScreen instanceof GuiChest)) {
            started = false;
            startCounter = 0;
            return;
        }

        GuiChest gui = (GuiChest) mc.currentScreen;
        ContainerChest container = (ContainerChest) gui.inventorySlots;

        // Start delay
        if (!started) {
            startCounter++;
            if (startCounter < startDelay.getIntValue()) {
                return;
            }
            started = true;
            tickCounter = 0;
            calculateNextDelay();
        }

        // Delay between steals using randomized nextDelay
        tickCounter++;
        if (tickCounter < nextDelay) {
            return;
        }
        tickCounter = 0;
        calculateNextDelay();

        // Get the chest inventory size (upper inventory = chest slots)
        int chestSize = container.getLowerChestInventory().getSizeInventory();

        // Find next item in chest
        for (int i = 0; i < chestSize; i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.getHasStack()) {
                ItemStack stack = slot.getStack();
                if (stack != null) {
                    // Shift-click to move to player inventory
                    mc.playerController.windowClick(
                            container.windowId,
                            i,
                            0,
                            1, // 1 = shift-click
                            mc.thePlayer
                    );
                    return; // One item per cycle
                }
            }
        }

        // Chest is empty — nothing left to steal
    }

    @Override
    public String getDisplayInfo() {
        return delay.getIntValue() + "t (Human)";
    }
}
