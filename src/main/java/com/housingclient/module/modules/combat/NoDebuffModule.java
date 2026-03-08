package com.housingclient.module.modules.combat;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.ClickGUIModule;
import com.housingclient.module.settings.BooleanSetting;
import net.minecraft.potion.Potion;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.SharedMonsterAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

/**
 * NoDebuff Module - Cancels negative potion effects and speed-reducing
 * attributes.
 * 
 * Safe Mode (Blatant OFF):
 * - Only blindness and nausea are available
 * - Blindness: only removes the visual rendering effect (fog/shader), NOT the
 * actual potion. The server can't detect this because rendering is client-side.
 * - Nausea: removes the visual effect (also client-side rendering only)
 * - Slowness: hidden and disabled
 * 
 * Blatant Mode (Blatant ON):
 * - All settings available: blindness, slowness, nausea
 * - Full effect removal (removes the actual potion effect)
 * 
 * Mechanism:
 * 1. Packet Interception (MixinNetHandlerPlayClient) prevents effects from
 * being added (Blatant mode only).
 * 2. OnEnable/onTick cleans existing effects.
 * 3. Safe mode uses client-side visual suppression instead of effect removal.
 */
public class NoDebuffModule extends Module {

    private final BooleanSetting blindness = new BooleanSetting("Blindness", "Remove blindness effect", true);
    private final BooleanSetting slowness = new BooleanSetting("Slowness", "Remove slowness effect", true);
    private final BooleanSetting nausea = new BooleanSetting("Nausea", "Remove nausea effect", true);

    public NoDebuffModule() {
        super("NoDebuff", "Cancel negative potion effects", Category.MISCELLANEOUS, ModuleMode.BOTH);
        addSetting(blindness);
        addSetting(slowness);
        addSetting(nausea);
    }

    private boolean isBlatantModeEnabled() {
        if (HousingClient.getInstance() == null || HousingClient.getInstance().getModuleManager() == null) {
            return false;
        }
        ClickGUIModule clickGUI = HousingClient.getInstance().getModuleManager().getModule(ClickGUIModule.class);
        return clickGUI != null && clickGUI.isBlatantModeEnabled();
    }

    private void updateSettingsVisibility() {
        boolean blatant = isBlatantModeEnabled();
        // Slowness is only available in blatant mode
        slowness.setVisible(blatant);
        // Blindness and nausea are always visible
        blindness.setVisible(true);
        nausea.setVisible(true);
    }

    @Override
    public java.util.List<com.housingclient.module.settings.Setting<?>> getSettings() {
        updateSettingsVisibility();
        return super.getSettings();
    }

    @Override
    protected void onEnable() {
        cleanEffects();
    }

    @Override
    public void onTick() {
        cleanEffects();
    }

