package com.housingclient.module.modules.client;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controls only whether selected enabled modules are drawn in the HUD module
 * list. It does not disable modules or remove them from HousingClient's menus.
 */
public class HideModulesModule extends Module {

    private static HideModulesModule instance;

    private final Map<String, BooleanSetting> hiddenModuleSettings = new LinkedHashMap<>();

    public HideModulesModule() {
        super("Hide Modules", "Choose enabled modules to hide from the on-screen module list",
                Category.CLIENT, ModuleMode.BOTH);
        instance = this;
    }

    /**
     * Called after ModuleManager has registered every module so all eligible
     * entries are available as native toggle settings before config loading.
     */
    public void initializeModuleSettings(List<Module> modules) {
        if (!hiddenModuleSettings.isEmpty()) {
            return;
        }

        for (Module module : modules) {
            String name = module.getName();
            if (isAlwaysExcludedFromModuleList(name)) {
                continue;
            }

            BooleanSetting setting = new BooleanSetting(
                    name,
                    "Hide " + name + " from the on-screen module list",
                    false);
            hiddenModuleSettings.put(normalize(name), setting);
            addSetting(setting);
        }
    }

    public static boolean shouldHide(Module module) {
        if (module == null || instance == null || !instance.isEnabled()) {
            return false;
        }

        BooleanSetting setting = instance.hiddenModuleSettings.get(normalize(module.getName()));
        return setting != null && setting.isEnabled();
    }

    private static boolean isAlwaysExcludedFromModuleList(String name) {
        return "Module List".equalsIgnoreCase(name)
                || "ClickGUI".equalsIgnoreCase(name)
                || "HUD Designer".equalsIgnoreCase(name);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
