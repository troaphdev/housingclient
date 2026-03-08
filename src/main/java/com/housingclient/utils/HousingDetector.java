package com.housingclient.utils;

import com.housingclient.HousingClient;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HousingDetector {
    
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    private boolean inHousing = false;
    private boolean isOwner = false;
    private boolean isCoOwner = false;
    private String currentPlotName = "";
    private String currentOwner = "";
    private int visitorCount = 0;
    
    private static final Pattern HOUSING_PATTERN = Pattern.compile("(?:Your|\\w+'s) Housing");
    private static final Pattern OWNER_PATTERN = Pattern.compile("Owner: (\\w+)");
    private static final Pattern VISITORS_PATTERN = Pattern.compile("Visitors: (\\d+)");
    
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        
        // Check scoreboard for Housing detection
        checkScoreboard();
    }
    
    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        String message = ChatUtils.stripColorCodes(event.message.getUnformattedText());
        
        // Detect ownership status
        if (message.contains("You are now visiting")) {
            isOwner = false;
            isCoOwner = false;
            HousingClient.getInstance().setOwnerMode(false);
        } else if (message.contains("Welcome to your Housing!") || 
                   message.contains("Teleported to your Housing")) {
            isOwner = true;
            HousingClient.getInstance().setOwnerMode(true);
        } else if (message.contains("You are now a co-owner")) {
            isCoOwner = true;
            HousingClient.getInstance().setOwnerMode(true);
        }
        
        // Detect visitor count changes
        Matcher visitorMatcher = VISITORS_PATTERN.matcher(message);
        if (visitorMatcher.find()) {
            visitorCount = Integer.parseInt(visitorMatcher.group(1));
        }
    }
    
    private void checkScoreboard() {
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        
        if (objective == null) {
            inHousing = false;
            return;
        }
        
        String title = ChatUtils.stripColorCodes(objective.getDisplayName());
        
        // Check if we're on Hypixel
        if (!title.contains("HYPIXEL")) {
            inHousing = false;
            return;
        }
        
        // Get scoreboard lines
        List<String> lines = getScoreboardLines(scoreboard, objective);
        
        for (String line : lines) {
            String cleanLine = ChatUtils.stripColorCodes(line);
            
            // Check for Housing indicator
            if (cleanLine.contains("Housing") || cleanLine.contains("housing")) {
                inHousing = true;
            }
            
            // Check for owner info
            Matcher ownerMatcher = OWNER_PATTERN.matcher(cleanLine);
            if (ownerMatcher.find()) {
                currentOwner = ownerMatcher.group(1);
                
                // Check if we're the owner
                if (mc.thePlayer != null && 
                    currentOwner.equalsIgnoreCase(mc.thePlayer.getName())) {
                    isOwner = true;
                    HousingClient.getInstance().setOwnerMode(true);
                }
            }
        }
    }
    
    private List<String> getScoreboardLines(Scoreboard scoreboard, ScoreObjective objective) {
        List<String> lines = new ArrayList<>();
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
            lines.add(line);
        }
        
        return lines;
    }
    
    public boolean isInHousing() {
        return inHousing;
    }
    
    public boolean isOwner() {
        return isOwner;
    }
    
    public boolean isCoOwner() {
        return isCoOwner;
    }
    
    public boolean hasOwnerPermissions() {
        return isOwner || isCoOwner;
    }
    
    public String getCurrentPlotName() {
        return currentPlotName;
    }
    
    public String getCurrentOwner() {
        return currentOwner;
    }
    
    public int getVisitorCount() {
        return visitorCount;
    }
    
    public void setInHousing(boolean inHousing) {
        this.inHousing = inHousing;
    }
    
    public void setOwner(boolean owner) {
        this.isOwner = owner;
        HousingClient.getInstance().setOwnerMode(owner || isCoOwner);
    }
}

