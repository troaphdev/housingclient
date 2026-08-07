package com.housingclient.module.modules.visuals;

import com.housingclient.module.Category;
import com.housingclient.module.Module;
import com.housingclient.module.ModuleMode;
import com.housingclient.module.settings.BooleanSetting;
import com.housingclient.module.settings.StringSetting;
import com.housingclient.utils.MinecraftFormatting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replaces the local account name and skin only in the client renderer.
 */
public class NickHiderModule extends Module {

    private static final GameProfile DEFAULT_STEVE_PROFILE = new GameProfile(new UUID(0L, 0L), "Steve");
    private static final Map<String, String> SKIN_FINGERPRINT_CACHE = new ConcurrentHashMap<>();

    private static NickHiderModule instance;
    private static String cachedAccountName;
    private static Pattern cachedAccountPattern;
    private static volatile ResourceLocation replacementSkin = DefaultPlayerSkin.getDefaultSkinLegacy();
    private static volatile ResourceLocation replacementCape;
    private static volatile String replacementSkinModel = "default";
    private static volatile GameProfile replacementProfile = DEFAULT_STEVE_PROFILE;
    private static volatile String requestedSkinKey = "steve";
    private static volatile String actualSkinFingerprint;
    private static String actualSessionKey = "";

    private final StringSetting displayName = new StringSetting(
            "Display Name",
            "Name shown instead of your account; supports Minecraft color codes such as &b",
            "Steve",
            64);

    private final StringSetting skinUsername = new StringSetting(
            "Skin Username",
            "Minecraft username whose skin and arm model will be shown",
            "Steve",
            16);

    private final StringSetting displayRole = new StringSetting(
            "Display Role",
            "Complete role shown where a role exists; include and color brackets however you want",
            "[]",
            64);

    private final BooleanSetting topOfTabList = new BooleanSetting(
            "Top of Tab List",
            "Move your disguised entry to the top of the rendered tab list",
            false);

    private String observedSkinKey = "steve";
    private String loadedSkinKey = "";
    private long skinSettingChangedAt;

    public NickHiderModule() {
        super("Nick Hider", "Visually hide your username and skin while streaming", Category.VISUALS, ModuleMode.BOTH);
        instance = this;
        addSetting(displayName);
        addSetting(skinUsername);
        addSetting(displayRole);
        addSetting(topOfTabList);
    }

