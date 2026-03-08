package com.housingclient.module.modules.qol;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;

/**
 * Anti Void Lag Module
 * 
 * Prevents client-side lag when void holes are created or filled.
 * This happens because Minecraft recalculates skylight for the entire column.
 * 
 * Works via MixinWorld which intercepts checkLightFor and calls
 * shouldSkipLightUpdate.
 * 
 * Features:
 * 1. Skip all skylight updates for void-level blocks (Y < 5) - done in mixin
 * 2. Limit additional skylight updates per tick to prevent spikes
 */
public class AntiVoidLagModule extends Module {

    private final NumberSetting maxUpdatesPerTick = new NumberSetting("Max Updates/Tick",
            "Maximum skylight updates per tick (lower = less lag)", 100, 10, 500, 10);
    private final BooleanSetting showStats = new BooleanSetting("Show Stats",
            "Display blocked updates in module info", true);

    // Counter for limiting updates per tick
    private int updatesThisTick = 0;
    private int blockedThisTick = 0;
    private long lastTickTime = 0;

    public AntiVoidLagModule() {
        super("Anti Void Lag", "Prevents lag when void holes are filled/broken", Category.QOL, ModuleMode.BOTH);
        addSetting(maxUpdatesPerTick);
        addSetting(showStats);
    }

    @Override
    public void onTick() {
        // Reset counters each game tick
        long now = System.currentTimeMillis();
        if (now - lastTickTime > 50) { // ~20 TPS = 50ms per tick
            updatesThisTick = 0;
            blockedThisTick = 0;
            lastTickTime = now;
        }
    }

    /**
     * Called by MixinWorld to check if a light update should be skipped.
     * This is the core optimization - by limiting updates per tick we prevent
     * the massive lag spike when void holes are filled.
     * 
     * Note: The mixin also skips ALL updates for Y < 5 before calling this.
     * 
     * @return true if the light update should be skipped
     */
    public boolean shouldSkipLightUpdate() {
        if (!isEnabled()) {
            return false;
        }

        updatesThisTick++;

        // Skip if we've exceeded the limit this tick
        int maxUpdates = maxUpdatesPerTick.getIntValue();
        if (updatesThisTick > maxUpdates) {
            blockedThisTick++;
            return true;
        }

        return false;
    }

    @Override
    public String getDisplayInfo() {
        if (showStats.isEnabled() && blockedThisTick > 0) {
            return "Blocked " + blockedThisTick;
        }
        return null;
    }
}
