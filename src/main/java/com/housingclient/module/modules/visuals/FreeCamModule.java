package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * FreeCam Module - True detached camera using renderViewEntity.
 * Creates a separate camera entity that we render from.
 * The real player NEVER MOVES - completely frozen.
 *
 * ROTATION ARCHITECTURE:
 * - ALL rotation handling happens in onRenderTick (single source of truth)
 * - We track lastPlayerYaw/Pitch to compute the mouse delta each frame
 * - After consuming the delta, we can set the player's look for interactions
 * without creating a feedback loop because we update lastPlayerYaw to match
 */
public class FreeCamModule extends Module {

    private final NumberSetting speed = new NumberSetting("Speed", "Camera flight speed", 1.0, 0.1, 5.0, 0.1);
    private final BooleanSetting showPlayer = new BooleanSetting("Show Player", "Show your body at real position",
            true);
    private final BooleanSetting allowInteractions = new BooleanSetting("Allow Interactions",
            "Allow interacting with the world from camera position", true);

    // The camera entity (what we render from)
    private EntityOtherPlayerMP cameraEntity;

    // The ghost showing where you really are
    private EntityOtherPlayerMP ghostEntity;

    // Saved state for packet spoofing (the REAL player position/rotation)
    private double savedX, savedY, savedZ;
    private float savedYaw, savedPitch;

    // Original render entity
    private Entity originalRenderEntity;

    // Camera rotation state (tracked independently from player)
    private float cameraYaw, cameraPitch;

    // What we last set the player's rotation to (for computing delta next frame)
    // This is the key to avoiding feedback loops.
    private float lastPlayerYaw, lastPlayerPitch;

    // Saved sneak state so we don't leak sneak to server
    private boolean savedSneaking;

    public FreeCamModule() {
        super("FreeCam", "True detached camera (anticheat safe)", Category.VISUALS, ModuleMode.BOTH);
        addSetting(speed);
        addSetting(showPlayer);
        addSetting(allowInteractions);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            setEnabled(false);
            return;
        }

        // Save the real player state
        savedX = mc.thePlayer.posX;
        savedY = mc.thePlayer.posY;
        savedZ = mc.thePlayer.posZ;
        savedYaw = mc.thePlayer.rotationYaw;
        savedPitch = mc.thePlayer.rotationPitch;

        // Initialize camera rotation
        cameraYaw = savedYaw;
        cameraPitch = savedPitch;

        // Initialize the "last set" tracker to current values
        lastPlayerYaw = savedYaw;
        lastPlayerPitch = savedPitch;

        // Save sneak state so we can freeze it
        savedSneaking = mc.thePlayer.isSneaking();

        // Save original render entity
        originalRenderEntity = mc.getRenderViewEntity();

        // Create camera entity
        cameraEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        cameraEntity.copyLocationAndAnglesFrom(mc.thePlayer);
        cameraEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
        cameraEntity.noClip = true;
        cameraEntity.inventory.copyInventory(mc.thePlayer.inventory);

        mc.theWorld.addEntityToWorld(-1338, cameraEntity);
        mc.setRenderViewEntity(cameraEntity);

        // Create ghost entity
        if (showPlayer.isEnabled()) {
            ghostEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            ghostEntity.copyLocationAndAnglesFrom(mc.thePlayer);
            ghostEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
            ghostEntity.renderYawOffset = mc.thePlayer.renderYawOffset;
            ghostEntity.inventory.copyInventory(mc.thePlayer.inventory);
            mc.theWorld.addEntityToWorld(-1337, ghostEntity);
        }

