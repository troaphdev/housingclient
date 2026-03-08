package com.housingclient.module.modules.building;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;

import java.lang.reflect.Field;

/**
 * FastBreak Module - Removes block hit delay for faster mining.
 * The blockHitDelay field controls how many ticks between block damage
 * application.
 * Default in vanilla is 5 ticks. Setting to 0 = instant repeated hits.
 * 
 * Note: This only removes the CLIENT-SIDE delay between break attempts.
 * The server still has its own validation, so this won't let you break
 * faster than the server allows, but it removes unnecessary client lag.
 */
public class FastBreakModule extends Module {

    private final NumberSetting delay = new NumberSetting("Delay", "Ticks between hits (0 = fastest)", 0, 0, 5);

    private Field blockHitDelayField;
    private boolean fieldFound = false;

    public FastBreakModule() {
        super("FastBreak", "Break blocks faster", Category.MISCELLANEOUS, ModuleMode.BOTH);
        addSetting(delay);

        // Find the blockHitDelay field via reflection (cached in constructor)
        try {
            Class<?> controllerClass = net.minecraft.client.multiplayer.PlayerControllerMP.class;

            // Try MCP deobfuscated name first
            try {
                blockHitDelayField = controllerClass.getDeclaredField("blockHitDelay");
                blockHitDelayField.setAccessible(true);
                fieldFound = true;
            } catch (NoSuchFieldException e) {
                // Try SRG obfuscated name (1.8.9 mapping)
                try {
                    blockHitDelayField = controllerClass.getDeclaredField("field_78781_i");
                    blockHitDelayField.setAccessible(true);
                    fieldFound = true;
                } catch (NoSuchFieldException e2) {
                    // Fallback: search int fields
                    for (Field field : controllerClass.getDeclaredFields()) {
                        if (field.getType() == int.class) {
                            field.setAccessible(true);
                            blockHitDelayField = field;
                            fieldFound = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Reflection failed completely
        }
    }

    @Override
    public void onTick() {
        if (!isEnabled())
            return;
        if (mc.thePlayer == null || mc.playerController == null)
            return;

        // Set the block hit delay to our configured value
        if (fieldFound && blockHitDelayField != null) {
            try {
                blockHitDelayField.setInt(mc.playerController, delay.getIntValue());
            } catch (Exception e) {
                // Reflection failed
            }
        }
    }

    @Override
    public String getDisplayInfo() {
        return delay.getIntValue() + "t";
    }
}
