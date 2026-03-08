package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.ModeSetting;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Clock Module - Shows the current real-world time
 */
public class ClockModule extends Module {

    private final ModeSetting timeFormat = new ModeSetting("Format", "Time format", "24h", "24h", "12h");

    public ClockModule() {
        super("Clock", "Shows the current time", Category.VISUALS, ModuleMode.BOTH);
        addSetting(timeFormat);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        // Format based on setting
        String pattern = timeFormat.getValue().equals("12h") ? "hh:mm:ss a" : "HH:mm:ss";
        String time = new SimpleDateFormat(pattern).format(new Date());

        // Get position from HudDesigner
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getTimeX() : 5;
        int y = designer != null ? designer.getTimeY() : 5;

        mc.fontRendererObj.drawStringWithShadow(time, x, y, 0xFFAAAAAA);
    }

    @Override
    public String getDisplayInfo() {
        // Return the mode instead of the time - shows "clock 24h" or "clock 12h" in
        // module list
        return timeFormat.getValue();
    }
}
