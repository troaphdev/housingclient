package com.housingclient.command;

import com.housingclient.HousingClient;
import com.housingclient.module.Module;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.module.settings.Setting;
import com.housingclient.utils.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatCommandHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final String PREFIX = ".";
    // Fullwidth period used by FancyText Module
    private static final String FULLWIDTH_PREFIX = "\uFF0E";

    private static final List<String> COMMANDS = Arrays.asList(
            "clip", "noclip", "fly", "speed", "help", "toggle", "bind", "friend", "list", "freecam", "sprint");

    /**
     * Checks if the message starts with the command prefix (normal or fullwidth).
     */
    public static boolean isCommand(String message) {
        if (message == null || message.isEmpty())
            return false;
        return message.startsWith(PREFIX) || message.startsWith(FULLWIDTH_PREFIX);
    }

    /**
     * Returns the message reverted to normal text if it was fancy.
     */
    public static String getCleanCommand(String message) {
        if (message == null)
            return null;
        return com.housingclient.module.modules.visuals.FancyTextModule.revertToNormal(message);
    }

    public static boolean handleChatMessage(String rawMessage) {
        if (!isCommand(rawMessage)) {
            return false;
        }

        // Must clean the string before processing so commands work when Fancy Text is
        // ON
        String message = getCleanCommand(rawMessage);

        // Delegate to dynamic CommandManager first
        if (HousingClient.getInstance().getCommandManager().handleCommand(message)) {
            mc.ingameGUI.getChatGUI().addToSentMessages(rawMessage); // Add raw (fancy) to sent history
            return true;
        }

        // Add to chat history (up arrow) - raw version so it matches what user typed
        mc.ingameGUI.getChatGUI().addToSentMessages(rawMessage);

        String[] parts = message.substring(PREFIX.length()).split(" ");
        if (parts.length == 0 || parts[0].isEmpty())
            return false;

        String command = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        switch (command) {
            case "help":
            case "h":
                showHelp();
                return true;

            case "clip":
            case "c":
                handleClip(args);
                return true;

            case "noclip":
                handleNoClip(args);
                return true;

            case "toggle":
            case "t":
                handleToggle(args);
                return true;

            case "fly":
                handleFly(args);
                return true;

            case "speed":
                handleSpeed(args);
                return true;

            case "bind":
                handleBind(args);
                return true;

            case "friend":
                handleFriend(args);
                return true;

            case "list":
                listModules();
                return true;

            case "freecam":
            case "fc":
                handleFreeCam(args);
                return true;

            case "sprint":
                handleSprint(args);
                return true;

            default:
                // Try to find a module with this name
                Module module = HousingClient.getInstance().getModuleManager().getModuleByName(command);
                if (module != null) {
                    module.toggle();
                    String status = module.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
                    ChatUtils.sendClientMessage(status + " \u00A7f" + module.getName());
                    return true;
                }
                ChatUtils.sendClientMessage("\u00A7cUnknown command: " + command);
                ChatUtils.sendClientMessage("\u00A77Use .help for commands");
                return true;
        }
    }

    private static void showHelp() {
        ChatUtils.sendClientMessage("\u00A7b\u00A7l=== HousingClient Commands ===");
        ChatUtils.sendClientMessage("\u00A73.help \u00A77- Show this help");
        ChatUtils.sendClientMessage("\u00A73.clip <dir> <dist> \u00A77- Teleport");
        ChatUtils.sendClientMessage("\u00A77  Directions: up, down, forward, back, left, right");
        ChatUtils.sendClientMessage("\u00A73.noclip \u00A77- Toggle noclip mode");
        ChatUtils.sendClientMessage("\u00A73.toggle <module> \u00A77- Toggle module");
        ChatUtils.sendClientMessage("\u00A73.<module> \u00A77- Quick toggle");
        ChatUtils.sendClientMessage("\u00A73.fly [speed] \u00A77- Toggle fly with optional speed");
        ChatUtils.sendClientMessage("\u00A73.speed [value] \u00A77- Toggle speed with optional value");
        ChatUtils.sendClientMessage("\u00A73.freecam [speed] \u00A77- Toggle freecam");
        ChatUtils.sendClientMessage("\u00A73.sprint \u00A77- Toggle sprint");
        ChatUtils.sendClientMessage("\u00A73.bind <module> <key> \u00A77- Bind key");
        ChatUtils.sendClientMessage("\u00A73.friend add/del/list <name> \u00A77- Manage friends");
        ChatUtils.sendClientMessage("\u00A73.profile <load/save/list> [name] \u00A77- Manage profiles");
        ChatUtils.sendClientMessage("\u00A73.safe \u00A77- Toggle safe mode");
        ChatUtils.sendClientMessage("\u00A73.list \u00A77- List all modules");
    }

    private static void handleClip(String[] args) {
        if (mc.thePlayer == null)
            return;

        if (args.length < 2) {
            ChatUtils.sendClientMessage("\u00A7cUsage: .clip <direction> <distance>");
            ChatUtils.sendClientMessage("\u00A77Directions: up, down, forward, back, left, right");
            return;
        }

        double distance;
        try {
            distance = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            ChatUtils.sendClientMessage("\u00A7cInvalid distance: " + args[1]);
            return;
        }

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        String dir = args[0].toLowerCase();

        switch (dir) {
            case "up":
            case "u":
                y += distance;
                break;
            case "down":
            case "d":
                y -= distance;
                break;
            case "forward":
            case "f":
                float yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            case "back":
            case "backward":
            case "b":
                yaw = mc.thePlayer.rotationYaw * (float) Math.PI / 180f;
                x += MathHelper.sin(yaw) * distance;
                z -= MathHelper.cos(yaw) * distance;
                break;
            case "left":
            case "l":
                yaw = (mc.thePlayer.rotationYaw - 90) * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            case "right":
            case "r":
                yaw = (mc.thePlayer.rotationYaw + 90) * (float) Math.PI / 180f;
                x -= MathHelper.sin(yaw) * distance;
                z += MathHelper.cos(yaw) * distance;
                break;
            default:
                ChatUtils.sendClientMessage("\u00A7cInvalid direction: " + dir);
                return;
        }

        // Teleport
        mc.thePlayer.setPosition(x, y, z);
        mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));

        ChatUtils.sendClientMessage("\u00A7aClipped " + dir + " " + String.format("%.1f", distance) + " blocks");
    }

    private static void handleNoClip(String[] args) {
        if (mc.thePlayer == null)
            return;

        mc.thePlayer.noClip = !mc.thePlayer.noClip;
        String status = mc.thePlayer.noClip ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7fNoClip");
    }

    private static void handleToggle(String[] args) {
        if (args.length < 1) {
            ChatUtils.sendClientMessage("\u00A7cUsage: .toggle <module>");
            return;
        }

        String moduleName = String.join(" ", args);
        Module module = HousingClient.getInstance().getModuleManager().getModuleByName(moduleName);

        if (module == null) {
            ChatUtils.sendClientMessage("\u00A7cModule not found: " + moduleName);
            return;
        }

        module.toggle();
        String status = module.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7f" + module.getName());
    }

    private static void handleFly(String[] args) {
        Module fly = HousingClient.getInstance().getModuleManager().getModuleByName("Fly");
        if (fly == null)
            return;

        if (args.length > 0) {
            try {
                double speed = Double.parseDouble(args[0]);
                Setting<?> speedSetting = fly.getSetting("Speed");
                if (speedSetting instanceof NumberSetting) {
                    ((NumberSetting) speedSetting).setValue(speed);
                    ChatUtils.sendClientMessage("\u00A7aFly speed set to " + speed);
                }
            } catch (NumberFormatException e) {
                ChatUtils.sendClientMessage("\u00A7cInvalid speed");
            }
        }

        fly.toggle();
        String status = fly.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7fFly");
    }

    private static void handleSpeed(String[] args) {
        Module speed = HousingClient.getInstance().getModuleManager().getModuleByName("Speed");
        if (speed == null)
            return;

        if (args.length > 0) {
            try {
                double value = Double.parseDouble(args[0]);
                Setting<?> speedSetting = speed.getSetting("Speed");
                if (speedSetting instanceof NumberSetting) {
                    ((NumberSetting) speedSetting).setValue(value);
                    ChatUtils.sendClientMessage("\u00A7aSpeed set to " + value);
                }
            } catch (NumberFormatException e) {
                ChatUtils.sendClientMessage("\u00A7cInvalid speed");
            }
        }

        speed.toggle();
        String status = speed.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7fSpeed");
    }

    private static void handleFreeCam(String[] args) {
        Module freecam = HousingClient.getInstance().getModuleManager().getModuleByName("FreeCam");
        if (freecam == null)
            return;

        if (args.length > 0) {
            try {
                double speed = Double.parseDouble(args[0]);
                Setting<?> speedSetting = freecam.getSetting("Speed");
                if (speedSetting instanceof NumberSetting) {
                    ((NumberSetting) speedSetting).setValue(speed);
                    ChatUtils.sendClientMessage("\u00A7aFreeCam speed set to " + speed);
                }
            } catch (NumberFormatException e) {
                ChatUtils.sendClientMessage("\u00A7cInvalid speed");
            }
        }

        freecam.toggle();
        String status = freecam.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7fFreeCam");
    }

    private static void handleSprint(String[] args) {
        Module sprint = HousingClient.getInstance().getModuleManager().getModuleByName("Sprint");
        if (sprint == null)
            return;

        sprint.toggle();
        String status = sprint.isEnabled() ? "\u00A7aEnabled" : "\u00A7cDisabled";
        ChatUtils.sendClientMessage(status + " \u00A7fSprint");
    }

    private static void handleBind(String[] args) {
        if (args.length < 2) {
            ChatUtils.sendClientMessage("\u00A7cUsage: .bind <module> <key>");
            return;
        }

        String moduleName = args[0];
        String keyName = args[1].toUpperCase();

        Module module = HousingClient.getInstance().getModuleManager().getModuleByName(moduleName);
        if (module == null) {
            ChatUtils.sendClientMessage("\u00A7cModule not found: " + moduleName);
            return;
        }

        int keyCode = org.lwjgl.input.Keyboard.getKeyIndex(keyName);
        if (keyCode == 0 && !keyName.equals("NONE")) {
            ChatUtils.sendClientMessage("\u00A7cInvalid key: " + keyName);
            return;
        }

        module.setKeybind(keyCode);
        if (keyCode == 0) {
            ChatUtils.sendClientMessage("\u00A7aCleared keybind for " + module.getName());
        } else {
            ChatUtils.sendClientMessage("\u00A7aBound " + module.getName() + " to " + keyName);
        }
    }

    private static void handleFriend(String[] args) {
        if (args.length < 1) {
            ChatUtils.sendClientMessage("\u00A7cUsage: .friend add/del/list <name>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "list":
                ChatUtils.sendClientMessage("\u00A7b=== Friends ===");
                for (String friend : com.housingclient.module.modules.client.FriendsModule.getFriends()) {
                    ChatUtils.sendClientMessage("\u00A7a- " + friend);
                }
                break;
            case "add":
                if (args.length < 2) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .friend add <name>");
                    return;
                }
                com.housingclient.module.modules.client.FriendsModule.addFriend(args[1]);
                ChatUtils.sendClientMessage("\u00A7aAdded friend: " + args[1]);
                break;
            case "del":
            case "remove":
                if (args.length < 2) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .friend del <name>");
                    return;
                }
                com.housingclient.module.modules.client.FriendsModule.removeFriend(args[1]);
                ChatUtils.sendClientMessage("\u00A7cRemoved friend: " + args[1]);
                break;
            case "clear":
                com.housingclient.module.modules.client.FriendsModule.clearFriends();
                ChatUtils.sendClientMessage("\u00A7cCleared all friends");
                break;
            default:
                ChatUtils.sendClientMessage("\u00A7cUnknown action: " + action);
        }
    }

    private static void listModules() {
        ChatUtils.sendClientMessage("\u00A7b\u00A7l=== Modules ===");
        for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
            String status = module.isEnabled() ? "\u00A7a[ON]" : "\u00A7c[OFF]";
            String bind = module.getKeybind() != 0
                    ? " \u00A77[" + org.lwjgl.input.Keyboard.getKeyName(module.getKeybind()) + "]"
                    : "";
            ChatUtils.sendClientMessage(status + " \u00A7f" + module.getName() + bind);
        }
    }

    public static List<String> getCommandSuggestions(String input) {
        List<String> suggestions = new ArrayList<String>();

        if (!input.startsWith(PREFIX))
            return suggestions;

        String partial = input.substring(PREFIX.length()).toLowerCase();

        for (String cmd : COMMANDS) {
            if (cmd.startsWith(partial)) {
                suggestions.add(PREFIX + cmd);
            }
        }

        for (Module module : HousingClient.getInstance().getModuleManager().getModules()) {
            String name = module.getName().toLowerCase().replace(" ", "");
            if (name.startsWith(partial)) {
                suggestions.add(PREFIX + module.getName().toLowerCase().replace(" ", ""));
            }
        }

        return suggestions;
    }
}
