package com.housingclient.module.modules.movement;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;

public class SprintModule extends Module {

    private final BooleanSetting keepSprint = new BooleanSetting("Keep Sprint",
            "Don't stop sprinting when hitting entities", true);
    private final BooleanSetting omniSprint = new BooleanSetting("Omni Sprint", "Sprint in all directions", false);
    private final BooleanSetting ignoreHunger = new BooleanSetting("Ignore Hunger", "Sprint even with low food", true);

    public SprintModule() {
        super("Sprint", "Always sprint without holding the key", Category.MOVEMENT, ModuleMode.BOTH);

        addSetting(keepSprint);
        addSetting(omniSprint);
        addSetting(ignoreHunger);
    }

    @Override
    public void onTick() {
        updateSprint();
    }

    @Override
    public void onUpdate(net.minecraft.entity.EntityLivingBase entity) {
        updateSprint();
    }

    private void updateSprint() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

        // Don't sprint if sneaking, using item, or not moving
        if (mc.thePlayer.isSneaking())
            return;
        if (mc.thePlayer.isUsingItem())
            return;

        // Check hunger
        if (mc.thePlayer.getFoodStats().getFoodLevel() <= 6 && !ignoreHunger.isEnabled())
            return;

        // Check if moving
        boolean moving = mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;

        if (omniSprint.isEnabled()) {
            // Sprint in any direction
            if (moving && !mc.thePlayer.isCollidedHorizontally) {
                mc.thePlayer.setSprinting(true);
            }
        } else {
            // Normal sprint - only forward
            if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isCollidedHorizontally) {
                mc.thePlayer.setSprinting(true);
            }
        }
    }

    @Override
    public void onJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        // Fix momentum loss when jumping while hungry
        if (mc.thePlayer == null || !ignoreHunger.isEnabled())
            return;

        // Only apply if we are hungry (normally sprint jumping works fine)
        if (mc.thePlayer.getFoodStats().getFoodLevel() > 6)
            return;

        // Assume if we are moving forward/strafing we want to sprint jump
        boolean moving = mc.thePlayer.moveForward != 0 || mc.thePlayer.moveStrafing != 0;

        if (moving && !mc.thePlayer.isSneaking() && !mc.thePlayer.isUsingItem()) {
            // Manually add sprint jump momentum (0.2F)
            float f = mc.thePlayer.rotationYaw * 0.017453292F;
            mc.thePlayer.motionX -= (double) (net.minecraft.util.MathHelper.sin(f) * 0.2F);
            mc.thePlayer.motionZ += (double) (net.minecraft.util.MathHelper.cos(f) * 0.2F);
        }
    }

    public boolean shouldKeepSprint() {
        return isEnabled() && keepSprint.isEnabled();
    }
}
