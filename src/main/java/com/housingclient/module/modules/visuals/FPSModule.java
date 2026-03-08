package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import net.minecraft.client.Minecraft;

/**
 * FPS Module - Shows frames per second
 */
public class FPSModule extends Module {

    public FPSModule() {
        super("FPS", "Shows your frames per second", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int fps = Minecraft.getDebugFPS();
        String text = "FPS: " + fps;

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getFpsX() : 5;
        int y = designer != null ? designer.getFpsY() : 88;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(Minecraft.getDebugFPS());
    }
}
