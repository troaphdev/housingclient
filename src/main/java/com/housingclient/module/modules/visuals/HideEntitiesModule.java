package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.BooleanSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.init.Blocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hide Entities Module
 * Hides specific entity types by cancelling their rendering via
 * MixinRenderManager.
 * Uses a pre-computed set of entity IDs updated each tick so that the
 * render-time check is a simple Set.contains() with no risk of
 * ConcurrentModificationException.
 */
public class HideEntitiesModule extends Module {

    private final BooleanSetting hideBoats = new BooleanSetting("Hide Boats", "Hide boats", true);
    private final BooleanSetting hideFallingSand = new BooleanSetting("Hide Falling Sand", "Hide falling sand", true);
    private final BooleanSetting hideFallingGravel = new BooleanSetting("Hide Falling Gravel", "Hide falling gravel",
            true);
    private final BooleanSetting hideFallingAnvil = new BooleanSetting("Hide Falling Anvil", "Hide falling anvils",
            true);
    private final BooleanSetting hideMinecarts = new BooleanSetting("Hide Minecarts", "Hide minecarts", false);
    private final BooleanSetting hideTNT = new BooleanSetting("Hide TNT", "Hide primed TNT", false);
    private final BooleanSetting hideFireworks = new BooleanSetting("Hide Fireworks", "Hide fireworks", false);
    private final BooleanSetting hideDragonEggs = new BooleanSetting("Hide Dragon Eggs", "Hide dragon egg entities",
            false);

    /** Pre-computed set of entity IDs to hide — swapped atomically each tick */
    private volatile Set<Integer> hiddenEntityIds = Collections.emptySet();
    private int hiddenCount = 0;

    public HideEntitiesModule() {
        super("Hide Entities", "Hides lag-causing entities", Category.VISUALS, ModuleMode.BOTH);

        addSetting(hideBoats);
        addSetting(hideFallingSand);
        addSetting(hideFallingGravel);
        addSetting(hideFallingAnvil);
        addSetting(hideMinecarts);
        addSetting(hideTNT);
        addSetting(hideFireworks);
        addSetting(hideDragonEggs);
    }

    @Override
    protected void onEnable() {
        hiddenCount = 0;
        hiddenEntityIds = Collections.emptySet();
    }

    @Override
    protected void onDisable() {
        hiddenCount = 0;
        hiddenEntityIds = Collections.emptySet();
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null) {
            hiddenCount = 0;
            hiddenEntityIds = Collections.emptySet();
            return;
        }

        // Build a fresh set of entity IDs to hide.
        // Wrapped in try-catch to guard against any CME from the entity list.
        Set<Integer> newIds = new HashSet<>();
        try {
            List<Entity> entities = mc.theWorld.loadedEntityList;
            for (int i = 0, size = entities.size(); i < size; i++) {
                try {
                    Entity entity = entities.get(i);
                    if (entity != null && shouldHideEntity(entity)) {
                        newIds.add(entity.getEntityId());
                    }
                } catch (IndexOutOfBoundsException ignored) {
                    break; // list shrank while iterating
                }
            }
        } catch (Exception ignored) {
            // Fallback: keep whatever we collected so far
        }

        // Atomic swap — the render thread only ever reads the reference
        hiddenEntityIds = newIds;
        hiddenCount = newIds.size();
    }

    /**
     * Fast render-time check used by MixinRenderManager.
     * Just a HashSet.contains() — O(1), no entity field access, no CME risk.
     */
    public boolean isEntityHidden(int entityId) {
        return hiddenEntityIds.contains(entityId);
    }

    /**
     * Check if an entity should be hidden based on its type and current settings.
     * Called only from onTick (game thread), never from the render loop.
     */
    public boolean shouldHideEntity(Entity entity) {
        if (entity == null)
            return false;

        if (hideBoats.isEnabled() && entity instanceof EntityBoat) {
            return true;
        }

        if (entity instanceof EntityFallingBlock) {
            EntityFallingBlock fallingBlock = (EntityFallingBlock) entity;
            if (fallingBlock.getBlock() != null) {
                if (hideFallingSand.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.sand) {
                    return true;
                }
                if (hideFallingGravel.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.gravel) {
                    return true;
                }
                if (hideFallingAnvil.isEnabled() && fallingBlock.getBlock().getBlock() == Blocks.anvil) {
                    return true;
                }
            }
        }

        if (hideMinecarts.isEnabled() && entity instanceof EntityMinecart) {
            return true;
        }

        if (hideTNT.isEnabled() && entity instanceof EntityTNTPrimed) {
            return true;
        }

        if (hideFireworks.isEnabled() && entity instanceof EntityFireworkRocket) {
            return true;
        }

        // Dragon eggs as falling blocks
        if (hideDragonEggs.isEnabled() && entity instanceof EntityFallingBlock) {
            EntityFallingBlock fb = (EntityFallingBlock) entity;
            if (fb.getBlock() != null && fb.getBlock().getBlock() == Blocks.dragon_egg) {
                return true;
            }
        }

        return false;
    }

    public int getHiddenCount() {
        return hiddenCount;
    }

    @Override
    public void onRender() {
        if (mc.thePlayer == null)
            return;

        String text = "Entities Hidden: " + hiddenCount;

        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        int x = designer != null ? designer.getHiddenEntitiesX() : 5;
        int y = designer != null ? designer.getHiddenEntitiesY() : 408;

        mc.fontRendererObj.drawStringWithShadow(text, x, y, 0xFFAAAAAA);

        setWidth(mc.fontRendererObj.getStringWidth(text));
        setHeight(mc.fontRendererObj.FONT_HEIGHT);
    }

    @Override
    public String getDisplayInfo() {
        return hiddenCount > 0 ? String.valueOf(hiddenCount) : null;
    }
}