    /**
     * Replaces every case-insensitive occurrence of the active session username.
     * This changes only strings passed to client-side renderers.
     */
    public static String replaceOwnName(String text) {
        if (text == null || text.isEmpty() || !isActive()) {
            return text;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getSession() == null) {
            return text;
        }

        String accountName = minecraft.getSession().getUsername();
        if (accountName == null || accountName.isEmpty()) {
            return text;
        }

        Pattern accountPattern = getAccountPattern(accountName);
        Matcher matcher = accountPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        String replacementName = instance.getReplacementName();
        String replacementRole = instance.getReplacementRole();

        while (matcher.find()) {
            String roleSuffix = matcher.group(1);
            String replacement = replacementName;

            if (roleSuffix != null) {
                if (!replacementRole.isEmpty()) {
                    replacement += " " + replacementRole;
                }
            }

            replacement += MinecraftFormatting.activeFormattingAt(text, matcher.end());

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Returns true only for the profile belonging to the active local session.
     */
    public static boolean shouldHideSkin(GameProfile profile) {
        if (profile == null || !isActive()) {
            return false;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getSession() == null) {
            return false;
        }

        GameProfile sessionProfile = minecraft.getSession().getProfile();
        UUID profileId = profile.getId();
        UUID sessionId = sessionProfile == null ? null : sessionProfile.getId();

        if (profileId != null && sessionId != null && profileId.equals(sessionId)) {
            return true;
        }

        String profileName = profile.getName();
        String sessionName = minecraft.getSession().getUsername();
        if (profileName != null && sessionName != null && profileName.equalsIgnoreCase(sessionName)) {
            return true;
        }

        String candidateFingerprint = getSkinFingerprint(profile);
        return candidateFingerprint != null
                && actualSkinFingerprint != null
                && candidateFingerprint.equalsIgnoreCase(actualSkinFingerprint);
    }

    public static GameProfile replaceMatchingSkullProfile(GameProfile profile) {
        return shouldHideSkin(profile) ? replacementProfile : profile;
    }

    public static List<NetworkPlayerInfo> reorderTabList(List<NetworkPlayerInfo> playerList) {
        if (playerList == null || playerList.isEmpty() || !isActive()
                || !instance.topOfTabList.isEnabled()) {
            return playerList;
        }

        List<NetworkPlayerInfo> reordered = new ArrayList<>(playerList);
        for (int i = 0; i < reordered.size(); i++) {
            NetworkPlayerInfo playerInfo = reordered.get(i);
            if (playerInfo != null && isCurrentProfile(playerInfo.getGameProfile())) {
                if (i > 0) {
                    reordered.remove(i);
                    reordered.add(0, playerInfo);
                }
                break;
            }
        }
        return reordered;
    }

    public static ResourceLocation getReplacementSkin() {
        return replacementSkin;
    }

    public static ResourceLocation getReplacementCape() {
        return replacementCape;
    }

    public static String getReplacementSkinModel() {
        return replacementSkinModel;
    }

    public String getReplacementName() {
        String value = displayName.getValue();
        String replacement = value == null || value.trim().isEmpty() ? "Steve" : value;
        return translateColorCodes(replacement);
    }

    private String getReplacementRole() {
        String value = displayRole.getValue();
        return value == null ? "" : translateColorCodes(value.trim());
    }

    @Override
    protected void onEnable() {
        replacementSkin = DefaultPlayerSkin.getDefaultSkinLegacy();
        replacementCape = null;
        replacementSkinModel = "default";
        replacementProfile = DEFAULT_STEVE_PROFILE;
        refreshActualSkinFingerprint();
        observedSkinKey = normalizeSkinUsername();
        loadedSkinKey = "";
        requestedSkinKey = observedSkinKey;
        skinSettingChangedAt = System.currentTimeMillis();

        if ("steve".equals(observedSkinKey)) {
            applySkinUsername(observedSkinKey);
        }
    }

    @Override
    public void onTick() {
        refreshActualSkinFingerprint();

        String currentSkinKey = normalizeSkinUsername();
        if (!currentSkinKey.equals(observedSkinKey)) {
            observedSkinKey = currentSkinKey;
            skinSettingChangedAt = System.currentTimeMillis();
        }

        if (!currentSkinKey.equals(loadedSkinKey)
                && System.currentTimeMillis() - skinSettingChangedAt >= 500L) {
            applySkinUsername(currentSkinKey);
        }
    }

    @Override
    public String getDisplayInfo() {
        return getReplacementName();
    }

    private static boolean isActive() {
        return instance != null && instance.isEnabled();
    }

    private static boolean isCurrentProfile(GameProfile profile) {
        if (profile == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getSession() == null) {
            return false;
        }

        GameProfile sessionProfile = minecraft.getSession().getProfile();
        UUID profileId = profile.getId();
        UUID sessionId = sessionProfile == null ? null : sessionProfile.getId();
        if (profileId != null && sessionId != null && profileId.equals(sessionId)) {
            return true;
        }

        String profileName = profile.getName();
        String sessionName = minecraft.getSession().getUsername();
        return profileName != null && sessionName != null && profileName.equalsIgnoreCase(sessionName);
    }

    private static Pattern getAccountPattern(String accountName) {
        if (cachedAccountPattern == null || !accountName.equals(cachedAccountName)) {
            cachedAccountName = accountName;
            cachedAccountPattern = Pattern.compile(
                    Pattern.quote(accountName)
                            + "( (?:\u00A7[0-9a-fk-or])*\\[[^\\]\\r\\n]*\\])?",
                    Pattern.CASE_INSENSITIVE);
        }
        return cachedAccountPattern;
    }

    /**
     * Supports the same alternate formatting syntax commonly used by Minecraft
     * servers: &0-&9, &a-&f, &k-&o, and &r. Existing section-sign codes are
     * preserved as-is.
     */
    private static String translateColorCodes(String text) {
        char[] characters = text.toCharArray();
        String validCodes = "0123456789abcdefklmnor";

        for (int i = 0; i < characters.length - 1; i++) {
            if (characters[i] != '&') {
                continue;
            }

            char code = Character.toLowerCase(characters[i + 1]);
            if (validCodes.indexOf(code) >= 0) {
                characters[i] = '\u00A7';
                characters[i + 1] = code;
            }
        }

        return new String(characters);
    }

    private String normalizeSkinUsername() {
        String value = skinUsername.getValue();
        if (value == null || value.trim().isEmpty()) {
            return "steve";
        }
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void applySkinUsername(final String skinKey) {
        loadedSkinKey = skinKey;
        requestedSkinKey = skinKey;
        replacementSkin = DefaultPlayerSkin.getDefaultSkinLegacy();
        replacementCape = null;
        replacementSkinModel = "default";
        replacementProfile = DEFAULT_STEVE_PROFILE;

        // "Steve" always means Minecraft's built-in classic Steve, never the
        // skin belonging to an account that happens to have that username.
        if ("steve".equals(skinKey)) {
            return;
        }

        Thread skinLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                loadPlayerSkin(skinKey);
            }
        }, "HousingClient-NickHider-Skin");
        skinLoader.setDaemon(true);
        skinLoader.start();
    }

