package com.housingclient.module.modules.render;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import net.minecraft.entity.Entity;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HideHykiaEntitiesModule extends Module {

    private boolean inHykiaContext = false;
    private int tickTimer = 0;

    // Hykia Zone Coordinates
    private static final double MIN_X = -86;
    private static final double MAX_X = -37;
    private static final double MIN_Y = 47;
    private static final double MAX_Y = 90;
    private static final double MIN_Z = -62;
    private static final double MAX_Z = 23;

    private static HideHykiaEntitiesModule INSTANCE;

    public HideHykiaEntitiesModule() {
        super("Hide Hykia Entities", "Hides entities in Hykia lobby region", Category.RENDER, ModuleMode.BOTH);
        INSTANCE = this;
    }

    public static boolean shouldHide(Entity entity) {
        if (INSTANCE == null || !INSTANCE.isEnabled() || !INSTANCE.inHykiaContext)
            return false;
        return INSTANCE.isInZone(entity);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<?> event) {
        if (!inHykiaContext)
            return;

        if (isInZone(event.entity)) {
            event.setCanceled(true);
        }
    }

    private boolean isInZone(Entity entity) {
        return entity.posX >= MIN_X && entity.posX <= MAX_X &&
                entity.posY >= MIN_Y && entity.posY <= MAX_Y &&
                entity.posZ >= MIN_Z && entity.posZ <= MAX_Z;
    }

    @Override
    public void onTick() {
        if (mc.theWorld == null) {
            inHykiaContext = false;
            return;
        }

        // Check context every 20 ticks (1 second) to save performance
        if (tickTimer++ < 20 && inHykiaContext) {
            // If we are already in context, keep it (assume it doesn't change instantly)
            // But if we aren't, wait for timer.
            // Actually, for responsiveness, checking every 20 ticks is fine.
            return;
        }
        tickTimer = 0;
        boolean newContext = checkHykiaContext();
        if (newContext != inHykiaContext) {
            inHykiaContext = newContext;
            if (inHykiaContext) {
                // com.housingclient.utils.ChatUtils.sendClientMessage("\u00A7a[HideHykia]
                // Context Active! Hiding entities in zone.");
            } else {
                // com.housingclient.utils.ChatUtils.sendClientMessage("\u00A7c[HideHykia]
                // Context Lost.");
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(net.minecraftforge.client.event.RenderPlayerEvent.Pre event) {
        if (!inHykiaContext)
            return;
        if (isInZone(event.entity))
            event.setCanceled(true);
    }

    @SubscribeEvent
    public void onRenderLivingSpecials(RenderLivingEvent.Specials.Pre<?> event) {
        if (!inHykiaContext)
            return;
        if (isInZone(event.entity))
            event.setCanceled(true);
    }

    private boolean checkHykiaContext() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null)
            return false;

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null)
            return false;

        boolean housingFound = false;
        String serverId = null;

        String title = stripColor(objective.getDisplayName());
        // Check Title for "HOUSING"
        if (title.toUpperCase().contains("HOUSING")) {
            housingFound = true;
        }

        Collection<Score> scores = scoreboard.getSortedScores(objective);
        Pattern idPattern = Pattern.compile("([LlMm][a-zA-Z0-9]{3,9})$");

        for (Score score : scores) {
            String playerName = score.getPlayerName();
            ScorePlayerTeam team = scoreboard.getPlayersTeam(playerName);
            String line = (team != null) ? team.getColorPrefix() + playerName + team.getColorSuffix() : playerName;

            String stripped = stripColor(line).trim();
            if (stripped.isEmpty())
                continue;

            // Check "HOUSING" in lines (backup if not in title)
            if (stripped.toUpperCase().contains("HOUSING")) {
                housingFound = true;
            }

            // Check Server ID
            // 1. Matches "MM/DD/YY ID" pattern (e.g. 01/07/26 L4B)
            if (stripped.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+\\S+")) {
                String[] parts = stripped.split("\\s+");
                if (parts.length >= 2) {
                    String id = parts[parts.length - 1];
                    if (id.startsWith("L"))
                        serverId = id;
                }
            }
            // 2. Matches ID pattern
            Matcher matcher = idPattern.matcher(stripped);
            if (matcher.find()) {
                String id = matcher.group(1);
                if (id.startsWith("L"))
                    serverId = id;
            }
            // 3. Simple L-ID
            if (stripped.matches("[a-zA-Z0-9]{4,10}") && stripped.startsWith("L")) {
                serverId = stripped;
            }
        }

        /* Debugging for User */
        if (!housingFound || serverId == null) {
            // Uncomment to debug specific failures if needed
        } else {
            // Found both
        }

        return housingFound && serverId != null;
    }

    private String stripColor(String input) {
        return input.replaceAll("\u00A7.", "");
    }
}
