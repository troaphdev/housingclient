package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.BooleanSetting;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Coordinates Module - Shows your XYZ position
 */
public class CoordsModule extends Module {

    private final BooleanSetting simplified = new BooleanSetting("Simplified Coordinates",
            "Round coordinates to whole numbers", false);

    public CoordsModule() {
        super("Coords", "Shows your XYZ coordinates", Category.VISUALS, ModuleMode.BOTH);
        addSetting(simplified);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        ScaledResolution sr = new ScaledResolution(mc);
        String text = formatCoords();

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getCoordsX() : 5;
        int y = designer != null ? designer.getCoordsY(sr.getScaledHeight()) : sr.getScaledHeight() - 25;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

    private String formatCoords() {
        if (simplified.isEnabled()) {
            return String.format("XYZ: %.0f, %.0f, %.0f",
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }
        // Match F3-style precision (thousandths)
        return String.format("XYZ: %.3f / %.3f / %.3f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    @Override
    public String getDisplayInfo() {
        if (mc.thePlayer == null) {
            return "";
        }
        if (simplified.isEnabled()) {
            return String.format("%.0f, %.0f, %.0f",
                    mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        }
        return String.format("%.3f / %.3f / %.3f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }
}
