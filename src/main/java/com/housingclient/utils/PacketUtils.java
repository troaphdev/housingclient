package com.housingclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.*;

import java.util.Random;

public class PacketUtils {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    
    private static long lastPacketTime = 0;
    private static int packetCount = 0;
    
    // Packet throttling settings
    private static int maxPacketsPerSecond = 10;
    private static long minPacketDelay = 50; // ms
    
    public static void sendPacket(Packet<?> packet) {
        if (mc.thePlayer != null && mc.getNetHandler() != null) {
            mc.getNetHandler().getNetworkManager().sendPacket(packet);
        }
    }
    
    public static void sendPacketThrottled(Packet<?> packet) {
        long currentTime = System.currentTimeMillis();
        
        // Reset counter every second
        if (currentTime - lastPacketTime > 1000) {
            packetCount = 0;
            lastPacketTime = currentTime;
        }
        
        // Check if we've exceeded packet limit
        if (packetCount >= maxPacketsPerSecond) {
            return;
        }
        
        sendPacket(packet);
        packetCount++;
    }
    
    public static void sendPacketWithDelay(Packet<?> packet, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay);
                sendPacket(packet);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public static void sendPacketWithRandomDelay(Packet<?> packet, long minDelay, long maxDelay) {
        long delay = minDelay + random.nextLong() % (maxDelay - minDelay);
        sendPacketWithDelay(packet, delay);
    }
    
    public static C03PacketPlayer.C04PacketPlayerPosition createPositionPacket(double x, double y, double z, boolean onGround) {
        return new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, onGround);
    }
    
    public static C03PacketPlayer.C05PacketPlayerLook createLookPacket(float yaw, float pitch, boolean onGround) {
        return new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround);
    }
    
    public static C03PacketPlayer.C06PacketPlayerPosLook createPosLookPacket(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        return new C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround);
    }
    
    public static C03PacketPlayer createPlayerPacket(boolean onGround) {
        return new C03PacketPlayer(onGround);
    }
    
    public static void setMaxPacketsPerSecond(int max) {
        maxPacketsPerSecond = Math.max(1, Math.min(50, max));
    }
    
    public static void setMinPacketDelay(long delay) {
        minPacketDelay = Math.max(0, Math.min(500, delay));
    }
    
    public static int getPacketCount() {
        return packetCount;
    }
    
    public static void addHumanizedDelay() {
        // Add slight random delay to simulate human input
        try {
            Thread.sleep(random.nextInt(20) + 5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static double addJitter(double value, double maxJitter) {
        return value + (random.nextDouble() * 2 - 1) * maxJitter;
    }
    
    public static float addJitter(float value, float maxJitter) {
        return value + (random.nextFloat() * 2 - 1) * maxJitter;
    }
}

