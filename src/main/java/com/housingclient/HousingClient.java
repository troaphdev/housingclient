package com.housingclient;

import com.housingclient.config.ConfigManager;
import com.housingclient.config.ProfileManager;
import com.housingclient.gui.ClickGUI;
import com.housingclient.gui.LegacyClickGUI;
import com.housingclient.gui.HUD;
import com.housingclient.itemlog.ItemLogManager;
import com.housingclient.storage.ItemLogTab;
import com.housingclient.module.ModuleManager;
import com.housingclient.module.modules.client.ClickGUIModule;
import com.housingclient.storage.CreativeTabStorage;
import com.housingclient.storage.ItemStealerStorage;
import com.housingclient.utils.ChatUtils;
import com.housingclient.utils.HousingDetector;
import com.housingclient.utils.KeybindManager;
import com.housingclient.command.CommandManager;
import com.housingclient.command.ChatCommandHandler;
import com.housingclient.event.EventManager;
import com.housingclient.event.ChatSendHandler;
import com.housingclient.event.InputHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.io.File;

@Mod(modid = HousingClient.MODID, name = HousingClient.NAME, version = HousingClient.VERSION, clientSideOnly = true)
public class HousingClient {

    public static final String MODID = "housingclient";
    public static final String NAME = "Housing Client Pro";
    public static final String VERSION = "1.0.4";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @Mod.Instance(MODID)
    public static HousingClient instance;

    private static Minecraft mc;
    private ModuleManager moduleManager;
    private ConfigManager configManager;
    private ProfileManager profileManager;
    private KeybindManager keybindManager;
    private CommandManager commandManager;
    private EventManager eventManager;
    private CreativeTabStorage creativeTabStorage;
    private ItemStealerStorage itemStealerStorage;
    private ItemLogManager itemLogManager;

    private HousingDetector housingDetector;
    private ClickGUI clickGUI;
    private LegacyClickGUI legacyClickGUI;
    private HUD hud;

    // Forge keybind for GUI
    private KeyBinding guiKeyBinding;

    private File dataDir;
    private boolean safeMode = false;
    private boolean isOwnerMode = false;

    // Chat interception
    private String lastChatInput = "";
    private int tickCounter = 0;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        mc = Minecraft.getMinecraft();
        dataDir = new File(mc.mcDataDir, "housingclient");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        LOGGER.info("===========================================");
        LOGGER.info("HousingClient v" + VERSION + " - Pre-Init");
        LOGGER.info("===========================================");

        // Initialize config first
        configManager = new ConfigManager(dataDir);
        configManager.loadConfig();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("HousingClient - Initializing...");

        // Register GUI keybind
        guiKeyBinding = new KeyBinding("Open HousingClient GUI", Keyboard.KEY_RSHIFT, "HousingClient");
        ClientRegistry.registerKeyBinding(guiKeyBinding);

        // Initialize managers
        eventManager = new EventManager();
        moduleManager = new ModuleManager();
        profileManager = new ProfileManager(dataDir);
        keybindManager = new KeybindManager();
        commandManager = new CommandManager();
        creativeTabStorage = new CreativeTabStorage(dataDir);
        itemStealerStorage = new ItemStealerStorage(dataDir);
        itemStealerStorage.load();
        itemLogManager = new ItemLogManager();
        new ItemLogTab();
        new com.housingclient.storage.ItemStealerTab(); // Initialize tab
        housingDetector = new HousingDetector();

        // Initialize FontManager
        com.housingclient.utils.font.FontManager.getInstance().loadFonts();

        // Initialize GUI
        clickGUI = new ClickGUI();
        legacyClickGUI = new LegacyClickGUI();
        hud = new HUD();

        // Register event handlers
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(eventManager);

        MinecraftForge.EVENT_BUS.register(keybindManager);
        MinecraftForge.EVENT_BUS.register(housingDetector);
        MinecraftForge.EVENT_BUS.register(hud);
        MinecraftForge.EVENT_BUS.register(new InputHandler()); // CPS tracking
        MinecraftForge.EVENT_BUS.register(new ChatSendHandler());
        // MinecraftForge.EVENT_BUS.register(new
        // com.housingclient.event.TabRainbowHandler()); // Replaced by Mixin

        // Load saved data
        profileManager.loadProfiles();
        creativeTabStorage.load();
        keybindManager.loadKeybinds();

        LOGGER.info("HousingClient - Initialization complete!");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("===========================================");
        LOGGER.info("HousingClient loaded successfully!");
        if (moduleManager != null) {
            LOGGER.info("Loaded " + moduleManager.getModules().size() + " modules");
        } else {
            LOGGER.warn("ModuleManager is null — init phase may have been skipped!");
        }
        LOGGER.info("Press RIGHT SHIFT to open the GUI");
        LOGGER.info("Use . commands in chat (e.g. .help)");
        LOGGER.info("===========================================");

        // Inject creative tab translations at runtime
        injectTranslations();

