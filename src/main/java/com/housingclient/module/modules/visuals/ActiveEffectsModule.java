package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.util.Collection;

public class ActiveEffectsModule extends Module {

    private static final ResourceLocation inventoryBackground = new ResourceLocation(
            "textures/gui/container/inventory.png");

    public ActiveEffectsModule() {
        super("ActiveEffects", "Display active potion effects on HUD", Category.VISUALS, ModuleMode.BOTH);
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        Collection<PotionEffect> effects = mc.thePlayer.getActivePotionEffects();
        if (effects.isEmpty())
            return;

        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getActiveEffectsX() : 10;
        int y = designer != null ? designer.getActiveEffectsY() : 100;

        int initialY = y;

        for (PotionEffect effect : effects) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(inventoryBackground);

            if (potion.hasStatusIcon()) {
                int index = potion.getStatusIconIndex();
                // Sample 18x18 icon from inventory.png (offset 198px down)
                mc.ingameGUI.drawTexturedModalRect(x, y, 0 + index % 8 * 18, 198 + index / 8 * 18, 18, 18);
            }

            // Draw Name
            String s = I18n.format(potion.getName());
            if (effect.getAmplifier() == 1) {
                s = s + " " + I18n.format("enchantment.level.2");
            } else if (effect.getAmplifier() == 2) {
                s = s + " " + I18n.format("enchantment.level.3");
            } else if (effect.getAmplifier() == 3) {
                s = s + " " + I18n.format("enchantment.level.4");
            }

            mc.fontRendererObj.drawStringWithShadow(s, x + 22, y + 2, 0xFFFFFF);

            // Draw Duration
            String s1 = Potion.getDurationString(effect);
            mc.fontRendererObj.drawStringWithShadow(s1, x + 22, y + 12, 0x7F7F7F);

            y += 24; // Next line
        }

        // Update width/height for designer
        this.width = 100; // Approx
        this.height = y - initialY;
    }

    // Width/Height getters handled by field updates effectively or override
    private int width = 100;
    private int height = 24;

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
