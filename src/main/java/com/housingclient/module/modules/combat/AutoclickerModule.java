package com.housingclient.module.modules.combat;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.ModeSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.TimerUtils;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Mouse;

import java.util.Random;

public class AutoclickerModule extends Module {

    // CPS Settings
    private final NumberSetting minCPS = new NumberSetting("Min CPS", "Minimum clicks per second", 8.0, 1.0, 25.0, 0.5);
    private final NumberSetting maxCPS = new NumberSetting("Max CPS", "Maximum clicks per second", 12.0, 1.0, 25.0,
            0.5);

    // Click Settings
    private final ModeSetting clickMode = new ModeSetting("Mode", "Click activation mode", "Hold", "Hold", "Toggle",
            "Always");
    private final BooleanSetting leftClick = new BooleanSetting("Left Click", "Autoclick left mouse", true);
    private final BooleanSetting rightClick = new BooleanSetting("Right Click", "Autoclick right mouse", false);
    private final NumberSetting holdPercentage = new NumberSetting("Hold Time", "How long click is held (%)", 50.0,
            10.0, 90.0, 5.0);

    // Humanization Settings
    private final BooleanSetting humanization = new BooleanSetting("Humanization", "Enable human-like patterns", true);
    private final NumberSetting jitter = new NumberSetting("Jitter", "Random mouse movement", 0.0, 0.0, 5.0, 0.1);
    private final BooleanSetting fatigue = new BooleanSetting("Fatigue Sim", "Simulate clicking fatigue", true);
    private final NumberSetting burstChance = new NumberSetting("Burst Chance", "Chance of speed burst (%)", 5.0, 0.0,
            20.0, 1.0);
    private final NumberSetting pauseChance = new NumberSetting("Pause Chance", "Chance of micro-pause (%)", 2.0, 0.0,
            10.0, 0.5);

    // Combat Settings
    private final BooleanSetting blockHit = new BooleanSetting("Block Hit", "Auto block after hits", false);
    private final NumberSetting blockHitDelay = new NumberSetting("Block Delay", "Ticks between block hits", 4, 2, 10);
    private final BooleanSetting onlyWeapon = new BooleanSetting("Only Weapon", "Only with weapon in hand", false);
    private final BooleanSetting onlyEntity = new BooleanSetting("Only Entity", "Only when looking at entity", false);

    // Breaking Settings
    private final BooleanSetting breakBlocks = new BooleanSetting("Break Blocks", "Also for block breaking", false);

    private final Random random = new Random();
    private final TimerUtils timer = new TimerUtils();

    private double currentCPS;
    private double fatigueFactor = 1.0;
    private long lastClickTime = 0;
    private int clickCount = 0;
    private boolean isHolding = false;
    private long holdStartTime = 0;
    private boolean toggleActive = false;

    public AutoclickerModule() {
        super("Left Autoclicker", "Advanced humanized autoclicker", Category.MISCELLANEOUS, ModuleMode.BOTH);

        addSetting(minCPS);
        addSetting(maxCPS);
        addSetting(clickMode);
        addSetting(leftClick);
        addSetting(rightClick);
        addSetting(holdPercentage);
        addSetting(humanization);
        addSetting(jitter);
        addSetting(fatigue);
        addSetting(burstChance);
        addSetting(pauseChance);
        addSetting(blockHit);
        addSetting(blockHitDelay);
        addSetting(onlyWeapon);
        addSetting(onlyEntity);
        addSetting(breakBlocks);
    }

    @Override
    protected void onEnable() {
        currentCPS = getRandomCPS();
        fatigueFactor = 1.0;
        clickCount = 0;
        isHolding = false;
        toggleActive = false;
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

        boolean mouseLeft = Mouse.isButtonDown(0);
        boolean mouseRight = Mouse.isButtonDown(1);

        // Handle toggle mode
        if (clickMode.getValue().equals("Toggle")) {
            if (mouseLeft && !isHolding) {
                toggleActive = !toggleActive;
            }
        }

        // Check if we should click based on mode
        boolean shouldClick = false;
        switch (clickMode.getValue()) {
            case "Hold":
                shouldClick = (leftClick.isEnabled() && mouseLeft) || (rightClick.isEnabled() && mouseRight);
                break;
            case "Toggle":
                shouldClick = toggleActive;
                break;
            case "Always":
                shouldClick = true;
                break;
        }

        if (!shouldClick) {
            resetState();
            return;
        }

        // Check conditions
        if (onlyWeapon.isEnabled() && !isHoldingWeapon()) {
            return;
        }

        if (onlyEntity.isEnabled() && mc.objectMouseOver == null) {
            return;
        }

        // Handle clicks
        if (leftClick.isEnabled()) {
            // Break Blocks Logic: If enabled and looking at block, pause AC to allow mining
            boolean mining = breakBlocks.isEnabled() && mc.objectMouseOver != null &&
                    mc.objectMouseOver.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK &&
                    Mouse.isButtonDown(0);

            if (!mining) {
                handleClick(mc.gameSettings.keyBindAttack, true);
            }
        }

        if (rightClick.isEnabled()) {
            handleClick(mc.gameSettings.keyBindUseItem, false);
        }

        // Apply jitter
        if (jitter.getValue() > 0) {
            applyJitter();
        }

        // Block hit
        if (blockHit.isEnabled() && clickCount % blockHitDelay.getIntValue() == 0) {
            performBlockHit();
        }
    }

