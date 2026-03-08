package com.housingclient.module.modules.visuals;

import com.housingclient.HousingClient;
import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.modules.client.HudDesignerModule;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.NumberSetting;
import com.housingclient.utils.RenderUtils;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.*;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Scoreboard Module
 * 
 * Renders a custom moveable scoreboard.
 * Vanilla scoreboard rendering is cancelled by MixinGuiIngame when this module
 * is enabled.
 * 
 * Features:
 * - Moveable via HUD Designer
 * - Hide score numbers option
 * - Custom background style
 * - Clean appearance
 */
public class ScoreboardModule extends Module {

    private final BooleanSetting hideNumbers = new BooleanSetting("Hide Numbers",
            "Hide the red score numbers", true);
    private final BooleanSetting cleanBackground = new BooleanSetting("Clean Background",
            "Use cleaner dark background", true);
    private final BooleanSetting roundedCorners = new BooleanSetting("Rounded Corners",
            "Use rounded corners on background", true);
    private final NumberSetting opacity = new NumberSetting("Background Opacity",
            "Background opacity %", 30, 0, 100, 5);

    public ScoreboardModule() {
        super("Scoreboard", "Custom moveable scoreboard with cleaner look", Category.VISUALS, ModuleMode.BOTH);
        addSetting(hideNumbers);
        addSetting(cleanBackground);
        addSetting(roundedCorners);
        addSetting(opacity);
    }

    // No need for onEnable/onDisable - MixinGuiIngame checks isEnabled() directly

    /**
     * Render custom scoreboard after all other elements
     */
    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (!isEnabled())
            return;
        if (event.type != RenderGameOverlayEvent.ElementType.ALL)
            return;
        if (mc.theWorld == null || mc.thePlayer == null)
            return;
        if (mc.gameSettings.showDebugInfo)
            return; // Don't show when F3 is open

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null)
            return;

        renderCustomScoreboard(objective, new ScaledResolution(mc));
    }

    private void renderCustomScoreboard(ScoreObjective objective, ScaledResolution sr) {
        Scoreboard scoreboard = objective.getScoreboard();
        Collection<Score> scores = scoreboard.getSortedScores(objective);

        // Filter to only show scores with players that have entries
        List<Score> filteredScores = scores.stream()
                .filter(score -> score.getPlayerName() != null && !score.getPlayerName().startsWith("#"))
                .collect(Collectors.toList());

        // Limit to 15 entries like vanilla
        if (filteredScores.size() > 15) {
            filteredScores = Lists.newArrayList(Iterables.skip(filteredScores, filteredScores.size() - 15));
        }

        if (filteredScores.isEmpty())
            return;

        FontRenderer fr = mc.fontRendererObj;
        int fontHeight = fr.FONT_HEIGHT;

        // Calculate dimensions
        int maxWidth = fr.getStringWidth(objective.getDisplayName());

        for (Score score : filteredScores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String text = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());

            int lineWidth = fr.getStringWidth(text);
            if (!hideNumbers.isEnabled()) {
                lineWidth += fr.getStringWidth(": " + score.getScorePoints());
            }
            maxWidth = Math.max(maxWidth, lineWidth);
        }

        int linesCount = filteredScores.size();
        int totalHeight = linesCount * fontHeight + fontHeight + 4; // +title +padding
        int width = maxWidth + 6;

        // Get position from HUD Designer or default to right side
        int x, y;
        HudDesignerModule designer = HousingClient.getInstance().getModuleManager().getModule(HudDesignerModule.class);
        if (designer != null && designer.getScoreboardX() >= 0) {
            x = designer.getScoreboardX();
            y = designer.getScoreboardY();
        } else {
            // Default: right side, vertically centered
            x = sr.getScaledWidth() - width - 3;
            y = (sr.getScaledHeight() - totalHeight) / 2;
        }

        // Background color
        int alpha = (int) (opacity.getValue() * 255.0 / 100.0);
        if (alpha == 0 && opacity.getValue() == 0)
            alpha = 1; // Fix bug where 0 alpha is rendered as opaque
        int bgColor = (alpha << 24) | 0x000000;

        // Draw background
        if (roundedCorners.isEnabled()) {
            RenderUtils.drawRoundedRect(x - 2, y - 2, width + 4, totalHeight + 2, 4, bgColor);
        } else {
            Gui.drawRect(x - 2, y - 2, x + width + 2, y + totalHeight, bgColor);
        }

        // Draw title centered
        String title = objective.getDisplayName();
        int titleX = x + (width - fr.getStringWidth(title)) / 2;
        fr.drawStringWithShadow(title, titleX, y, 0xFFFFFFFF);

        // Draw scores
        int lineY = y + fontHeight + 2;

        // Reverse to draw from top to bottom (highest score first)
        for (int i = filteredScores.size() - 1; i >= 0; i--) {
            Score score = filteredScores.get(i);
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String playerName = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());

            // Draw player name
            fr.drawStringWithShadow(playerName, x, lineY, 0xFFFFFFFF);

            // Draw score number (if not hidden)
            if (!hideNumbers.isEnabled()) {
                String scoreText = "" + score.getScorePoints();
                int scoreX = x + width - fr.getStringWidth(scoreText);
                fr.drawStringWithShadow(scoreText, scoreX, lineY, 0xFFFF5555);
            }

            lineY += fontHeight;
        }
    }

    /**
     * Get the bounding box for HUD Editor
     */
    public int[] getBounds() {
        if (mc.theWorld == null)
            return new int[] { 0, 0, 100, 100 };

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null)
            return new int[] { 0, 0, 100, 100 };

        // Calculate approximate size
        Collection<Score> scores = scoreboard.getSortedScores(objective);
        int lineCount = Math.min(scores.size(), 15);
        int height = (lineCount + 1) * mc.fontRendererObj.FONT_HEIGHT + 6;
        int width = 100; // Approximate

        return new int[] { 0, 0, width, height };
    }
}
