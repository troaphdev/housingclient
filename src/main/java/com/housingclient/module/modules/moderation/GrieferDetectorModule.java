package com.housingclient.module.modules.moderation;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GrieferDetectorModule extends Module {

    private final BooleanSetting blockDestruction = new BooleanSetting("Block Destruction",
            "Detect rapid block breaking", true);
    private final NumberSetting threshold = new NumberSetting("Threshold", "Blocks per second to trigger alert", 10, 1,
            50, 1);

    // New item detectors
    private final BooleanSetting bonemealDetector = new BooleanSetting("Bonemeal Detector",
            "Detects when anyone holds bonemeal", true);
    private final BooleanSetting dispenserDetector = new BooleanSetting("Dispenser Detector",
            "Detects when anyone holds a dispenser", true);
    private final BooleanSetting stickyPistonDetector = new BooleanSetting("Sticky Piston Detector",
            "Detects when anyone holds a sticky piston", true);
    private final BooleanSetting pistonDetector = new BooleanSetting("Piston Detector",
            "Detects when anyone holds a piston", true);
    private final BooleanSetting slimeDetector = new BooleanSetting("Slime Detector", "Detects when anyone holds slime",
            true);
    private final BooleanSetting flintAndSteelDetector = new BooleanSetting("Flint and Steel",
            "Detects when anyone holds flint and steel", true);
    private final BooleanSetting fireChargeDetector = new BooleanSetting("Fire Charge",
            "Detects when anyone holds a fire charge", true);
    private final BooleanSetting saplingsDetector = new BooleanSetting("Saplings", "Detects when anyone holds saplings",
            true);
    private final BooleanSetting jukeboxDetector = new BooleanSetting("Jukebox", "Detects when anyone holds a jukebox",
            true);
    private final BooleanSetting noteBlockDetector = new BooleanSetting("Note Block",
            "Detects when anyone holds a note block", true);
    private final BooleanSetting musicDiscDetector = new BooleanSetting("Music Disc",
            "Detects when anyone holds a music disc", true);
    private final BooleanSetting splashPotionsDetector = new BooleanSetting("Splash Potions",
            "Detects when anyone holds splash potions", true);
    private final BooleanSetting boatDetector = new BooleanSetting("Boat", "Detects when anyone holds a boat", true);
    private final BooleanSetting spawnEggsDetector = new BooleanSetting("Spawn Eggs",
            "Detects when anyone holds spawn eggs", true);
    private final BooleanSetting fireworksRocketDetector = new BooleanSetting("Fireworks Rocket",
            "Detects when anyone holds a fireworks rocket", true);
    private final BooleanSetting waterBucketDetector = new BooleanSetting("Water Bucket",
            "Detects when anyone holds a water bucket", true);
    private final BooleanSetting lavaBucketDetector = new BooleanSetting("Lava Bucket",
            "Detects when anyone holds a lava bucket", true);
    private final BooleanSetting barrierDetector = new BooleanSetting("Barrier",
            "Detects when anyone holds a barrier block", true);

    private final Map<UUID, Integer> breakCounts = new HashMap<>();

    // Set to keep track of items we've already alerted for this player recently
    // Prevents spamming chat every tick while they hold the item.
    // Clears when they stop holding it.
    private final Map<UUID, Set<String>> alertedItems = new HashMap<>();

    private int ticks = 0;

    public GrieferDetectorModule() {
        super("Griefer Detector", "Alerts when a player flags griefer checks", Category.MODERATION,
                com.housingclient.module.ModuleMode.BOTH);
        addSetting(blockDestruction);
        addSetting(threshold);
        addSetting(bonemealDetector);
        addSetting(dispenserDetector);
        addSetting(stickyPistonDetector);
        addSetting(pistonDetector);
        addSetting(slimeDetector);
        addSetting(flintAndSteelDetector);
        addSetting(fireChargeDetector);
        addSetting(saplingsDetector);
        addSetting(jukeboxDetector);
        addSetting(noteBlockDetector);
        addSetting(musicDiscDetector);
        addSetting(splashPotionsDetector);
        addSetting(boatDetector);
        addSetting(spawnEggsDetector);
        addSetting(fireworksRocketDetector);
        addSetting(waterBucketDetector);
        addSetting(lavaBucketDetector);
        addSetting(barrierDetector);

        // Disable threshold visibility when block destruction is off
        threshold.setVisibility(() -> blockDestruction.isEnabled());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        breakCounts.clear();
        alertedItems.clear();
        ticks = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null)
            return;

        ticks++;

        // Item checking logic (runs frequently)
        if (ticks % 5 == 0) {
            checkPlayerItems();
        }

        // Block destruction checking logic (runs every second)
        if (ticks >= 20) {
            if (blockDestruction.isEnabled()) {
                checkThresholds();
            }
            breakCounts.clear();
            ticks = 0;
        }
    }

    private void checkPlayerItems() {
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == mc.thePlayer)
                continue; // Skip self

            ItemStack heldItem = player.getHeldItem();
            UUID uuid = player.getUniqueID();

            // Get or create alerted set for this player
            Set<String> currentlyHolding = new HashSet<>();
            Set<String> alerted = alertedItems.computeIfAbsent(uuid, k -> new HashSet<>());

            if (heldItem != null) {
                Item item = heldItem.getItem();
                String itemId = Item.itemRegistry.getNameForObject(item).toString(); // e.g., "minecraft:dye"
                int meta = heldItem.getMetadata();
                String displayName = heldItem.getDisplayName();

                String flagName = null;
                boolean isSevere = false;

                // Bonemeal (Dye with meta 15)
                if (bonemealDetector.isEnabled() && itemId.equals("minecraft:dye") && meta == 15) {
                    flagName = "Bonemeal";
                }
                // Dispenser
                else if (dispenserDetector.isEnabled() && itemId.equals("minecraft:dispenser")) {
                    flagName = "Dispenser";
                }
                // Sticky Piston
                else if (stickyPistonDetector.isEnabled() && itemId.equals("minecraft:sticky_piston")) {
                    flagName = "Sticky Piston";
                }
                // Piston
                else if (pistonDetector.isEnabled() && itemId.equals("minecraft:piston")) {
                    flagName = "Piston";
                }
                // Slime (Block or Ball)
                else if (slimeDetector.isEnabled()
                        && (itemId.equals("minecraft:slime") || itemId.equals("minecraft:slime_ball"))) {
                    flagName = "Slime";
                }
                // Flint and Steel
                else if (flintAndSteelDetector.isEnabled() && itemId.equals("minecraft:flint_and_steel")) {
                    flagName = "Flint and Steel";
                }
                // Fire Charge
                else if (fireChargeDetector.isEnabled() && itemId.equals("minecraft:fire_charge")) {
                    flagName = "Fire Charge";
                }
                // Saplings
                else if (saplingsDetector.isEnabled() && itemId.equals("minecraft:sapling")) {
                    flagName = displayName; // specific sapling
                }
                // Jukebox
                else if (jukeboxDetector.isEnabled() && itemId.equals("minecraft:jukebox")) {
                    flagName = "Jukebox";
                }
                // Note Block
                else if (noteBlockDetector.isEnabled() && itemId.equals("minecraft:noteblock")) {
                    flagName = "Note Block";
                }
                // Music Disc (Any record)
                else if (musicDiscDetector.isEnabled() && itemId.startsWith("minecraft:record_")) {
                    flagName = displayName; // specific disc
                }
                // Splash Potions (Potion with meta >= 16384)
                else if (splashPotionsDetector.isEnabled() && itemId.equals("minecraft:potion")
                        && net.minecraft.item.ItemPotion.isSplash(meta)) {
                    flagName = "Splash " + displayName; // specific potion
                }
                // Boat
                else if (boatDetector.isEnabled() && itemId.equals("minecraft:boat")) {
                    flagName = "Boat";
                }
                // Spawn Eggs
                else if (spawnEggsDetector.isEnabled() && itemId.equals("minecraft:spawn_egg")) {
                    flagName = displayName; // specific egg
                    if (meta == 56) { // Ghast entity ID is 56
                        isSevere = true;
                    }
                }
                // Fireworks Rocket
                else if (fireworksRocketDetector.isEnabled() && itemId.equals("minecraft:fireworks")) {
                    flagName = "Fireworks Rocket";
                }
                // Water Bucket
                else if (waterBucketDetector.isEnabled() && itemId.equals("minecraft:water_bucket")) {
                    flagName = "Water Bucket";
                }
                // Lava Bucket
                else if (lavaBucketDetector.isEnabled() && itemId.equals("minecraft:lava_bucket")) {
                    flagName = "Lava Bucket";
                }
                // Barrier
                else if (barrierDetector.isEnabled() && itemId.equals("minecraft:barrier")) {
                    flagName = "Barrier";
                }

                if (flagName != null) {
                    currentlyHolding.add(flagName);
                    // Only alert if we haven't already alerted for this specific item type recently
                    if (!alerted.contains(flagName)) {
                        alert(player.getName(), flagName, isSevere);
                        alerted.add(flagName);
                    }
                }
            }

            // Retain only items they are currently holding to re-trigger if they switch
            // away and back
            alerted.retainAll(currentlyHolding);
        }
    }

    private void alert(String name, String type, boolean severe) {
        String color = severe ? "\u00A7c" : "\u00A7f"; // Light red if severe, white otherwise for the item type
        ChatUtils.sendClientMessage(
                "\u00A7c[GrieferDetector] \u00A7f" + name + " is flagging [" + color + type + "\u00A7f]");
    }

    public void onPacketReceive(net.minecraft.network.Packet<?> packet) {
        if (!blockDestruction.isEnabled())
            return;

        if (packet instanceof S23PacketBlockChange) {
            S23PacketBlockChange p = (S23PacketBlockChange) packet;
            // Access blockState field via reflection safely
            try {
                java.lang.reflect.Field f = S23PacketBlockChange.class.getDeclaredField("field_148883_d");
                f.setAccessible(true);
                IBlockState state = (IBlockState) f.get(p);
                if (state.getBlock() == net.minecraft.init.Blocks.air) {
                    processBlockBreak(p.getBlockPosition());
                }
            } catch (Exception e) {
                // Try non-obfuscated name
                try {
                    java.lang.reflect.Field f = S23PacketBlockChange.class.getDeclaredField("blockState");
                    f.setAccessible(true);
                    IBlockState state = (IBlockState) f.get(p);
                    if (state.getBlock() == net.minecraft.init.Blocks.air) {
                        processBlockBreak(p.getBlockPosition());
                    }
                } catch (Exception e2) {
                    // Ignore
                }
            }
        } else if (packet instanceof S22PacketMultiBlockChange) {
            S22PacketMultiBlockChange p = (S22PacketMultiBlockChange) packet;
            for (S22PacketMultiBlockChange.BlockUpdateData data : p.getChangedBlocks()) {
                if (data.getBlockState().getBlock() == net.minecraft.init.Blocks.air) {
                    processBlockBreak(data.getPos());
                }
            }
        }
    }

    private void processBlockBreak(BlockPos pos) {
        double range = 6.0;
        EntityPlayer suspect = null;
        double closestDistSq = range * range;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityPlayer && entity != mc.thePlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                double distSq = player.getDistanceSq(pos);

                if (distSq <= range * range) {
                    if (isLookingAt(player, pos)) {
                        if (distSq < closestDistSq) {
                            closestDistSq = distSq;
                            suspect = player;
                        }
                    }
                }
            }
        }

        if (suspect != null) {
            UUID id = suspect.getUniqueID();
            breakCounts.put(id, breakCounts.getOrDefault(id, 0) + 1);
        }
    }

    private boolean isLookingAt(EntityPlayer player, BlockPos target) {
        Vec3 eyePos = player.getPositionEyes(1.0f);
        Vec3 lookVec = player.getLook(1.0f);

        Vec3 targetVec = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        Vec3 diff = targetVec.subtract(eyePos);
        diff = diff.normalize();

        double dot = lookVec.dotProduct(diff);
        return dot > 0.95;
    }

    private void checkThresholds() {
        int limit = threshold.getIntValue();
        for (Map.Entry<UUID, Integer> entry : breakCounts.entrySet()) {
            if (entry.getValue() > limit) {
                EntityPlayer player = mc.theWorld.getPlayerEntityByUUID(entry.getKey());
                String name = (player != null) ? player.getName() : "Unknown";
                alert(name, "Block Destruction", false);
            }
        }
    }
}
