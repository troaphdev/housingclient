package com.housingclient.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.housingclient.HousingClient;
import com.housingclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class KeybindManager {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private final File keybindsFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Map<Integer, Runnable> customKeybinds = new HashMap<>();
    private final boolean[] keyStates = new boolean[256];
    private final boolean[] mouseStates = new boolean[20]; // Support up to 20 mouse buttons

    public KeybindManager() {
        keybindsFile = new File(HousingClient.getInstance().getDataDir(), "keybinds.json");
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (mc.thePlayer == null)
            return;
        // Allow keybinds only when no GUI is open, or specifically inside a standard
        // Chest GUI
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest))
            return;

        int key = Keyboard.getEventKey();
        if (key <= 0 || key >= keyStates.length)
            return;

        boolean pressed = Keyboard.getEventKeyState();

        if (pressed && !keyStates[key]) {
            // Key just pressed
            keyStates[key] = true;
            handleKeyPress(key);
        } else if (!pressed && keyStates[key]) {
            // Key released
            keyStates[key] = false;
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (mc.thePlayer == null)
            return;
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiChest))
            return;

        int button = Mouse.getEventButton();
        if (button < 3 || button >= mouseStates.length)
            return; // Only side buttons (3+), skip LMB/RMB/MMB

        boolean pressed = Mouse.getEventButtonState();

        if (pressed && !mouseStates[button]) {
            mouseStates[button] = true;
            // Mouse buttons are stored as negative keybind values: -(button + 100)
            handleKeyPress(-(button + 100));
        } else if (!pressed && mouseStates[button]) {
            mouseStates[button] = false;
        }
    }

    private void handleKeyPress(int key) {
        // Check module keybinds (supports per-action binds via Module.handleKeybindPress)
        for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
            module.handleKeybindPress(key);
            // Don't return - allow multiple modules with same keybind
        }

        // Check custom keybinds
        if (customKeybinds.containsKey(key)) {
            customKeybinds.get(key).run();
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;

        // Reset key states when screen opens
        if (mc.currentScreen != null) {
            resetKeyStates();
        }
    }

    public void registerKeybind(int key, Runnable action) {
        customKeybinds.put(key, action);
    }

    public void unregisterKeybind(int key) {
        customKeybinds.remove(key);
    }

    public void loadKeybinds() {
        if (!keybindsFile.exists())
            return;

        try (FileReader reader = new FileReader(keybindsFile)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null)
                return;

            if (json.has("modules")) {
                JsonObject modules = json.getAsJsonObject("modules");
                for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
                    if (modules.has(module.getName())) {
                        module.setKeybind(modules.get(module.getName()).getAsInt());
                    }
                }
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to load keybinds", e);
        }
    }

    public void saveKeybinds() {
        try {
            JsonObject json = new JsonObject();

            JsonObject modules = new JsonObject();
            for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
                if (module.getKeybind() != 0) {
                    modules.addProperty(module.getName(), module.getKeybind());
                }
            }
            json.add("modules", modules);

            try (FileWriter writer = new FileWriter(keybindsFile)) {
                gson.toJson(json, writer);
            }
        } catch (IOException e) {
            HousingClient.LOGGER.error("Failed to save keybinds", e);
        }
    }

    /**
     * Returns a display name for the given keybind code.
     * Positive values are keyboard keys, negative values are mouse buttons
     * using the convention -(button + 100).
     */
    public static String getKeyName(int key) {
        if (key == 0)
            return "None";
        if (key < 0) {
            // Mouse button: encoded as -(button + 100)
            int btn = -(key + 100);
            if (btn == 0) return "M1 (Left)";
            if (btn == 1) return "M2 (Right)";
            if (btn == 2) return "M3 (Middle)";
            return "Mouse " + (btn + 1);
        }
        String name = Keyboard.getKeyName(key);
        return name != null ? name : "Key" + key;
    }

    public static int getKeyCode(String name) {
        return Keyboard.getKeyIndex(name.toUpperCase());
    }

    public void resetKeyStates() {
        for (int i = 0; i < keyStates.length; i++) {
            keyStates[i] = false;
        }
        for (int i = 0; i < mouseStates.length; i++) {
            mouseStates[i] = false;
        }
    }
}

