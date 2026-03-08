package com.housingclient.utils.font;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.housingclient.utils.ChatUtils;

/**
 * Manages loading and storing custom fonts.
 */
public class FontManager {

    private static FontManager instance;
    private final Map<String, CustomFontRenderer> fonts = new HashMap<>();
    private CustomFontRenderer currentFont;
    private final File fontDir;

    public FontManager() {
        // "HousingClient/fonts" relative to game dir
        fontDir = new File(Minecraft.getMinecraft().mcDataDir, "HousingClient/fonts");
        if (!fontDir.exists()) {
            fontDir.mkdirs();
        }

        // Initialize with default/fallback
        fonts.put("default", null); // Key for default minecraft font
    }

    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    public void loadFonts() {
        fonts.clear();
        fonts.put("default", null);

        if (!fontDir.exists()) {
            fontDir.mkdirs();
        }

        // List of bundled fonts to extract
        String[] bundledFonts = { "Inter-Regular.ttf", "Inter-Bold.ttf" };

        for (String fontName : bundledFonts) {
            File fontFile = new File(fontDir, fontName);
            if (!fontFile.exists()) {
                ChatUtils.sendClientMessage("Debug: Extracting bundled font: " + fontName);
                try (InputStream is = getClass().getResourceAsStream("/assets/housingclient/fonts/" + fontName)) {
                    if (is != null) {
                        java.nio.file.Files.copy(is, fontFile.toPath());
                        ChatUtils.sendClientMessage("Debug: Extracted " + fontName);
                    } else {
                        ChatUtils.sendClientMessage("Debug: Bundled font not found in JAR: " + fontName);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ChatUtils.sendClientMessage("Debug: Failed to extract " + fontName + ": " + e.getMessage());
                }
            }
        }

        if (fontDir.exists() && fontDir.isDirectory()) {
            File[] files = fontDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".ttf"));

            ChatUtils.sendClientMessage("Debug: Found " + (files != null ? files.length : 0) + " font files in "
                    + fontDir.getAbsolutePath());

            if (files != null) {
                for (File file : files) {
                    try {
                        Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                        font = font.deriveFont(18f); // Default size 18 (roughly equals MC size)
                        CustomFontRenderer renderer = new CustomFontRenderer(font, true, true);

                        // Use filename without extension as key
                        String name = file.getName().substring(0, file.getName().length() - 4);
                        fonts.put(name, renderer);
                        ChatUtils.sendClientMessage("Debug: Loaded font " + name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        ChatUtils.sendClientMessage("Debug: Error loading " + file.getName());
                    }
                }
            }
        }
    }

    public void setFont(String fontName) {
        if (fonts.containsKey(fontName)) {
            currentFont = fonts.get(fontName);
        } else {
            currentFont = null; // Fallback to default
        }
    }

    public CustomFontRenderer getCurrentFont() {
        return currentFont;
    }

    public CustomFontRenderer getFont(String name) {
        return fonts.get(name);
    }

    public List<String> getAvailableFonts() {
        return new ArrayList<>(fonts.keySet());
    }
}
