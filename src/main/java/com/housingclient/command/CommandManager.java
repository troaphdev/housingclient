package com.housingclient.command;

import com.housingclient.HousingClient;
import com.housingclient.config.ProfileManager;
import com.housingclient.config.Profile;
import com.housingclient.module.Module;
import com.housingclient.utils.ChatUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandManager {

    private final List<Command> commands = new ArrayList<>();
    private final String prefix = ".";

    public CommandManager() {
        registerCommands();
    }

    private void registerCommands() {
        // Clip command
        commands.add(new Command("clip", "Teleport in a direction", "clip <up/down/forward/backward> <distance>") {
            @Override
            public void execute(String[] args) {
                if (args.length < 2) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .clip <direction> <distance>");
                    return;
                }

                double distance;
                try {
                    distance = Double.parseDouble(args[1]);
                } catch (NumberFormatException e) {
                    ChatUtils.sendClientMessage("\u00A7cInvalid distance: " + args[1]);
                    return;
                }

                Minecraft mc = Minecraft.getMinecraft();
                if (mc.thePlayer == null)
                    return;

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

                mc.thePlayer.setPosition(x, y, z);
                mc.getNetHandler().addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(x, y, z, false));
                ChatUtils
                        .sendClientMessage("\u00A7aClipped " + dir + " " + String.format("%.1f", distance) + " blocks");
            }
        });

        // Toggle command
        commands.add(new Command("t", "Toggle a module", "t <module>") {
            @Override
            public void execute(String[] args) {
                if (args.length < 1) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .t <module>");
                    return;
                }

                String moduleName = String.join(" ", args);
                Module module = HousingClient.getInstance().getModuleManager().getModule(moduleName);
                if (module != null) {
                    module.toggle();
                    ChatUtils.sendClientMessage(
                            module.getName() + " \u00A77"
                                    + (module.isEnabled() ? "\u00A7aenabled" : "\u00A7cdisabled"));
                } else {
                    ChatUtils.sendClientMessage("\u00A7cModule not found: " + moduleName);
                }
            }
        });

        // Bind command
        commands.add(new Command("bind", "Bind a key to a module", "bind <module> <key>") {
            @Override
            public void execute(String[] args) {
                if (args.length < 2) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .bind <module> <key>");
                    return;
                }

                String key = args[args.length - 1];
                String moduleName = String.join(" ", Arrays.copyOfRange(args, 0, args.length - 1));

                Module module = HousingClient.getInstance().getModuleManager().getModule(moduleName);
                if (module != null) {
                    int keyCode = org.lwjgl.input.Keyboard.getKeyIndex(key.toUpperCase());
                    module.setKeybind(keyCode);
                    ChatUtils.sendClientMessage("\u00A7aBound " + module.getName() + " to " + key.toUpperCase());
                } else {
                    ChatUtils.sendClientMessage("\u00A7cModule not found: " + moduleName);
                }
            }
        });

        // Profile command
        commands.add(new Command("profile", "Manage profiles", "profile <load/save/list> [name]") {
            @Override
            public void execute(String[] args) {
                if (args.length < 1) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .profile <load/save/list> [name]");
                    return;
                }

                ProfileManager profileManager = HousingClient.getInstance().getProfileManager();

                switch (args[0].toLowerCase()) {
                    case "load":
                        if (args.length < 2) {
                            ChatUtils.sendClientMessage("\u00A7cUsage: .profile load <name>");
                            return;
                        }
                        profileManager.setActiveProfile(args[1]);
                        ChatUtils.sendClientMessage("\u00A7aLoaded profile: " + args[1]);
                        break;
                    case "save":
                        profileManager.saveCurrentToProfile();
                        ChatUtils
                                .sendClientMessage("\u00A7aSaved to profile: " + profileManager.getActiveProfileName());
                        break;
                    case "list":
                        ChatUtils.sendClientMessage("\u00A77Profiles:");
                        for (Profile profile : profileManager.getProfiles()) {
                            String pfx = profile.getName().equals(profileManager.getActiveProfileName()) ? "\u00A7a> "
                                    : "\u00A77- ";
                            ChatUtils.sendClientMessageNoPrefix(pfx + profile.getName());
                        }
                        break;
                    default:
                        ChatUtils.sendClientMessage("\u00A7cUnknown subcommand: " + args[0]);
                }
            }
        });

        // Safe mode command
        commands.add(new Command("safe", "Toggle safe mode", "safe") {
            @Override
            public void execute(String[] args) {
                HousingClient.getInstance().toggleSafeMode();
            }
        });

        // Font command
        commands.add(new Command("font", "Manage custom fonts", "font <reload/list/set> [name]") {
            @Override
            public void execute(String[] args) {
                if (args.length < 1) {
                    ChatUtils.sendClientMessage("\u00A7cUsage: .font <reload/list/set> [name]");
                    return;
                }

                com.housingclient.utils.font.FontManager fontManager = com.housingclient.utils.font.FontManager
                        .getInstance();

                switch (args[0].toLowerCase()) {
                    case "reload":
                        fontManager.loadFonts();
                        ChatUtils.sendClientMessage("\u00A7aFonts reloaded!");
                        break;
                    case "list":
                        ChatUtils.sendClientMessage("\u00A77Available Fonts:");
                        for (String font : fontManager.getAvailableFonts()) {
                            ChatUtils.sendClientMessageNoPrefix("\u00A77- " + font);
                        }
                        break;
                    case "set":
                        if (args.length < 2) {
                            ChatUtils.sendClientMessage("\u00A7cUsage: .font set <name>");
                            return;
                        }
                        String fontName = args[1];
                        if (fontManager.getAvailableFonts().contains(fontName) || fontName.equals("default")) {
                            fontManager.setFont(fontName);
                            ChatUtils.sendClientMessage("\u00A7aFont set to: " + fontName);
                            // Trigger GUI update if needed? For now, next open will catch it.
                        } else {
                            ChatUtils.sendClientMessage("\u00A7cFont not found: " + fontName);
                        }
                        break;
                    default:
                        ChatUtils.sendClientMessage("\u00A7cUnknown subcommand: " + args[0]);
                }
            }
        });

    }

    public boolean handleCommand(String message) {
        if (!message.startsWith(prefix))
            return false;

        String[] parts = message.substring(prefix.length()).split(" ");
        if (parts.length == 0)
            return false;

        String commandName = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                command.execute(args);
                return true;
            }
        }

        return false;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }
}
