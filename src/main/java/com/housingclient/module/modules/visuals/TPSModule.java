package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * TPS Module
 * Displays the server's current TPS (ticks per second).
 * 
 * The vanilla Minecraft server sends S03PacketTimeUpdate exactly once
 * every 20 game ticks. At a perfect 20 TPS, this is every 1000ms.
 * If the server is lagging (e.g. running at 10 TPS), the interval
 * stretches to 2000ms. Formula: TPS = 20000 / interval_ms.
 * 
 * Uses a rolling average of the last 5 intervals (~5 seconds of data)
 * for a responsive but stable reading.
 */
public class TPSModule extends Module {

    private static final int SAMPLE_SIZE = 5;

    private final long[] intervals = new long[SAMPLE_SIZE];
    private int sampleIndex = 0;
    private int sampleCount = 0;
    private long lastPacketNanos = -1;
    private double currentTPS = 20.0;

    public TPSModule() {
        super("TPS", "Shows server TPS", Category.VISUALS, ModuleMode.BOTH);
    }

    /**
     * Called when S03PacketTimeUpdate is received (via mixin).
     * This is called regardless of whether the module is enabled,
     * so we always have fresh data when the user toggles it on.
     */
    public void onTimeUpdate() {
        long now = System.nanoTime();

        if (lastPacketNanos != -1) {
            long diffMs = (now - lastPacketNanos) / 1_000_000L;

            // Skip unrealistic intervals:
            // < 50ms = duplicate/spurious packet (impossible at any real TPS)
            // > 10000ms = server was frozen or we just reconnected
            if (diffMs >= 50 && diffMs <= 10000) {
                intervals[sampleIndex] = diffMs;
                sampleIndex = (sampleIndex + 1) % SAMPLE_SIZE;
                if (sampleCount < SAMPLE_SIZE) {
                    sampleCount++;
                }
                recalculate();
            }
            // If interval is invalid, just skip this sample but keep lastPacketNanos
            // so the next valid interval can be measured from "now"
        }

        lastPacketNanos = now;
    }

    private void recalculate() {
        if (sampleCount == 0) {
            currentTPS = 20.0;
            return;
        }

        long sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += intervals[i];
        }
        double avgMs = (double) sum / sampleCount;

        if (avgMs > 0) {
            // Server sends the packet every 20 ticks, so:
            // TPS = 20 ticks / (avgMs / 1000) seconds = 20000 / avgMs
            currentTPS = Math.min(20.0, 20000.0 / avgMs);
        }
    }

    @SubscribeEvent
    public void onWorldJoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        resetTPS();
    }

    @Override
    protected void onEnable() {
        // Don't reset data on enable — we want accumulated measurements
    }

    /**
     * Reset TPS data — called on server/world change
     */
    public void resetTPS() {
        lastPacketNanos = -1;
        currentTPS = 20.0;
        sampleIndex = 0;
        sampleCount = 0;
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            intervals[i] = 0;
        }
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getTpsX() : 5;
        int y = designer != null ? designer.getTpsY() : 25;

        // Format TPS with color coding
        String tpsColor;
        if (currentTPS >= 19.0) {
            tpsColor = "\u00A7a"; // Green - good
        } else if (currentTPS >= 17.0) {
            tpsColor = "\u00A7e"; // Yellow - ok
        } else if (currentTPS >= 12.0) {
            tpsColor = "\u00A76"; // Orange - degraded
        } else {
            tpsColor = "\u00A7c"; // Red - bad
        }

        String tpsText = String.format("TPS: %s%.1f", tpsColor, currentTPS);
        mc.fontRendererObj.drawStringWithShadow(tpsText, x, y, 0xFFFFFFFF);

        // Update width/height for HUD Designer
        setWidth(mc.fontRendererObj.getStringWidth(tpsText));
        setHeight(mc.fontRendererObj.FONT_HEIGHT);
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.1f", currentTPS);
    }

    public double getCurrentTPS() {
        return currentTPS;
    }
}
