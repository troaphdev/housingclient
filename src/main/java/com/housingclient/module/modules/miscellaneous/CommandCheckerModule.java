package com.housingclient.module.modules.miscellaneous;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.utils.ChatUtils;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Command Checker Module — Instant
 * 
 * Sends a tab-complete request to get the server's command list,
 * compares it against the default Hypixel housing commands,
 * filters out forge mod commands, and displays any custom
 * (non-default) commands in chat separated by commas.
 */
public class CommandCheckerModule extends Module {

    private boolean awaitingResponse = false;
    private int timeout = 0;

    // Default housing commands (from commands.txt — full Hypixel housing default list)
    private static final Set<String> DEFAULT_COMMANDS = new HashSet<>(Arrays.asList(
            "//", "//clearselection", "//copy", "//cut", "//desel", "//fill", "//paste",
            "//pos1", "//pos2", "//posa", "//posb", "//replace", "//set", "//undo",
            "//walls", "//wand", "//wireframe",
            "/about", "/ac", "/achat", "/achrewards", "/adat", "/addstaffbookmark",
            "/addstaffbookmarkforplayer", "/advent", "/adventmenu", "/aihe",
            "/Altın", "/arany", "/atlas", "/aur",
            "/autostartstop_disable", "/autostartstop_enable_counting", "/autostartstop_enable_recording",
            "/back", "/block", "/booster", "/boosteradmin", "/border",
            "/bug", "/bugreport", "/buildMode", "/buildreport",
            "/chatinput", "/chatkeywords", "/chatreport", "/chatspy",
            "/citizens", "/citizens2", "/clearfurniture",
            "/clearglobalstats", "/clearplayerstats", "/clearselection", "/clearspawn",
            "/clearstats", "/clearteamstats", "/click", "/clima",
            "/combatinfo", "/command", "/commands", "/companion", "/configvisualizer",
            "/copy", "/cr", "/creport", "/csoport", "/cuaca",
            "/customcommands", "/custommenu", "/custommenus", "/customSkull", "/customstatus",
            "/cut", "/dane", "/dati", "/debug", "/desel", "/dfreset",
            "/die", "/dimension", "/disablepunch",
            "/données", "/dontpersistrecording", "/dumpSigns",
            "/edit", "/editglobalstat", "/editglobalstats", "/edititem",
            "/editplayerstat", "/editplayerstats", "/editstat", "/editstats",
            "/editteamstat", "/editteamstats",
            "/emas", "/emoji", "/emojihelp", "/emojis", "/emotes", "/empty",
            "/enchant", "/esine",
            "/eventaction", "/eventactions", "/eşya",
            "/Fest", "/Festa", "/fill", "/fly",
            "/function", "/functions", "/fw", "/föremål",
            "/gamehistory", "/gamemode", "/gamerule", "/gamerules", "/games",
            "/genstand", "/getoffmyface", "/gjenstand", "/glow",
            "/gm", "/gma", "/gmc", "/gms", "/gmsp",
            "/goud", "/group", "/groupe", "/grup", "/grupa", "/grupo",
            "/grupp", "/gruppe", "/gruppo", "/guld", "/gull",
            "/h", "/hava durumu", "/headstatus", "/hello", "/help", "/hi",
            "/hmenu", "/home", "/house", "/housefix", "/houselang",
            "/housereport", "/houses", "/housesetting", "/housetag",
            "/housing", "/housinglimits", "/housingmenu", "/housingplaceholders",
            "/icanhasbukkit", "/időjárás", "/ignore", "/Impreza",
            "/información", "/infoskull",
            "/internalrankgift", "/internalrankgiftcheck", "/invspy",
            "/item", "/itemreport",
            "/join", "/joinadvertisedgame", "/Juhlat",
            "/kultaa", "/kumpulan",
            "/lang", "/language", "/layout", "/layouts",
            "/limits", "/listlimits", "/listplaceholders",
            "/loadresourcepack", "/loadresourcepackimmediately",
            "/map", "/mapfeedback", "/maxVisitors", "/me", "/menu", "/menus",
            "/meteo", "/mmreport", "/motiv", "/motyw", "/mp", "/msgreport",
            "/multiLobbyDump", "/myfilter", "/mygames", "/myhouses",
            "/mypos", "/myposition", "/météo",
            "/namereport", "/networkbooster", "/newqueue",
            "/nick", "/nickTest", "/npc", "/npc2", "/npcdump",
            "/objet", "/objeto", "/oggetto",
            "/openachievementmenu", "/openduelpickpage", "/openpunchmessagemenu",
            "/openresourcepackbook", "/opme", "/or", "/oro", "/ouro",
            "/p", "/park", "/parkour", "/Parti", "/particlequality", "/Party",
            "/paste", "/pastgames", "/permissions", "/persistrecording",
            "/Pesta", "/pet", "/pl", "/placeholders",
            "/plotborder", "/plugins", "/pogoda",
            "/pos1", "/pos2", "/posa", "/posb", "/počasí",
            "/pps", "/pq", "/profile", "/pvpinfo", "/předmět",
            "/quality", "/questMenu",
            "/rank", "/rankcolor", "/rankcolour", "/recentgames",
            "/region", "/regions", "/regiontool", "/rej", "/rejoin",
            "/reloadSigns", "/rename", "/renameitem",
            "/replace", "/replay", "/replays",
            "/report", "/reportbug", "/reportbuild", "/reportcontext",
            "/reporthome", "/reporthouse", "/reportmenu", "/reportname",
            "/reporttext", "/reportworld",
            "/requeue", "/resethouse", "/resetlocation", "/resetparkour",
            "/resetplot", "/resetspawn",
            "/resource", "/resourcebook", "/resourcehelp",
            "/resourcepackbook", "/resourcepackhelp",
            "/restart", "/return", "/rewards",
            "/rg", "/rmnpcs", "/run_housing_command", "/ryhmä", "/rzecz",
            "/safemode", "/save", "/savehouse", "/saveworld",
            "/sc", "/scoreboard", "/script", "/secretinstance",
            "/set", "/setbiome", "/setcarpenterlocation", "/setlang", "/setlanguage",
            "/setsky", "/setspawn", "/settheme",
            "/setting", "/settings", "/setvisibility", "/setweather",
            "/share", "/shout", "/sisyphus",
            "/skullpacks", "/skulls", "/skupina",
            "/smp", "/social", "/socialinternal", "/socialMode", "/socialoptions",
            "/spawn", "/startagain", "/startlive",
            "/startrecording", "/startrecordingcounting",
            "/stopallrecordings", "/stoprecording",
            "/surveyinternal", "/sää",
            "/team", "/teamchatspy", "/teams",
            "/teleport", "/tema", "/template", "/temă",
            "/testplaceholder", "/textreport",
            "/thema", "/theme", "/thiscommandliterallydoesnothing", "/thème",
            "/time", "/toggle", "/toggleBorder", "/togglechat",
            "/toggleflight", "/togglefly", "/toggleJukebox",
            "/toggleMessages", "/toggleMode", "/toggleMusic",
            "/togglesetting", "/toggleTips",
            "/tp", "/tpa", "/tpl", "/tr",
            "/trackcombat", "/trackpvp",
            "/trait", "/traitc", "/trc",
            "/tárgy", "/téma",
            "/unblock", "/undo", "/unnick",
            "/vanish", "/var", "/variable", "/variables", "/vars",
            "/vejr", "/ver", "/veri", "/version", "/versions",
            "/viewallteamchat", "/viewglobalstats", "/viewlimits",
            "/viewplaceholders", "/viewplayerstats", "/viewprofile",
            "/viewstats", "/viewteamstats",
            "/visibility", "/visit", "/visitingrule", "/votemap", "/vreme",
            "/väder", "/vær",
            "/walls", "/wand", "/watchdogreport", "/watchlive",
            "/waypoint", "/waypoints",
            "/wdr", "/wdreport", "/weather", "/webprofile", "/weer",
            "/who", "/wireframe", "/worldreport", "/wp", "/wreport", "/wtfmap",
            "/zlatá", "/złoto",
            "/αντικείμενο", "/δεδομένα", "/θέμα", "/καιρός", "/ομάδα",
            "/Πάρτυ", "/χρυσό",
            "/Вечеринка", "/Вечірка", "/група", "/группа",
            "/данные", "/дані", "/золото", "/Парти",
            "/погода", "/предмет", "/тема",
            "/ปาร์ตี้",
            "/アイテム", "/グループ", "/ゴールド", "/テーマ", "/データ",
            "/主題", "/主题", "/举报", "/喊话",
            "/天气", "/天気", "/天氣", "/数据", "/物品",
            "/組隊", "/组", "/组队", "/群組", "/資料", "/金", "/随从",
            "/골드", "/그룹", "/날씨", "/데이터", "/아이템", "/주제", "/파티",
            "/組別", "/金色"
    ));

