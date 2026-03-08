package com.housingclient.mixin;

import com.housingclient.HousingClient;
import com.housingclient.module.modules.qol.AntiVoidLagModule;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mixin for World class.
 * 
 * 1. Replaces loadedEntityList and playerEntities with CopyOnWriteArrayList
 *    to prevent ConcurrentModificationException when many entities spawn/despawn.
 * 2. Prevents void lag by intercepting skylight calculations.
 */
@Mixin(World.class)
public abstract class MixinWorld {

    private boolean housingclient_listsSwapped = false;

    /**
     * On the first call to updateEntities, swap the entity lists to
     * CopyOnWriteArrayList via reflection (using SRG field names for
     * production compatibility).
     *
     * This prevents ConcurrentModificationException across ALL code that
     * iterates these lists — vanilla, Forge, other mods, and our modules.
     */
    @Inject(method = "func_72939_s", at = @At("HEAD"))
    private void onUpdateEntities(CallbackInfo ci) {
        if (!housingclient_listsSwapped) {
            housingclient_listsSwapped = true;
            try {
                World world = (World) (Object) this;
                // loadedEntityList: SRG = field_72996_f, deobf = loadedEntityList
                swapListField(world, "field_72996_f", "loadedEntityList");
                // playerEntities: SRG = field_73010_i, deobf = playerEntities
                swapListField(world, "field_73010_i", "playerEntities");
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("unchecked")
    private void swapListField(World world, String srgName, String deobfName) {
        Field field = null;
        // Try SRG name first (production)
        try {
            field = World.class.getDeclaredField(srgName);
        } catch (NoSuchFieldException ignored) {}
        // Fallback to deobfuscated name (dev environment)
        if (field == null) {
            try {
                field = World.class.getDeclaredField(deobfName);
            } catch (NoSuchFieldException ignored) {}
        }
        if (field == null) return;

        try {
            field.setAccessible(true);
            Object currentList = field.get(world);
            if (currentList instanceof List && !(currentList instanceof CopyOnWriteArrayList)) {
                // Remove final modifier if present
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                } catch (Exception ignored) {}
                field.set(world, new CopyOnWriteArrayList<>((List<?>) currentList));
            }
        } catch (Exception ignored) {}
    }

    /**
     * Injects at the start of checkLightFor to optionally skip expensive skylight
     * calculations.
     * 
     * Uses SRG name func_180500_c for production compatibility.
     */
    @Inject(method = "func_180500_c", at = @At("HEAD"), cancellable = true)
    private void onCheckLightFor(EnumSkyBlock lightType, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Only intercept skylight checks - block light is less expensive
        if (lightType != EnumSkyBlock.SKY) {
            return;
        }

        // Check if AntiVoidLag module is enabled
        if (HousingClient.getInstance() == null ||
                HousingClient.getInstance().getModuleManager() == null) {
            return;
        }

        AntiVoidLagModule antiVoidLag = HousingClient.getInstance()
                .getModuleManager()
                .getModule(AntiVoidLagModule.class);

        if (antiVoidLag == null || !antiVoidLag.isEnabled()) {
            return;
        }

        // Skip all skylight calculation for void-level blocks (Y < 5)
        // This is where the massive lag occurs
        if (pos.getY() < 5) {
            cir.setReturnValue(false);
            return;
        }

        // Also limit updates per tick using the module's limiter
        if (antiVoidLag.shouldSkipLightUpdate()) {
            cir.setReturnValue(false);
        }
    }
}
