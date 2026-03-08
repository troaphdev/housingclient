package com.housingclient.module.modules.building;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.item.Item;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * FastPlace Module - Universal fast right-click
 * 
 * Acts as a high-speed right-click autoclicker that works on EVERYTHING:
 * blocks, levers, noteblocks, buttons, doors, NPCs, entities, items, etc.
 * 
 * Uses rightClickMouse() reflection to simulate real full right-clicks
 * (no bad packets — identical to the player actually clicking).
 */
public class FastPlaceModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between clicks", 0, 0, 4);
    private final NumberSetting cps = new NumberSetting("Clicks/Tick", "Right-clicks per tick (at 0 delay)", 1, 1, 5);

    private Field rightClickDelayField;
    private Method rightClickMethod;
    private boolean delayFieldFound = false;
    private boolean methodFound = false;

    /** Track item changes to handle block-swap sync */
    private Item lastHeldItem = null;
    private int lastHeldMeta = -1;
    private int swapCooldown = 0;
    private int tickCounter = 0;

    public FastPlaceModule() {
        super("FastPlace", "Universal fast right-click", Category.MISCELLANEOUS, ModuleMode.BOTH);

        addSetting(delay);
        addSetting(cps);

        // Get the rightClickDelayTimer field via reflection
        try {
            try {
                rightClickDelayField = mc.getClass().getDeclaredField("rightClickDelayTimer");
                rightClickDelayField.setAccessible(true);
                delayFieldFound = true;
            } catch (NoSuchFieldException e) {
                try {
                    rightClickDelayField = mc.getClass().getDeclaredField("field_71467_ac");
                    rightClickDelayField.setAccessible(true);
                    delayFieldFound = true;
                } catch (NoSuchFieldException e2) {
                    // Fallback: search for the field
                    for (Field field : mc.getClass().getDeclaredFields()) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            try {
                                int val = field.getInt(mc);
                                if (val >= 0 && val <= 6) {
                                    rightClickDelayField = field;
                                    delayFieldFound = true;
                                    break;
                                }
                            } catch (Exception ex) {
                                // Continue
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed
        }

        // Get the rightClickMouse() method via reflection
        // This is the vanilla method that handles ALL right-click interactions
        try {
            try {
                rightClickMethod = mc.getClass().getDeclaredMethod("rightClickMouse");
                rightClickMethod.setAccessible(true);
                methodFound = true;
            } catch (NoSuchMethodException e) {
                try {
                    // SRG name for rightClickMouse
                    rightClickMethod = mc.getClass().getDeclaredMethod("func_147121_ag");
                    rightClickMethod.setAccessible(true);
                    methodFound = true;
                } catch (NoSuchMethodException e2) {
                    // Search for it
                    for (Method m : mc.getClass().getDeclaredMethods()) {
                        if (m.getParameterCount() == 0 && m.getReturnType() == void.class) {
                            // rightClickMouse is a private void with no args
                            // We can't easily distinguish it, so skip this fallback
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed
        }
    }

    @Override
    protected void onEnable() {
        lastHeldItem = null;
        lastHeldMeta = -1;
        swapCooldown = 0;
        tickCounter = 0;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

        // Decrement swap cooldown
        if (swapCooldown > 0) {
            swapCooldown--;
        }

        // Only when holding right click
        if (!Mouse.isButtonDown(1))
            return;

        // Detect item change (block-swap)
        Item currentItem = mc.thePlayer.getHeldItem() != null ? mc.thePlayer.getHeldItem().getItem() : null;
        int currentMeta = mc.thePlayer.getHeldItem() != null ? mc.thePlayer.getHeldItem().getMetadata() : -1;
        if (currentItem != lastHeldItem || currentMeta != lastHeldMeta) {
            lastHeldItem = currentItem;
            lastHeldMeta = currentMeta;
            swapCooldown = 1;
            // Reset delay timer so next tick fires immediately
            resetDelayTimer();
            return;
        }

        if (swapCooldown > 0) {
            return;
        }

        // Delay logic: only fire every N ticks
        int delayVal = delay.getIntValue();
        if (delayVal > 0) {
            tickCounter++;
            if (tickCounter < delayVal) {
                // Still waiting — but always reset the vanilla delay timer
                // so it doesn't add its own 4-tick delay on top
                resetDelayTimer();
                return;
            }
            tickCounter = 0;
        }

        // Always reset the vanilla right-click delay timer to 0
        // This removes the built-in 4-tick cooldown between block placements
        resetDelayTimer();

        // Fire additional right-clicks per tick if we have the method
        // The first click already happened via vanilla (since delay timer is 0),
        // so we fire (cps - 1) additional clicks
        if (methodFound && rightClickMethod != null) {
            int extraClicks = cps.getIntValue() - 1;
            for (int i = 0; i < extraClicks; i++) {
                try {
                    rightClickMethod.invoke(mc);
                } catch (Exception e) {
                    // Reflection failed — fall back to delay-only mode
                    break;
                }
            }
        }
    }

    private void resetDelayTimer() {
        if (delayFieldFound && rightClickDelayField != null) {
            try {
                rightClickDelayField.setInt(mc, 0);
            } catch (Exception e) {
                // Reflection failed
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        int d = delay.getIntValue();
        int c = cps.getIntValue();
        if (d == 0 && c > 1) {
            return c + "x";
        }
        return d + "t";
    }
}
