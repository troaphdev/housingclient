package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Coordinates Module - Shows your XYZ position
 */
public class CoordsModule extends Module {

    public CoordsModule() {
        super("Coords", "Shows your XYZ coordinates", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        ScaledResolution sr = new ScaledResolution(mc);
        String text = String.format("XYZ: %.0f, %.0f, %.0f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getCoordsX() : 5;
        int y = designer != null ? designer.getCoordsY(sr.getScaledHeight()) : sr.getScaledHeight() - 25;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.0f, %.0f, %.0f",
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }
}