    // Housing owners can define /clear as a custom command. Always report it
    // even if another client-side command happens to use the same name.
    private static final Set<String> ALWAYS_CUSTOM_COMMANDS = new HashSet<>(Arrays.asList(
            "/clear"
    ));

    public CommandCheckerModule() {
        super("Command Checker", "Checks for custom commands in this housing", Category.MISCELLANEOUS, ModuleMode.BOTH);
    }

    @Override
    protected void onEnable() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            ChatUtils.sendClientMessage("\u00A7c[Command Checker] \u00A7fNot connected to a server!");
            setEnabled(false);
            return;
        }

        // Send tab-complete request for "/"
        mc.getNetHandler().addToSendQueue(new C14PacketTabComplete("/"));
        awaitingResponse = true;
        timeout = 100; // 5 seconds timeout
    }

    @Override
    protected void onDisable() {
        awaitingResponse = false;
        timeout = 0;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!isEnabled() || !awaitingResponse) return;

        timeout--;
        if (timeout <= 0) {
            ChatUtils.sendClientMessage("\u00A7c[Command Checker] \u00A7fTimed out waiting for server response.");
            awaitingResponse = false;
            setEnabled(false);
        }
    }

    /**
     * Get the set of commands registered by forge mods on the client side.
     * These should be excluded from the custom command list.
     */
    private Set<String> getForgeModCommands() {
        Set<String> forgeCommands = new HashSet<>();
        try {
            Map<String, ?> commandMap = net.minecraftforge.client.ClientCommandHandler.instance.getCommands();
            if (commandMap != null) {
                for (String cmd : commandMap.keySet()) {
                    String normalized = cmd.toLowerCase(Locale.ROOT);
                    if (!normalized.startsWith("/")) {
                        normalized = "/" + normalized;
                    }
                    forgeCommands.add(normalized);
                }
            }
        } catch (Exception e) {
            // Forge command handler not available, ignore
        }
        return forgeCommands;
    }

    /**
     * Called when we receive tab-complete results from the server.
     * This should be invoked from a mixin or packet handler.
     */
    public void onTabCompleteResponse(S3APacketTabComplete packet) {
        if (!awaitingResponse) return;
        awaitingResponse = false;

        String[] matches = packet.func_149630_c();
        if (matches == null || matches.length == 0) {
            ChatUtils.sendClientMessage("\u00A7c[Command Checker] \u00A7fNo commands received from server.");
            setEnabled(false);
            return;
        }

        // Build lowercase default set for comparison
        Set<String> lowerDefaults = new HashSet<>();
        for (String d : DEFAULT_COMMANDS) {
            lowerDefaults.add(d.toLowerCase(Locale.ROOT));
        }

        // Get forge mod commands to exclude
        Set<String> forgeCommands = getForgeModCommands();

        // Find non-default, non-forge commands
        List<String> customCommands = new ArrayList<>();
        for (String cmd : matches) {
            String trimmed = net.minecraft.util.StringUtils.stripControlCodes(cmd).trim();
            if (trimmed.isEmpty()) continue;

            String normalized = trimmed.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }

            boolean alwaysCustom = ALWAYS_CUSTOM_COMMANDS.contains(normalized);

            // Skip default commands
            if (!alwaysCustom && lowerDefaults.contains(normalized)) continue;

            // Skip forge mod commands
            if (!alwaysCustom && forgeCommands.contains(normalized)) continue;

            // Prefix with slash for display if missing
            if (!trimmed.startsWith("/")) {
                trimmed = "/" + trimmed;
            }
            customCommands.add(trimmed);
        }

        // Display results
        if (customCommands.isEmpty()) {
            ChatUtils.sendClientMessage("\u00A7a[Command Checker] \u00A7fNo custom commands found. This housing uses default commands only.");
        } else {
            ChatUtils.sendClientMessage("\u00A7e[Command Checker] \u00A7fFound \u00A7a" + customCommands.size() + "\u00A7f custom command(s):");
            // Display all commands on one line, separated by white commas
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < customCommands.size(); i++) {
                if (i > 0) {
                    sb.append("\u00A7f, ");
                }
                sb.append("\u00A7b").append(customCommands.get(i));
            }
            ChatUtils.sendClientMessageNoPrefix(sb.toString());
        }

        // Auto-disable (instant module)
        setEnabled(false);
    }
}
