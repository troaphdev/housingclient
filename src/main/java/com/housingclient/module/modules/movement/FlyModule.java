package com.housingclient.module.modules.movement;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.ClickGUIModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;

public class FlyModule extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", "Fly speed multiplier", 1.0, 0.1, 20.0, 0.1);
    private final NumberSetting sprintSpeed = new NumberSetting("Sprint Speed", "Speed when sprinting", 2.0, 0.5, 50.0,
            0.1);
    private final BooleanSetting doubleTap = new BooleanSetting("Double Tap", "Double tap space to toggle fly", true);
    private final BooleanSetting strictMode = new BooleanSetting("Strict Mode", "Instantly stop motion", false);
    private final BooleanSetting limitEnabled = new BooleanSetting("Limit Height", "Limit vertical fly range", false);
    private final NumberSetting limitHeight = new NumberSetting("Height Limit", "Max blocks up/down", 3.5, 1.0, 20.0,
            0.5);

    private float originalFlySpeed;
    private double startY;

    /**
     * Tracks the server-granted allowFlying state BEFORE the module modifies it.
     * This lets us preserve survival fly permissions in safe mode.
     */
    private boolean serverAllowedFlying = false;

    public FlyModule() {
        super("Creative Flight", "Creative-mode flight with double-tap space", Category.MISCELLANEOUS, ModuleMode.BOTH);

        addSetting(speed);
        addSetting(sprintSpeed);
        addSetting(doubleTap);
        addSetting(strictMode);
        addSetting(limitEnabled);
        addSetting(limitHeight);
    }

    private boolean isBlatantModeEnabled() {
        if (HousingClient.getInstance() == null || HousingClient.getInstance().getModuleManager() == null) {
            return false;
        }
        ClickGUIModule clickGUI = HousingClient.getInstance().getModuleManager().getModule(ClickGUIModule.class);
        return clickGUI != null && clickGUI.isBlatantModeEnabled();
    }

    @Override
    public String getDisplayName() {
        if (!isBlatantModeEnabled()) {
            return "Flight Speed";
        }
        return super.getDisplayName();
    }

    @Override
    public String getDisplayInfo() {
        return mc.thePlayer != null && mc.thePlayer.capabilities.isFlying ? "Active" : "Ready";
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer != null) {
            originalFlySpeed = mc.thePlayer.capabilities.getFlySpeed();
            startY = mc.thePlayer.posY;

            // Save the server-granted fly state BEFORE we modify anything
            serverAllowedFlying = mc.thePlayer.capabilities.allowFlying;

            // Only force fly behavior if in Blatant Mode OR Creative Mode
            if (isBlatantModeEnabled() || mc.thePlayer.capabilities.isCreativeMode) {
                if (!doubleTap.isEnabled()) {
                    // Start flying immediately if double tap is disabled
                    mc.thePlayer.capabilities.allowFlying = true;
                    mc.thePlayer.capabilities.isFlying = true;

                    // Apply fly speed immediately (don't wait for first tick)
                    float flySpeed = (float) (speed.getValue() * 0.05f);
                    mc.thePlayer.capabilities.setFlySpeed(flySpeed);

                    // Give a tiny upward boost to lift off ground immediately
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.1;
                        startY = mc.thePlayer.posY + 0.1;
                    }
                }
            }
            // Safe mode + survival with server fly perms: just apply speed, don't force fly
            // The player can still use their server-granted flight normally
        }
    }

    @Override
    protected void onDisable() {
        if (mc.thePlayer != null) {
            // Restore the original fly speed
            mc.thePlayer.capabilities.setFlySpeed(originalFlySpeed);

            // Safe Mode: Respect server-granted permissions
            if (!isBlatantModeEnabled()) {
                if (mc.thePlayer.capabilities.isCreativeMode) {
                    // Creative mode: keep allowFlying=true (vanilla behavior)
                    mc.thePlayer.capabilities.allowFlying = true;
                } else {
                    // Survival mode: restore to what the server originally granted
                    mc.thePlayer.capabilities.allowFlying = serverAllowedFlying;
                    if (!serverAllowedFlying) {
                        mc.thePlayer.capabilities.isFlying = false;
                    }
                }
                return;
            }

            // Blatant Mode: Full reset
            if (mc.thePlayer.capabilities.isFlying) {
                mc.thePlayer.motionY = -0.1;
            }

            if (strictMode.isEnabled()) {
                mc.thePlayer.motionX = 0;
                mc.thePlayer.motionY = 0;
                mc.thePlayer.motionZ = 0;
            }

            if (mc.thePlayer.capabilities.isCreativeMode) {
                mc.thePlayer.capabilities.allowFlying = true;
            } else {
                mc.thePlayer.capabilities.isFlying = false;
                mc.thePlayer.capabilities.allowFlying = false;
            }
        }
    }

    /**
     * Called from MixinNetHandlerPlayClient when the server sends
     * S39PacketPlayerAbilities.
     * Updates the saved server fly state so we know what the server allows.
     */
    public void updateServerFlyState(boolean allowFlying) {
        this.serverAllowedFlying = allowFlying;
    }

    private void updateSettingsVisibility() {
        if (!isBlatantModeEnabled()) {
            // Safe Mode: Hide risky settings
            speed.setVisible(true);
            sprintSpeed.setVisible(true);
            doubleTap.setVisible(false);
            strictMode.setVisible(false);
            limitEnabled.setVisible(false);
            limitHeight.setVisible(false);
        } else {
            // Blatant Mode: Show all
            speed.setVisible(true);
            sprintSpeed.setVisible(true);
            doubleTap.setVisible(true);
            strictMode.setVisible(true);
            limitEnabled.setVisible(true);
            limitHeight.setVisible(true);
        }
    }

    @Override
    public java.util.List<com.housingclient.module.settings.Setting<?>> getSettings() {
        updateSettingsVisibility();
        return super.getSettings();
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // SAFE MODE (Blatant OFF): strict restrictions
        if (!isBlatantModeEnabled()) {
            updateSettingsVisibility();

            if (mc.thePlayer.capabilities.isCreativeMode) {
                // Creative Mode: Vanilla + Speed settings
                mc.thePlayer.capabilities.allowFlying = true;

                float flySpeed = (float) (speed.getValue() * 0.05f);
                if (mc.thePlayer.isSprinting()) {
                    flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
                }
                mc.thePlayer.capabilities.setFlySpeed(flySpeed);
            } else if (serverAllowedFlying) {
                // Survival mode WITH server fly permissions: apply speed boost only
                // Don't force isFlying — let the player toggle naturally
                mc.thePlayer.capabilities.allowFlying = true;

                float flySpeed = (float) (speed.getValue() * 0.05f);
                if (mc.thePlayer.isSprinting()) {
                    flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
                }
                mc.thePlayer.capabilities.setFlySpeed(flySpeed);
            } else {
                // Survival mode WITHOUT server fly permissions: do nothing
                // Don't give flight to players who don't have it from the server
                mc.thePlayer.capabilities.setFlySpeed(0.05f); // Reset to default
            }
            return;
        }

        // BLATANT MODE
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        // Force allowFlying to true so Vanilla handles the double-tap logic naturally
        mc.thePlayer.capabilities.allowFlying = true;

        boolean isFlying = mc.thePlayer.capabilities.isFlying;

        // Reset startY if we just started flying naturally (double tap enabled)
        if (mc.thePlayer.onGround) {
            startY = mc.thePlayer.posY;
        }

        // Handle "Instant Fly" (Double Tap Disabled)
        if (!doubleTap.isEnabled()) {
            mc.thePlayer.capabilities.isFlying = true;
            isFlying = true;

            if (mc.thePlayer.onGround) {
                mc.thePlayer.motionY = 0.04;
                startY = mc.thePlayer.posY;
            }
        }

        if (isFlying || !doubleTap.isEnabled()) {
            float flySpeed = (float) (speed.getValue() * 0.05f);
            if (mc.thePlayer.isSprinting()) {
                flySpeed = (float) (sprintSpeed.getValue() * 0.05f);
            }
            mc.thePlayer.capabilities.setFlySpeed(flySpeed);

            // Strict Mode: Stop motion if no input
            if (strictMode.isEnabled()) {
                if (mc.thePlayer.movementInput.moveForward == 0 && mc.thePlayer.movementInput.moveStrafe == 0) {
                    mc.thePlayer.motionX = 0;
                    mc.thePlayer.motionZ = 0;
                }

                if (!mc.thePlayer.movementInput.jump && !mc.thePlayer.movementInput.sneak) {
                    mc.thePlayer.motionY = 0;
                }
            }

            // Limit Height Logic
            if (limitEnabled.isEnabled()) {
                double limit = limitHeight.getValue();
                double maxY = startY + limit;
                double minY = startY - limit;

                if (mc.thePlayer.posY > maxY) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, maxY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0;
                } else if (mc.thePlayer.posY < minY) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, minY, mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0;
                }
            }
        }
    }

}