        LOGGER.info("--- Creative Tabs Dump ---");
        for (net.minecraft.creativetab.CreativeTabs tab : net.minecraft.creativetab.CreativeTabs.creativeTabArray) {
            if (tab != null) {
                LOGGER.info("Index: " + tab.getTabIndex() + " Label: " + tab.getTabLabel() + " Class: "
                        + tab.getClass().getName());
            } else {
                LOGGER.info("Index: NULL SLOT");
            }
        }
        LOGGER.info("--------------------------");
    }

    /**
     * Inject custom translations into MC's language map at runtime.
     * This is needed because Forge 1.8.9 doesn't always load mod lang files
     * properly.
     */
    private void injectTranslations() {
        try {
            // Get the I18n's internal Locale instance via reflection
            java.lang.reflect.Field localeField;
            try {
                localeField = net.minecraft.client.resources.I18n.class.getDeclaredField("i18nLocale");
            } catch (NoSuchFieldException e) {
                // Fallback to obfuscated field name (SRG)
                localeField = net.minecraft.client.resources.I18n.class.getDeclaredField("field_135054_a");
            }
            localeField.setAccessible(true);
            net.minecraft.client.resources.Locale locale = (net.minecraft.client.resources.Locale) localeField
                    .get(null);

            if (locale == null) {
                LOGGER.warn("I18n locale is null, translations not injected");
                return;
            }

            // Access the properties map inside Locale
            java.lang.reflect.Field propsField = null;
            for (java.lang.reflect.Field f : net.minecraft.client.resources.Locale.class.getDeclaredFields()) {
                if (java.util.Map.class.isAssignableFrom(f.getType())) {
                    propsField = f;
                    break;
                }
            }

            if (propsField != null) {
                propsField.setAccessible(true);
                @SuppressWarnings("unchecked")
                java.util.Map<String, String> langMap = (java.util.Map<String, String>) propsField.get(locale);

                if (langMap != null) {
                    // Add our translations
                    langMap.put("itemGroup.hclogger", "NBT Logger");
                    langMap.put("itemGroup.itemstealer", "Item Stealer");

                    LOGGER.info("Injected creative tab translations successfully!");
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to inject translations: " + e.getMessage());
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null)
            return;

        // Check if our keybind was pressed
        if (guiKeyBinding.isPressed()) {
            ClickGUIModule mod = moduleManager.getModule(ClickGUIModule.class);
            if (mod != null && mod.isLegacyMode()) {
                mc.displayGuiScreen(legacyClickGUI);
            } else {
                clickGUI.reset();
                mc.displayGuiScreen(clickGUI);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        if (!Display.getTitle().equals(NAME)) {
            Display.setTitle(NAME);
        }

        // Update rainbow name cache every 20 ticks (1 second) for responsive tab list
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            if (mc.thePlayer != null) {
                com.housingclient.utils.HousingClientUserManager.getInstance().updateRainbowNameCache();
            }
        }

        if (mc.thePlayer == null)
            return;

        // Module onClientTick
        moduleManager.onTick();

        // Check for chat input with . prefix
        if (mc.currentScreen instanceof GuiChat) {
            try {

                java.lang.reflect.Field inputField = GuiChat.class.getDeclaredField("inputField");
                inputField.setAccessible(true);
                net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) inputField
                        .get(mc.currentScreen);

                if (textField != null) {
                    lastChatInput = textField.getText();
                }
            } catch (Exception e) {
                // Try obfuscated name
                try {
                    java.lang.reflect.Field inputField = GuiChat.class.getDeclaredField("field_146415_a");
                    inputField.setAccessible(true);
                    net.minecraft.client.gui.GuiTextField textField = (net.minecraft.client.gui.GuiTextField) inputField
                            .get(mc.currentScreen);

                    if (textField != null) {
                        lastChatInput = textField.getText();
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Called when the player sends a chat message
     * 
     * @return true if message was handled (don't send to server)
     */
    public boolean onChatMessage(String message) {
        if (ChatCommandHandler.isCommand(message)) {
            return ChatCommandHandler.handleChatMessage(message);
        }
        return false;
    }

    public static Minecraft getMinecraft() {
        return mc;
    }

    public static HousingClient getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public KeybindManager getKeybindManager() {
        return keybindManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public EventManager getEventManager() {
        return eventManager;
    }

    public CreativeTabStorage getCreativeTabStorage() {
        return creativeTabStorage;
    }

    public ItemStealerStorage getItemStealerStorage() {
        return itemStealerStorage;
    }

    public HousingDetector getHousingDetector() {
        return housingDetector;
    }

    public ClickGUI getClickGUI() {
        return clickGUI;
    }

    public HUD getHud() {
        return hud;
    }

    public File getDataDir() {
        return dataDir;
    }

    public boolean isSafeMode() {
        if (moduleManager == null)
            return false;
        ClickGUIModule mod = moduleManager.getModule(ClickGUIModule.class);
        return mod != null && !mod.isBlatantModeEnabled();
    }

    // setSafeMode/toggleSafeMode removed as they are now controlled by the setting
    // directly
    @Deprecated
    public void setSafeMode(boolean safeMode) {
        // No-op or update setting?
        // Best to just rely on the setting.
    }

    public void toggleSafeMode() {
        // No-op
    }

    public boolean isOwnerMode() {
        return isOwnerMode;
    }

    public void setOwnerMode(boolean ownerMode) {
        this.isOwnerMode = ownerMode;
    }

    public void saveAll() {
        configManager.saveConfig();
        profileManager.saveProfiles();
        creativeTabStorage.save();
        keybindManager.saveKeybinds();
        moduleManager.saveModuleStates();
    }

    public void onShutdown() {
        saveAll();
        LOGGER.info("HousingClient shutdown complete.");
    }
}
