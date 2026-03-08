package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;

/**
 * Biome Module - Shows the current biome
 */
public class BiomeModule extends Module {

    public BiomeModule() {
        super("Biome", "Shows the current biome", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        String biome = mc.theWorld.getBiomeGenForCoords(mc.thePlayer.getPosition()).biomeName;
        String text = "Biome: " + biome;

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getBiomeX() : 5;
        int y = designer != null ? designer.getBiomeY() : 136;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);
    }

    @Override
    public String getDisplayInfo() {
        return null;
    }
}
