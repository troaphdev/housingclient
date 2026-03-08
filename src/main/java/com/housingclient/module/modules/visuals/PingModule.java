package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;

/**
 * Ping Module - Shows your ping to the server
 */
public class PingModule extends Module {

    public PingModule() {
        super("Ping", "Shows your ping to the server", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        int ping = 0;
        if (mc.getNetHandler() != null && mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
            ping = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
        }
        String text = "Ping: " + ping + "ms";

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getPingX() : 5;
        int y = designer != null ? designer.getPingY() : 112;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

}
