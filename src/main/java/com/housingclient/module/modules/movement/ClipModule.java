package com.housingclient.module.modules.movement;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.ChatUtils;
import com.housingclient.utils.PacketUtils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;

public class ClipModule extends Module {
    
    private final NumberSetting upLimit = new NumberSetting("Up Limit", "Maximum clip distance up", 8, 1, 10);
    private final NumberSetting downLimit = new NumberSetting("Down Limit", "Maximum clip distance down", 8, 1, 10);
    private final NumberSetting horizontalLimit = new NumberSetting("Horizontal Limit", "Maximum horizontal clip", 1, 1, 5);
    
    public ClipModule() {
        super("Clip", "Teleport via .clip command", Category.MOVEMENT, ModuleMode.PLAYER);
        
        addSetting(upLimit);
        addSetting(downLimit);
        addSetting(horizontalLimit);
    }
    
    public void executeClip(String direction, String distanceStr) {
        if (mc.thePlayer == null) return;
        
        double distance;
        try {
            distance = Double.parseDouble(distanceStr);
        } catch (NumberFormatException e) {
            ChatUtils.sendClientMessage("§cInvalid distance: " + distanceStr);
            return;
        }
        
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;
        
        switch (direction.toLowerCase()) {
            case "up":
            case "u":
                distance = Math.min(distance, upLimit.getValue());
                y += distance;
                break;
            case "down":
            case "d":
                distance = Math.min(distance, downLimit.getValue());
                y -= distance;
                break;
            case "forward":
            case "f":
                distance = Math.min(distance, horizontalLimit.getValue());
                float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            case "backward":
            case "b":
            case "back":
                distance = Math.min(distance, horizontalLimit.getValue());
                yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                x += MathHelper.sin(yaw) * distance;
                z -= MathHelper.cos(yaw) * distance;
                break;
            case "left":
            case "l":
                distance = Math.min(distance, horizontalLimit.getValue());
                yaw = (mc.thePlayer.rotationYaw - 90) * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            case "right":
            case "r":
                distance = Math.min(distance, horizontalLimit.getValue());
                yaw = (mc.thePlayer.rotationYaw + 90) * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            default:
                ChatUtils.sendClientMessage("§cInvalid direction: " + direction);
                ChatUtils.sendClientMessage("§7Valid: up, down, forward, backward, left, right");
                return;
        }
        
        // Send position packet
        PacketUtils.sendPacket(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
        
        // Update client position
        mc.thePlayer.setPosition(x, y, z);
        
        ChatUtils.sendClientMessage("§aClipped " + direction + " " + String.format("%.1f", distance) + " blocks");
    }
    
    // Quick clip methods for GUI buttons
    public void clipUp(int distance) {
        executeClip("up", String.valueOf(distance));
    }
    
    public void clipDown(int distance) {
        executeClip("down", String.valueOf(distance));
    }
    
    public void clipForward(int distance) {
        executeClip("forward", String.valueOf(distance));
    }
    
    public void clipBackward(int distance) {
        executeClip("backward", String.valueOf(distance));
    }
}