        lastFrameTime = 0;
    }

    @Override
    protected void onDisable() {
        if (originalRenderEntity != null) {
            mc.setRenderViewEntity(originalRenderEntity);
        } else if (mc.thePlayer != null) {
            mc.setRenderViewEntity(mc.thePlayer);
        }

        if (cameraEntity != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(cameraEntity);
            cameraEntity = null;
        }

        if (ghostEntity != null && mc.theWorld != null) {
            mc.theWorld.removeEntity(ghostEntity);
            ghostEntity = null;
        }
    }

    /**
     * PlayerTickEvent: ONLY freezes the real player position and syncs inventory.
     * No rotation handling here to avoid feedback loops.
     */
    @SubscribeEvent
    public void onUpdate(net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || cameraEntity == null)
            return;
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)
            return;

        // Freeze player position
        mc.thePlayer.setPosition(savedX, savedY, savedZ);
        mc.thePlayer.prevPosX = savedX;
        mc.thePlayer.prevPosY = savedY;
        mc.thePlayer.prevPosZ = savedZ;
        mc.thePlayer.lastTickPosX = savedX;
        mc.thePlayer.lastTickPosY = savedY;
        mc.thePlayer.lastTickPosZ = savedZ;
        mc.thePlayer.motionX = 0;
        mc.thePlayer.motionY = 0;
        mc.thePlayer.motionZ = 0;

        // Freeze sneak state so the server doesn't see sneaking from freecam movement
        mc.thePlayer.setSneaking(savedSneaking);

        // Sync hotbar every tick
        cameraEntity.inventory.currentItem = mc.thePlayer.inventory.currentItem;
        cameraEntity.inventory.copyInventory(mc.thePlayer.inventory);

        if (ghostEntity != null) {
            ghostEntity.inventory.copyInventory(mc.thePlayer.inventory);
        }
    }

    private long lastFrameTime = 0;

    /**
     * RenderTickEvent: handles ALL rotation and movement (single source of truth).
     */
    @SubscribeEvent
    public void onRenderTick(net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent event) {
        if (!isEnabled() || mc.thePlayer == null || cameraEntity == null)
            return;
        if (event.phase != net.minecraftforge.fml.common.gameevent.TickEvent.Phase.START)
            return;

        // --- ROTATION ---
        // Mouse input modifies mc.thePlayer.rotationYaw/Pitch.
        // Compute delta against what we LAST SET the player to (not savedYaw).
        // This is the key: lastPlayerYaw accounts for interaction sync.
        float yawDelta = mc.thePlayer.rotationYaw - lastPlayerYaw;
        float pitchDelta = mc.thePlayer.rotationPitch - lastPlayerPitch;

        // Apply mouse delta to camera
        cameraYaw += yawDelta;
        cameraPitch += pitchDelta;
        cameraPitch = Math.max(-90, Math.min(90, cameraPitch));

        // --- MOVEMENT ---
        long currentTime = System.nanoTime();
        if (lastFrameTime == 0)
            lastFrameTime = currentTime;
        float dt = (currentTime - lastFrameTime) / 1_000_000_000f;
        lastFrameTime = currentTime;
        if (dt > 0.1f)
            dt = 0.1f;

        float baseSpeed = speed.getFloatValue() * 10.0f;
        float moveSpeed = baseSpeed * dt;

        float forward = 0, strafe = 0, vertical = 0;

        if (mc.currentScreen == null) {
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()))
                forward += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()))
                forward -= 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()))
                strafe += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()))
                strafe -= 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()))
                vertical += 1;
            if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()))
                vertical -= 1;
        }

        double moveX = 0, moveZ = 0;
        if (forward != 0 || strafe != 0) {
            float yaw = cameraYaw;
            if (forward != 0 && strafe != 0) {
                yaw += (forward > 0 ? -45 : 45) * (strafe > 0 ? 1 : -1);
                strafe = 0;
            }

            double rad = Math.toRadians(yaw + 90);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);

            if (forward != 0) {
                moveX = forward * moveSpeed * cos;
                moveZ = forward * moveSpeed * sin;
            }
            if (strafe != 0) {
                moveX = strafe * moveSpeed * sin;
                moveZ = -strafe * moveSpeed * cos;
            }
        }

        // Apply camera position
        cameraEntity.setPosition(
                cameraEntity.posX + moveX,
                cameraEntity.posY + vertical * moveSpeed,
                cameraEntity.posZ + moveZ);

        // --- SYNC CAMERA ENTITY ---
        cameraEntity.rotationYaw = cameraYaw;
        cameraEntity.rotationPitch = cameraPitch;
        cameraEntity.prevRotationYaw = cameraYaw;
        cameraEntity.prevRotationPitch = cameraPitch;
        cameraEntity.rotationYawHead = cameraYaw;
        cameraEntity.prevRotationYawHead = cameraYaw;
        // Sync body yaw so first-person hand doesn't slide across the screen
        cameraEntity.renderYawOffset = cameraYaw;
        cameraEntity.prevRenderYawOffset = cameraYaw;

        // Disable position interpolation
        cameraEntity.prevPosX = cameraEntity.posX;
        cameraEntity.prevPosY = cameraEntity.posY;
        cameraEntity.prevPosZ = cameraEntity.posZ;
        cameraEntity.lastTickPosX = cameraEntity.posX;
        cameraEntity.lastTickPosY = cameraEntity.posY;
        cameraEntity.lastTickPosZ = cameraEntity.posZ;

        // FOV sync
        cameraEntity.setSprinting(mc.thePlayer.isSprinting());
        cameraEntity.capabilities.isFlying = mc.thePlayer.capabilities.isFlying;
        try {
            double playerSpeed = mc.thePlayer
                    .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).getAttributeValue();
            cameraEntity.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed)
                    .setBaseValue(playerSpeed);
        } catch (Exception ignored) {
        }

        // --- INTERACTION SYNC (must be LAST) ---
        // Set player's look direction so mc.objectMouseOver traces from camera view.
        // Then update lastPlayerYaw/Pitch so next frame's delta is clean.
        if (allowInteractions.isEnabled()) {
            mc.thePlayer.rotationYaw = cameraYaw;
            mc.thePlayer.rotationPitch = cameraPitch;
            mc.thePlayer.rotationYawHead = cameraYaw;
            mc.thePlayer.prevRotationYaw = cameraYaw;
            mc.thePlayer.prevRotationPitch = cameraPitch;
            // Track what we set, so next frame: delta = newPlayerYaw - cameraYaw = pure
            // mouse input
            lastPlayerYaw = cameraYaw;
            lastPlayerPitch = cameraPitch;
        } else {
            // No interactions: reset to saved position and track that
            mc.thePlayer.rotationYaw = savedYaw;
            mc.thePlayer.rotationPitch = savedPitch;
            mc.thePlayer.prevRotationYaw = savedYaw;
            mc.thePlayer.prevRotationPitch = savedPitch;
            lastPlayerYaw = savedYaw;
            lastPlayerPitch = savedPitch;
        }

        // --- HAND SWAY FIX ---
        // ItemRenderer uses renderArmYaw/renderArmPitch for first-person hand movement.
        // These normally lerp toward rotationYaw in onLivingUpdate, causing the hand
        // to slide when rotationYaw is being reset each frame. Override them to match
        // the camera so the hand stays perfectly still relative to the view.
        mc.thePlayer.renderArmYaw = cameraYaw;
        mc.thePlayer.prevRenderArmYaw = cameraYaw;
        mc.thePlayer.renderArmPitch = cameraPitch;
        mc.thePlayer.prevRenderArmPitch = cameraPitch;
    }

    // Getters for packet handler
    public double getSavedX() {
        return savedX;
    }

    public double getSavedY() {
        return savedY;
    }

    public double getSavedZ() {
        return savedZ;
    }

    public float getSavedYaw() {
        return savedYaw;
    }

    public float getSavedPitch() {
        return savedPitch;
    }

    public boolean isAllowInteractions() {
        return allowInteractions.isEnabled();
    }
}