    private static void loadPlayerSkin(final String skinKey) {
        HttpURLConnection connection = null;
        try {
            URL profileUrl = new URL("https://api.mojang.com/users/profiles/minecraft/"
                    + URLEncoder.encode(skinKey, "UTF-8"));
            connection = (HttpURLConnection) profileUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "HousingClient/1.0");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            JsonObject profileJson;
            try (InputStreamReader reader = new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8)) {
                profileJson = new JsonParser().parse(reader).getAsJsonObject();
            }

            if (!profileJson.has("id") || !profileJson.has("name")) {
                return;
            }

            UUID uuid = UUIDTypeAdapter.fromString(profileJson.get("id").getAsString());
            GameProfile profile = new GameProfile(uuid, profileJson.get("name").getAsString());
            Minecraft minecraft = Minecraft.getMinecraft();
            final GameProfile filledProfile = minecraft.getSessionService().fillProfileProperties(profile, false);
            Map<Type, MinecraftProfileTexture> textures = minecraft.getSessionService()
                    .getTextures(filledProfile, false);
            final MinecraftProfileTexture skinTexture = textures.get(Type.SKIN);
            final MinecraftProfileTexture capeTexture = textures.get(Type.CAPE);

            if (!skinKey.equals(requestedSkinKey)) {
                return;
            }

            final String model = skinTexture == null
                    ? DefaultPlayerSkin.getSkinType(uuid)
                    : ("slim".equalsIgnoreCase(skinTexture.getMetadata("model")) ? "slim" : "default");
            final ResourceLocation fallbackSkin = DefaultPlayerSkin.getDefaultSkin(uuid);

            minecraft.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    if (!skinKey.equals(requestedSkinKey) || !isActive()) {
                        return;
                    }

                    replacementProfile = filledProfile;
                    replacementSkin = fallbackSkin;
                    replacementSkinModel = model;