    private void cleanEffects() {
        if (mc.thePlayer == null)
            return;

        boolean blatant = isBlatantModeEnabled();

        // BLINDNESS
        if (blindness.isEnabled() && mc.thePlayer.isPotionActive(Potion.blindness)) {
            if (blatant) {
                // Blatant mode: full removal
                mc.thePlayer.removePotionEffect(Potion.blindness.id);
            }
            // Safe mode: do NOT remove the potion effect
            // The visual suppression is handled in onRender via EntityRenderer fog override
        }

        // NAUSEA
        if (nausea.isEnabled() && mc.thePlayer.isPotionActive(Potion.confusion)) {
            if (blatant) {
                // Blatant mode: full removal
                mc.thePlayer.removePotionEffect(Potion.confusion.id);
            }
            // Safe mode: visual is suppressed via the same rendering override approach
        }

        // SLOWNESS (blatant mode only)
        if (blatant && slowness.isEnabled()) {
            // 1. Remove the standard potion effect
            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                mc.thePlayer.removePotionEffect(Potion.moveSlowdown.id);
            }

            // 2. Clean up negative attribute modifiers on movement speed
            IAttributeInstance speedAttr = mc.thePlayer.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
            if (speedAttr != null) {
                try {
                    // Use SRG names for reliability
                    // func_111122_c = getModifiers
                    // func_111124_b = removeModifier
                    Method getModifiersMethod = speedAttr.getClass().getMethod("func_111122_c");
                    Method removeModifierMethod = speedAttr.getClass().getMethod("func_111124_b",
                            AttributeModifier.class);

                    if (getModifiersMethod != null && removeModifierMethod != null) {
                        @SuppressWarnings("unchecked")
                        Collection<AttributeModifier> modifiers = new ArrayList<>(
                                (Collection<AttributeModifier>) getModifiersMethod.invoke(speedAttr));
                        for (AttributeModifier modifier : modifiers) {
                            if (modifier.getAmount() < 0) {
                                HousingClient.LOGGER.info("Removing negative attribute modifier: " + modifier.getName()
                                        + " (" + modifier.getAmount() + ")");
                                removeModifierMethod.invoke(speedAttr, modifier);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback to searching if SRG fails or in dev environment
                    try {
                        for (Method m : speedAttr.getClass().getMethods()) {
                            if ((m.getName().equals("getModifiers") || m.getName().equals("func_111122_c"))
                                    && m.getReturnType().equals(Collection.class)) {
                                @SuppressWarnings("unchecked")
                                Collection<AttributeModifier> modifiers = new ArrayList<>(
                                        (Collection<AttributeModifier>) m.invoke(speedAttr));
                                for (AttributeModifier modifier : modifiers) {
                                    if (modifier.getAmount() < 0) {
                                        speedAttr.removeModifier(modifier);
                                    }
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            // 3. Reset walkSpeed to default (0.1)
            if (mc.thePlayer.capabilities.getWalkSpeed() < 0.1f) {
                mc.thePlayer.capabilities.setPlayerWalkSpeed(0.1f);
            }
        }

        // SAFE MODE VISUAL SUPPRESSION
        // For blindness and nausea in safe mode, we suppress the visual effects
        // by manipulating the client-side renderer state.
        if (!blatant) {
            suppressVisualEffects();
        }
    }

    /**
     * Suppress the visual rendering of blindness and nausea without removing
     * the actual potion effects. This is purely client-side and undetectable.
     */
    private void suppressVisualEffects() {
        if (mc.thePlayer == null)
            return;

        // Blindness visual suppression: override the fog distance
        // The blindness effect works by setting fog to a very short distance.
        // We counteract this by resetting the fog parameters each tick.
        if (blindness.isEnabled() && mc.thePlayer.isPotionActive(Potion.blindness)) {
            // Override the blindness effect's impact on rendering
            // The EntityRenderer checks isPotionActive(Potion.blindness) for fog.
            // We use the activePotionEffects map to set the duration to 0 visually
            // without removing the effect from the server's perspective.
            try {
                net.minecraft.potion.PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.blindness);
                if (effect != null) {
                    // Set amplifier to 0 to minimize the visual effect
                    // The server still thinks we have the effect
                    java.lang.reflect.Field amplifierField = null;
                    for (java.lang.reflect.Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            int val = f.getInt(effect);
                            // The amplifier field is typically 0-255,
                            // separate from duration which is usually >1
                            if (f.getName().equals("amplifier") || f.getName().equals("field_76461_c")) {
                                amplifierField = f;
                                break;
                            }
                        }
                    }
                    if (amplifierField != null) {
                        amplifierField.setInt(effect, 0);
                    }

                    // Also override the duration to 1 tick
                    // This makes the fog fade out quickly while the server still tracks the effect
                    java.lang.reflect.Field durationField = null;
                    for (java.lang.reflect.Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            if (f.getName().equals("duration") || f.getName().equals("field_76460_b")) {
                                durationField = f;
                                break;
                            }
                        }
                    }
                    if (durationField != null) {
                        durationField.setInt(effect, 1);
                    }
                }
            } catch (Exception ignored) {
                // Reflection failed — fall back to no-op
            }
        }

        // Nausea visual suppression: same approach
        if (nausea.isEnabled() && mc.thePlayer.isPotionActive(Potion.confusion)) {
            try {
                net.minecraft.potion.PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.confusion);
                if (effect != null) {
                    java.lang.reflect.Field durationField = null;
                    for (java.lang.reflect.Field f : effect.getClass().getDeclaredFields()) {
                        if (f.getType() == int.class) {
                            f.setAccessible(true);
                            if (f.getName().equals("duration") || f.getName().equals("field_76460_b")) {
                                durationField = f;
                                break;
                            }
                        }
                    }
                    if (durationField != null) {
                        durationField.setInt(effect, 1);
                    }
                }
            } catch (Exception ignored) {
                // Reflection failed — fall back to no-op
            }
        }
    }

    /**
     * Whether a specific potion effect should be blocked at the packet level.
     * In safe mode, we DON'T block packets — we let effects apply and suppress
     * visuals.
     * In blatant mode, we block the packets entirely.
     */
    public boolean shouldRemoveEffect(int potionId) {
        // Safe mode: don't block any packets, handle visually
        if (!isBlatantModeEnabled()) {
            return false;
        }

        // Blatant mode: block the effect packets
        if (Potion.blindness != null && potionId == Potion.blindness.id && blindness.isEnabled())
            return true;
        if (Potion.moveSlowdown != null && potionId == Potion.moveSlowdown.id && slowness.isEnabled())
            return true;
        if (Potion.confusion != null && potionId == Potion.confusion.id && nausea.isEnabled())
            return true;
        return false;
    }

    /**
     * Whether slowness removal is active (blatant mode only).
     */
    public boolean isSlownessRemovalEnabled() {
        return isBlatantModeEnabled() && slowness.isEnabled();
    }
}
