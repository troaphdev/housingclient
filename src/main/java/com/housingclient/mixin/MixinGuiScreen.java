package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.Module;
import com.housingclient.module.modules.visuals.FancyTextModule;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts handleKeyboardInput to replace the char argument
 * passed to keyTyped() — the central dispatch for ALL GUI keyboard input.
 * This ensures fancy text works universally across chat, signs, anvils, books,
 * etc.
 * 
 * Search boxes (ClickGUI, Creative inventory) are excluded so searching still
 * works normally.
 * 
 * Also handles module keybinds inside container GUIs (chests, dispensers, etc.)
 * since Forge's InputEvent.KeyInputEvent does not fire when a GUI is open.
 */
@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen {

    /**
     * Intercept handleKeyboardInput to check module keybinds when inside
     * a container GUI (chest, dispenser, etc.). Forge's KeyInputEvent
     * does NOT fire when any GUI is open, so this is the only way
     * to support keybinds inside containers.
     */
    @Inject(method = "func_146282_l", at = @At("HEAD"))
    private void onHandleKeyboardInput(CallbackInfo ci) {
        try {
            GuiScreen self = (GuiScreen) (Object) this;
            // Only handle keybinds in standard Chest GUIs (fixes typing triggering binds in
            // Anvils/Creative Search)
            if (!(self instanceof net.minecraft.client.gui.inventory.GuiChest))
                return;

            if (!Keyboard.getEventKeyState())
                return; // Only on key press, not release
            int key = Keyboard.getEventKey();
            if (key <= 0)
                return;

            // Check all module keybinds
            if (HousingClient.getInstance() != null && HousingClient.getInstance().getModuleManager() != null) {
                for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
                    if (module.getKeybind() == key) {
                        module.toggle();
                    }
                }
            }
        } catch (Throwable t) {
            // Fail silently
        }
    }

    /**
     * Modify the first char argument of the keyTyped() call inside
     * handleKeyboardInput().
     * This fires before any subclass override of keyTyped receives the char.
     */
    @ModifyArg(method = "func_146282_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;func_73869_a(CI)V"), index = 0)
    private char modifyTypedChar(char original) {
        // Don't modify control characters (backspace, enter, escape, tab, etc.)
        if (original < 32 && original != 0) {
            return original;
        }

        // Skip conversion for search-related GUIs
        GuiScreen self = (GuiScreen) (Object) this;

        // Skip: HousingClient ClickGUI (has search box)
        if (self instanceof com.housingclient.gui.ClickGUI) {
            return original;
        }

        // Skip: HousingClient Item Selector (has search box)
        if (self instanceof com.housingclient.gui.ItemSelectorGUI) {
            return original;
        }

        // Skip: Minecraft Creative Inventory (has search tab)
        if (self instanceof GuiContainerCreative) {
            return original;
        }

        try {
            FancyTextModule fancyText = HousingClient.getInstance().getModuleManager()
                    .getModule(FancyTextModule.class);
            if (fancyText != null && fancyText.isEnabled()) {
                return fancyText.convertChar(original);
            }
        } catch (Exception e) {
            // Fail silently - module may not be loaded yet
        }
        return original;
    }
}
