package com.housingclient.module;

import com.housingclient.HousingClient;

import com.housingclient.module.settings.Setting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getMinecraft();

    private final String name;
    private final String description;
    private final Category category;
    private final ModuleMode mode;
    private boolean enabled;
    private int keybind;
    private boolean visible = true;
    private boolean isBlatant = false;

    // HUD Properties
    private int width = 0;
    private int height = 0;

    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, ModuleMode mode) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.mode = mode;
        this.enabled = false;
        this.keybind = 0;
    }

    public Module(String name, String description, Category category, ModuleMode mode, int defaultKeybind) {
        this(name, description, category, mode);
        this.keybind = defaultKeybind;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled)
            return;

        // Safe Mode guard - prevent enabling blatant modules if Safe Mode is active
        if (enabled && isBlatant() && HousingClient.getInstance().isSafeMode()) {
            return; // Silently refuse
        }

        this.enabled = enabled;

        // Show toggle notification if enabled in settings
        if (mc.theWorld != null && shouldShowNotification()) {
            com.housingclient.module.modules.client.ClickGUIModule clickGui = HousingClient.getInstance()
                    .getModuleManager().getModule(
                            com.housingclient.module.modules.client.ClickGUIModule.class);
            if (clickGui != null && clickGui.isNotificationsEnabled()) {
                com.housingclient.gui.ToggleNotification.addNotification(this, enabled);
            }
        }

        if (enabled) {
            onEnable();
            MinecraftForge.EVENT_BUS.register(this);
        } else {
            MinecraftForge.EVENT_BUS.unregister(this);
            onDisable();
        }
    }

    public boolean isAvailable() {
        return true;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    public void onTick() {
    }

    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
    }

    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
    }

    public void onRender() {
    }

    public void onRender3D(float partialTicks) {
    }

    /**
     * Called during 3D world rendering BEFORE view bobbing is applied.
     * Use this for rendering that needs a stable camera without bobbing effects.
     * 
     * @param partialTicks Partial tick time for interpolation
     */
    public void onRender3DPreBobbing(float partialTicks) {
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDisplayName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public ModuleMode getMode() {
        return mode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getKeybind() {
        return keybind;
    }

    public void setKeybind(int keybind) {
        this.keybind = keybind;
        // Auto-save keybinds
        if (HousingClient.getInstance() != null && HousingClient.getInstance().getKeybindManager() != null) {
            HousingClient.getInstance().getKeybindManager().saveKeybinds();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public List<Setting<?>> getSettings() {
        return settings;
    }

    protected <T extends Setting<?>> T addSetting(T setting) {
        settings.add(setting);
        return setting;
    }

    public Setting<?> getSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equalsIgnoreCase(name)) {
                return setting;
            }
        }
        return null;
    }

    public String getDisplayInfo() {
        return null;
    }

    public boolean isBlatant() {
        return isBlatant;
    }

    public void setBlatant(boolean blatant) {
        this.isBlatant = blatant;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    protected boolean shouldShowNotification() {
        return true;
    }
}
