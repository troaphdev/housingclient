package com.housingclient.module.modules.client;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.*;
import java.util.HashSet;
import java.util.Set;

public class FriendsModule extends Module {

    private static final Set<String> friends = new HashSet<String>();

    private final BooleanSetting middleClickAdd = new BooleanSetting("Middle Click",
            "Middle click to add/remove friends", true);

    private boolean wasMiddleDown = false;

    public FriendsModule() {
        super("Friends", "Middle click players to add as friends", Category.CLIENT, ModuleMode.BOTH);

        addSetting(middleClickAdd);

        setEnabled(true); // Always on by default
        setVisible(false); // Valid for HUD
    }

    @Override
    public void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null)
            return;
        if (mc.currentScreen != null)
            return;

        if (middleClickAdd.isEnabled()) {
            boolean middleDown = Mouse.isButtonDown(2);

            if (middleDown && !wasMiddleDown) {
                // Middle click pressed
                MovingObjectPosition mop = mc.objectMouseOver;
                if (mop != null && mop.entityHit != null && mop.entityHit instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) mop.entityHit;
                    String name = player.getName();

                    if (isFriend(name)) {
                        removeFriend(name);
                        ChatUtils.sendClientMessage("\u00A7cRemoved friend: \u00A7f" + name);
                    } else {
                        addFriend(name);
                        ChatUtils.sendClientMessage("\u00A7aFriend added: \u00A7f" + name);
                    }
                }
            }

            wasMiddleDown = middleDown;
        }
    }

    public static boolean isFriend(String name) {
        return friends.contains(name.toLowerCase());
    }

    public static void addFriend(String name) {
        friends.add(name.toLowerCase());
        saveFriends();
    }

    public static void removeFriend(String name) {
        friends.remove(name.toLowerCase());
        saveFriends();
    }

    public static Set<String> getFriends() {
        return friends;
    }

    public static void clearFriends() {
        friends.clear();
        saveFriends();
    }

    private static void saveFriends() {
        try {
            File friendsFile = new File(HousingClient.getInstance().getDataDir(), "friends.txt");
            PrintWriter writer = new PrintWriter(friendsFile);
            for (String friend : friends) {
                writer.println(friend);
            }
            writer.close();
        } catch (Exception e) {
            HousingClient.LOGGER.error("Failed to save friends", e);
        }
    }

    public static void loadFriends() {
        try {
            File friendsFile = new File(HousingClient.getInstance().getDataDir(), "friends.txt");
            if (friendsFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(friendsFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        friends.add(line.trim().toLowerCase());
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
            HousingClient.LOGGER.error("Failed to load friends", e);
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(friends.size());
    }

    @Override
    public boolean isVisible() {
        return false;
    }
}