    private boolean isHoldingWeapon() {
        if (mc.thePlayer.getHeldItem() == null)
            return false;
        String name = mc.thePlayer.getHeldItem().getDisplayName().toLowerCase();
        return name.contains("sword") || name.contains("axe") || name.contains("bow");
    }

    private void handleClick(KeyBinding keyBinding, boolean isAttack) {
        long currentTime = System.currentTimeMillis();
        long delay = (long) (1000.0 / currentCPS);

        if (humanization.isEnabled()) {
            delay = addHumanVariance(delay);
        }

        if (isHolding) {
            long holdDuration = (long) (delay * holdPercentage.getValue() / 100.0);
            if (currentTime - holdStartTime >= holdDuration) {
                KeyBinding.setKeyBindState(keyBinding.getKeyCode(), false);
                isHolding = false;
            }
        } else if (currentTime - lastClickTime >= delay) {
            KeyBinding.setKeyBindState(keyBinding.getKeyCode(), true);
            if (isAttack) {
                KeyBinding.onTick(keyBinding.getKeyCode());
            }

            isHolding = true;
            holdStartTime = currentTime;
            lastClickTime = currentTime;
            clickCount++;

            updateCPS();
        }
    }

    private void updateCPS() {
        if (fatigue.isEnabled()) {
            if (clickCount > 50) {
                fatigueFactor = Math.max(0.8, fatigueFactor - 0.002);
            }
        }

        // Burst chance
        if (random.nextInt(100) < burstChance.getValue()) {
            fatigueFactor = Math.min(1.15, fatigueFactor + 0.1);
        }

        double baseCPS = getRandomCPS();
        currentCPS = baseCPS * fatigueFactor;

        if (humanization.isEnabled()) {
            double sineOffset = Math.sin(System.currentTimeMillis() / 400.0) * 0.8;
            currentCPS += sineOffset;
        }

        currentCPS = Math.max(minCPS.getValue(), Math.min(maxCPS.getValue(), currentCPS));
    }

    private double getRandomCPS() {
        double min = minCPS.getValue();
        double max = maxCPS.getValue();

        if (humanization.isEnabled()) {
            double gaussian = random.nextGaussian() * 0.2 + 0.5;
            gaussian = Math.max(0, Math.min(1, gaussian));
            return min + (max - min) * gaussian;
        }

        return min + random.nextDouble() * (max - min);
    }

    private long addHumanVariance(long delay) {
        double variance = (random.nextDouble() * 0.3 - 0.15);
        delay = (long) (delay * (1 + variance));

        // Micro-pause
        if (random.nextInt(100) < pauseChance.getValue()) {
            delay += random.nextInt(80) + 30;
        }

        return delay;
    }

    private void applyJitter() {
        if (mc.thePlayer == null)
            return;

        float jitterAmount = jitter.getFloatValue();
        float yawJitter = (random.nextFloat() - 0.5f) * jitterAmount;
        float pitchJitter = (random.nextFloat() - 0.5f) * jitterAmount * 0.5f;

        mc.thePlayer.rotationYaw += yawJitter;
        mc.thePlayer.rotationPitch += pitchJitter;
        mc.thePlayer.rotationPitch = Math.max(-90, Math.min(90, mc.thePlayer.rotationPitch));
    }

    private void performBlockHit() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null)
            return;

        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
    }

    private void resetState() {
        if (isHolding) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            isHolding = false;
        }

        if (System.currentTimeMillis() - lastClickTime > 1500) {
            fatigueFactor = 1.0;
            clickCount = 0;
        }
    }

    @Override
    protected void onDisable() {
        resetState();
        toggleActive = false;
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.1f", currentCPS);
    }
}
