package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;

/**
 * CPS Module - Shows clicks per second
 */
public class CPSModule extends Module {

    public CPSModule() {
        super("CPS", "Shows your clicks per second", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int leftCPS = HousingClient.getInstance().getHud().getLeftCPS();
        int rightCPS = HousingClient.getInstance().getHud().getRightCPS();
        String text = "CPS: " + leftCPS + " | " + rightCPS;

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getCpsX() : 5;
        int y = designer != null ? designer.getCpsY() : 100;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

    @Override
    public String getDisplayInfo() {
        return null; // Don't show values in module list
    }
}
