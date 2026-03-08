package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class FullbrightModule extends Module {
    
    private final NumberSetting brightness = new NumberSetting("Brightness", "Gamma level", 10.0, 1.0, 15.0, 0.5);
    
    private float originalGamma = 1.0f;
    
    public FullbrightModule() {
        super("Fullbright", "Maximum brightness everywhere", Category.VISUALS, ModuleMode.BOTH);
        
        addSetting(brightness);
    }
    
    @Override
    protected void onEnable() {
        if (mc.gameSettings != null) {
            originalGamma = mc.gameSettings.gammaSetting;
        }
    }
    
    @Override
    protected void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.gammaSetting = originalGamma;
        }
    }
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.gameSettings == null) return;
        
        mc.gameSettings.gammaSetting = brightness.getFloatValue();
    }
}

