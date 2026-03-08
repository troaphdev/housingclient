package com.housingclient.module.modules.movement;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.PlayerUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class SpeedModule extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Speed mode", "Strafe", "Strafe", "OnGround", "Boost");
    private final NumberSetting speed = new NumberSetting("Speed", "Speed multiplier", 1.2, 0.5, 3.0, 0.1);
    private final NumberSetting jumpHeight = new NumberSetting("Jump Height", "Jump height modifier", 0.42, 0.3, 0.5,
            0.01);

    private int stage = 0;
    private double moveSpeed = 0;

    public SpeedModule() {
        super("Speed", "Move faster", Category.MISCELLANEOUS, ModuleMode.BOTH);
        setBlatant(true);

        addSetting(mode);
        addSetting(speed);
        addSetting(jumpHeight);
    }

    @Override
    protected void onEnable() {
        stage = 0;
        moveSpeed = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END)
            return;
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        if (!PlayerUtils.isMoving())
            return;

        switch (mode.getValue()) {
            case "Strafe":
                handleStrafe();
                break;
            case "OnGround":
                handleOnGround();
                break;
            case "Boost":
                handleBoost();
                break;
        }
    }

    private void handleStrafe() {
        if (mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            moveSpeed = getBaseMoveSpeed() * speed.getValue();
        } else {
            moveSpeed = getBaseMoveSpeed() * speed.getValue() * 0.9;
        }

        PlayerUtils.setSpeed(moveSpeed);
    }

    private void handleOnGround() {
        if (!mc.thePlayer.onGround)
            return;

        moveSpeed = getBaseMoveSpeed() * speed.getValue();
        PlayerUtils.setSpeed(moveSpeed);
    }

    private void handleBoost() {
        if (mc.thePlayer.onGround) {
            stage = 0;
            mc.thePlayer.motionY = jumpHeight.getValue();
            moveSpeed = getBaseMoveSpeed() * speed.getValue() * 1.5;
        } else {
            stage++;
            if (stage == 1) {
                moveSpeed *= 1.05;
            } else {
                moveSpeed *= 0.98;
            }
        }

        moveSpeed = Math.max(moveSpeed, getBaseMoveSpeed());
        PlayerUtils.setSpeed(moveSpeed);
    }

    private double getBaseMoveSpeed() {
        double base = 0.2873;
        if (mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSpeed)) {
            int amp = mc.thePlayer.getActivePotionEffect(net.minecraft.potion.Potion.moveSpeed).getAmplifier();
            base *= 1.0 + 0.2 * (amp + 1);
        }
        return base;
    }

    @Override
    public String getDisplayInfo() {
        return mode.getValue();
    }
}
