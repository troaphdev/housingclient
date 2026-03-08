package com.housingclient.module.modules.combat;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;

/**
 * Reach Module
 * Allows customizing left and right click reach distance
 */
public class ReachModule extends Module {

    private final NumberSetting leftReach = new NumberSetting("Left Reach", "Left click reach distance", 3.0, 0.0, 8.0,
            0.1);
    private final NumberSetting rightReach = new NumberSetting("Right Reach", "Right click reach distance", 4.5, 0.0,
            8.0, 0.1);

    public ReachModule() {
        super("Reach", "Modify attack and interaction reach", Category.MISCELLANEOUS, ModuleMode.BOTH);

        addSetting(leftReach);
        addSetting(rightReach);
    }

    public double getLeftReach() {
        if (!isEnabled())
            return 3.0; // Default MC reach
        return leftReach.getValue();
    }

    public double getRightReach() {
        if (!isEnabled())
            return 4.5; // Default MC interaction reach
        return rightReach.getValue();
    }

    /**
     * Check if an entity is within left click (attack) reach
     */
    public boolean isInAttackReach(Entity target) {
        if (mc.thePlayer == null || target == null)
            return false;
        double distance = mc.thePlayer.getDistanceToEntity(target);
        return distance <= getLeftReach();
    }

    /**
     * Check if an entity is within right click (interact) reach
     */
    public boolean isInInteractReach(Entity target) {
        if (mc.thePlayer == null || target == null)
            return false;
        double distance = mc.thePlayer.getDistanceToEntity(target);
        return distance <= getRightReach();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("L:%.1f R:%.1f", leftReach.getValue(), rightReach.getValue());
    }
}
