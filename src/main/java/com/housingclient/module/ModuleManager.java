package com.housingclient.module;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.building.*;
import com.housingclient.module.modules.client.*;
import com.housingclient.module.modules.combat.*;
import com.housingclient.module.modules.freebuild.*;
import com.housingclient.module.modules.movement.*;
import com.housingclient.module.modules.visuals.*;
import com.housingclient.module.modules.render.*;
import com.housingclient.module.modules.items.*;
import com.housingclient.module.modules.exploit.*;

import com.housingclient.module.modules.moderation.*;

import com.housingclient.module.modules.qol.*;
import com.housingclient.module.settings.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {

    private final List<Module> modules = new ArrayList<Module>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File modulesFile;

    public ModuleManager() {
        modulesFile = new File(HousingClient.getInstance().getDataDir(), "modules.json");
        registerModules();
        loadModuleStates();
    }

    private void registerModules() {
        // Movement modules
        modules.add(new FlyModule());
        modules.add(new SprintModule());
        modules.add(new SpeedModule());

        // Visuals modules
        modules.add(new StorageESPModule());
        modules.add(new FullbrightModule());
        modules.add(new FreeCamModule());
        modules.add(new TrueSightModule());
        modules.add(new ChamsModule()); // Render Chams first
        modules.add(new NametagsModule()); // Render Nametags after (on top)
        modules.add(new ZoomModule());
        modules.add(new TracersModule());
        modules.add(new ESPModule());
        modules.add(new SearchModule());
        modules.add(new WeatherModule());
        // AntiBot removed
        modules.add(new LoadedPlayersModule());
        modules.add(new TPSModule());
        modules.add(new HideEntitiesModule());
        modules.add(new CPSModule());
        modules.add(new FPSModule());
        modules.add(new PingModule());
        modules.add(new CoordsModule());
        modules.add(new DirectionModule());
        modules.add(new BiomeModule());
        modules.add(new ClockModule());
        modules.add(new ScoreboardModule());
        modules.add(new ActiveEffectsModule());

        // Render modules
        modules.add(new HideHykiaEntitiesModule());

        // Moderation modules
        modules.add(new GrieferDetectorModule());

        // Combat modules
        modules.add(new AutoclickerModule());
        modules.add(new ReachModule());
        modules.add(new NoDebuffModule());

        // Building modules
        modules.add(new FastPlaceModule());
        modules.add(new GhostBlocksModule());
        // modules.add(new FastDispenserModule()); // PATCHED - Temporarily disabled
        modules.add(new NukerModule());
        modules.add(new FastBreakModule());

        // QOL modules
        modules.add(new AntiVoidLagModule());

        // Client modules
        modules.add(new HUDModule());
        modules.add(new ClickGUIModule());
        modules.add(new FriendsModule());
        modules.add(new HudDesignerModule());
        modules.add(new ChatModule());
        modules.add(new CrasherDetectorModule());
        modules.add(new FancyTextModule());

        modules.add(new ItemStealerModule());

        // Items modules
        // modules.add(new CrashEggDispenserModule()); // PATCHED - Temporarily disabled

        // Exploit modules
        modules.add(new NBTLoggerModule());
        // modules.add(new PlayerCrasherModule()); // PATCHED - Temporarily disabled
        modules.add(new ServerMatcherModule());
        modules.add(new BlinkModule());
        modules.add(new BypassBlacklistModule());

        modules.add(new DispenserFillModule());
        modules.add(new ImageToNBTModule());
        modules.add(new GhostDiscModule());
        modules.add(new PacketMultiplierModule());

        HousingClient.LOGGER.info("Registered " + modules.size() + " modules.");
    }

    public List<Module> getModules() {
        return modules;
    }

    public Module getModule(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        return null;
    }

    public Module getModuleByName(String name) {
        for (Module module : modules) {
            if (module.getName().equalsIgnoreCase(name)) {
                return module;
            }
        }
        for (Module module : modules) {
            if (module.getName().toLowerCase().contains(name.toLowerCase())) {
                return module;
            }
        }
        String noSpaces = name.replace(" ", "").toLowerCase();
        for (Module module : modules) {
            if (module.getName().replace(" ", "").toLowerCase().equals(noSpaces)) {
                return module;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> clazz) {
        for (Module module : modules) {
            if (clazz.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public List<Module> getModulesByCategory(Category category) {
        return modules.stream()
                .filter(m -> m.getCategory() == category)
                .collect(Collectors.toList());
    }

    public List<Module> getEnabledModules() {
        return modules.stream()
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public List<Module> getVisibleModules() {
        return modules.stream()
                .filter(Module::isVisible)
                .filter(Module::isEnabled)
                .collect(Collectors.toList());
    }

    public List<Module> getVisibleModulesSorted() {
        return modules.stream()
                .filter(Module::isVisible)
                .filter(Module::isEnabled)
                .sorted(Comparator.comparingInt(m -> -m.getName().length()))
                .collect(Collectors.toList());
    }

    public void disableAll() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.setEnabled(false);
            }
        }
    }

    public void saveModuleStates() {
        try {
            JsonObject json = new JsonObject();

            for (Module module : modules) {
                JsonObject moduleJson = new JsonObject();
                moduleJson.addProperty("enabled", module.isEnabled());
                moduleJson.addProperty("keybind", module.getKeybind());
                moduleJson.addProperty("visible", module.isVisible());

                JsonObject settingsJson = new JsonObject();
                for (Setting<?> setting : module.getSettings()) {
                    settingsJson.add(setting.getName(), setting.toJson());
                }
                moduleJson.add("settings", settingsJson);

                json.add(module.getName(), moduleJson);
            }

            try (FileWriter writer = new FileWriter(modulesFile)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save module states", e);
        }
    }

    public void loadModuleStates() {
        if (!modulesFile.exists())
            return;

        try (FileReader reader = new FileReader(modulesFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null)
                return;

            for (Module module : modules) {
                if (json.has(module.getName())) {
                    JsonObject moduleJson = json.getAsJsonObject(module.getName());

                    if (moduleJson.has("keybind")) {
                        module.setKeybind(moduleJson.get("keybind").getAsInt());
                    }
                    if (moduleJson.has("visible")) {
                        module.setVisible(moduleJson.get("visible").getAsBoolean());
                    }

                    if (moduleJson.has("settings")) {
                        JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                        for (Setting<?> setting : module.getSettings()) {
                            if (settingsJson.has(setting.getName())) {
                                setting.fromJson(settingsJson.get(setting.getName()));
                            }
                        }
                    }

                    if (moduleJson.has("enabled") && moduleJson.get("enabled").getAsBoolean()) {
                        module.setEnabled(true);
                    }
                }
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to load module states", e);
        }
    }

    public void onTick() {
        boolean safeMode = HousingClient.getInstance().isSafeMode();
        for (Module module : modules) {
            if (safeMode && module.isBlatant() && module.isEnabled()) {
                module.setEnabled(false);
            }
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }

    public void onRender() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender();
            }
        }
    }

    public void onRender3D(float partialTicks) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender3D(partialTicks);
            }
        }
    }

    /**
     * Called during 3D world rendering BEFORE view bobbing is applied.
     * Dispatches to all enabled modules that override onRender3DPreBobbing.
     */
    public void onRender3DPreBobbing(float partialTicks) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onRender3DPreBobbing(partialTicks);
            }
        }
    }

    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onUpdate(entity);
            }
        }
    }

    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onJump(event);
            }
        }
    }
}