                    if (skinTexture != null) {
                        ResourceLocation location = Minecraft.getMinecraft().getSkinManager().loadSkin(
                                skinTexture,
                                Type.SKIN,
                                new SkinManager.SkinAvailableCallback() {
                                    @Override
                                    public void skinAvailable(Type type, ResourceLocation loadedLocation,
                                            MinecraftProfileTexture profileTexture) {
                                        if (skinKey.equals(requestedSkinKey) && isActive()) {
                                            replacementSkin = loadedLocation;
                                            replacementSkinModel = model;
                                        }
                                    }
                                });

                        // The skin manager renders its safe default fallback at
                        // this location until the download callback completes.
                        replacementSkin = location;
                    }

                    if (capeTexture != null) {
                        Minecraft.getMinecraft().getSkinManager().loadSkin(
                                capeTexture,
                                Type.CAPE,
                                new SkinManager.SkinAvailableCallback() {
                                    @Override
                                    public void skinAvailable(Type type, ResourceLocation loadedLocation,
                                            MinecraftProfileTexture profileTexture) {
                                        if (skinKey.equals(requestedSkinKey) && isActive()) {
                                            replacementCape = loadedLocation;
                                        }
                                    }
                                });
                    }
                }
            });
        } catch (Exception ignored) {
            // Keep the safe built-in Steve fallback for invalid names or network failures.
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void refreshActualSkinFingerprint() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getSession() == null) {
            actualSkinFingerprint = null;
            actualSessionKey = "";
            return;
        }

        GameProfile sessionProfile = minecraft.getSession().getProfile();
        String sessionKey = minecraft.getSession().getUsername() + ":"
                + (sessionProfile == null ? "" : String.valueOf(sessionProfile.getId()));
        if (!sessionKey.equals(actualSessionKey)) {
            actualSessionKey = sessionKey;
            actualSkinFingerprint = null;
        }

        String fingerprint = null;
        if (minecraft.thePlayer != null) {
            fingerprint = getSkinFingerprint(minecraft.thePlayer.getGameProfile());

            if (fingerprint == null && minecraft.getNetHandler() != null) {
                NetworkPlayerInfo playerInfo = minecraft.getNetHandler()
                        .getPlayerInfo(minecraft.thePlayer.getUniqueID());
                if (playerInfo != null) {
                    fingerprint = getSkinFingerprint(playerInfo.getGameProfile());
                }
            }
        }

        if (fingerprint == null) {
            fingerprint = getSkinFingerprint(sessionProfile);
        }

        if (fingerprint != null) {
            actualSkinFingerprint = fingerprint;
        }
    }

    private static String getSkinFingerprint(GameProfile profile) {
        if (profile == null || profile.getProperties() == null) {
            return null;
        }

        Collection<Property> textureProperties = profile.getProperties().get("textures");
        if (textureProperties == null) {
            return null;
        }

        for (Property property : textureProperties) {
            String encodedTextures = property.getValue();
            if (encodedTextures == null || encodedTextures.isEmpty() || encodedTextures.length() > 16384) {
                continue;
            }

            String cached = SKIN_FINGERPRINT_CACHE.get(encodedTextures);
            if (cached != null) {
                return cached;
            }

            try {
                String decoded = new String(Base64.getDecoder().decode(encodedTextures), StandardCharsets.UTF_8);
                JsonObject root = new JsonParser().parse(decoded).getAsJsonObject();
                JsonObject textures = root.getAsJsonObject("textures");
                JsonObject skin = textures == null ? null : textures.getAsJsonObject("SKIN");
                if (skin == null || !skin.has("url")) {
                    continue;
                }

                String url = skin.get("url").getAsString();
                int textureMarker = url.toLowerCase(java.util.Locale.ROOT).lastIndexOf("/texture/");
                String fingerprint = textureMarker >= 0
                        ? url.substring(textureMarker + "/texture/".length())
                        : url;

                if (SKIN_FINGERPRINT_CACHE.size() > 512) {
                    SKIN_FINGERPRINT_CACHE.clear();
                }
                SKIN_FINGERPRINT_CACHE.put(encodedTextures, fingerprint);
                return fingerprint;
            } catch (Exception ignored) {
                // Ignore malformed or non-standard texture properties.
            }
        }

        return null;
    }
}
