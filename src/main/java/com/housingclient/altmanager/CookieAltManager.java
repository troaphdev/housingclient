package com.housingclient.altmanager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cookie Alt Manager
 * 
 * Handles parsing cookie files and logging into Microsoft accounts.
 */
public class CookieAltManager {

    public static class AltAccount {
        public String username;
        public String uuid;
        public Map<String, String> cookies;
        public boolean valid = true;

        public AltAccount(String username, String uuid, Map<String, String> cookies) {
            this.username = username;
            this.uuid = uuid;
            this.cookies = cookies;
        }

        public String getUsername() {
            return username;
        }

        public String getUuid() {
            return uuid;
        }

        public Map<String, String> getCookies() {
            return cookies;
        }
    }

    public enum Status {
        IDLE("\u00A77Idle"),
        PARSING("\u00A7eParsing cookies..."),
        OAUTH("\u00A7eMicrosoft OAuth..."),
        XBL("\u00A7eXbox Live..."),
        XSTS("\u00A7eXSTS Auth..."),
        MC_AUTH("\u00A7eMinecraft Auth..."),
        PROFILE("\u00A7eLoading profile..."),
        SUCCESS("\u00A7aSuccess!"),
        FAILED("\u00A7cFailed!");

        private final String message;

        Status(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static CookieAltManager instance;
    private final Minecraft mc = Minecraft.getMinecraft();
    private String lastError = "";
    private Status status = Status.IDLE;
    private final List<AltAccount> accounts = new ArrayList<>();
    private final File altsFile;
    private Map<String, String> sessionCookies = new HashMap<>();

    public CookieAltManager() {
        this.altsFile = new File(new File(mc.mcDataDir, "housingclient"), "alts.json");
        load();
    }

    public static CookieAltManager getInstance() {
        if (instance == null) {
            instance = new CookieAltManager();
        }
        return instance;
    }

    public Map<String, String> parseCookieFile(File file) {
        Map<String, String> cookies = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                if (line.contains("\t")) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 7) {
                        String name = parts[5].trim();
                        String value = parts[6].trim();
                        if (!name.isEmpty()) {
                            cookies.put(name, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            lastError = "Failed to read file: " + e.getMessage();
            e.printStackTrace();
        }

        return cookies;
    }

    private String buildCookieHeader(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (sb.length() > 0)
                sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    private void updateCookiesFromResponse(HttpURLConnection conn) {
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies != null) {
            for (String cookie : setCookies) {
                String[] parts = cookie.split(";")[0].split("=", 2);
                if (parts.length == 2) {
                    sessionCookies.put(parts[0], parts[1]);
                }
            }
        }
    }

    public boolean addAccountFromCookies(File cookieFile) {
        try {
            status = Status.PARSING;
            Map<String, String> cookies = parseCookieFile(cookieFile);
            sessionCookies = new HashMap<>(cookies);

            if (cookies.isEmpty()) {
                status = Status.FAILED;
                lastError = "No cookies parsed!";
                return false;
            }

            System.out.println("[CookieAlt] Found " + cookies.size() + " cookies");

            // Step 1: Get Microsoft OAuth token using cookies
            status = Status.OAUTH;
            String msToken = getMicrosoftToken();
            if (msToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got MS token (length: " + msToken.length() + ")");

            // Step 2: Get Xbox Live token
            status = Status.XBL;
            String xblToken = getXboxLiveToken(msToken);
            if (xblToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got XBL token");

            // Step 3: Get XSTS token
            status = Status.XSTS;
            String[] xstsData = getXSTSToken(xblToken);
            if (xstsData == null) {
                status = Status.FAILED;
                return false;
            }
            String xstsToken = xstsData[0];
            String userHash = xstsData[1];
            System.out.println("[CookieAlt] Got XSTS token");

            // Step 4: Get Minecraft token
            status = Status.MC_AUTH;
            String accessToken = getMinecraftToken(xstsToken, userHash);
            if (accessToken == null) {
                status = Status.FAILED;
                return false;
            }
            System.out.println("[CookieAlt] Got MC token");

            // Step 5: Get profile
            status = Status.PROFILE;
            String[] profileData = getMinecraftProfile(accessToken);
            if (profileData == null) {
                status = Status.FAILED;
                return false;
            }
            String uuid = profileData[0];
            String username = profileData[1];
            System.out.println("[CookieAlt] Profile: " + username);

            // Save account
            AltAccount account = new AltAccount(username, uuid, sessionCookies);
            addAccount(account);

            status = Status.SUCCESS;
            return true;

        } catch (Exception e) {
            status = Status.FAILED;
            lastError = "Error: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    private String getMicrosoftToken() {
        try {
            // Step 1: Initial OAuth request
            String oauthUrl = "https://login.live.com/oauth20_authorize.srf?client_id=000000004C12AE6F&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL&response_type=token";

            HttpURLConnection conn = createConnection(oauthUrl, "GET");
            int code = conn.getResponseCode();
            updateCookiesFromResponse(conn);

            String location = conn.getHeaderField("Location");
            System.out.println("[CookieAlt] OAuth initial: " + code);

            // Check if we got redirected directly to token
            if (location != null && location.contains("access_token=")) {
                System.out.println("[CookieAlt] Direct token redirect!");
                String token = extractFromUrl(location, "access_token");
                String decoded = java.net.URLDecoder.decode(token, "UTF-8");
                System.out.println("[CookieAlt] Extracted access_token. Starts with: "
                        + (decoded.length() > 10 ? decoded.substring(0, 10) : decoded));
                return decoded;
            }

            // Follow redirect if 302
            if (code == 302 && location != null) {
                System.out.println("[CookieAlt] Following redirect...");
                conn = createConnection(location, "GET");
                code = conn.getResponseCode();
                updateCookiesFromResponse(conn);
                location = conn.getHeaderField("Location");

                if (location != null && location.contains("access_token=")) {
                    return extractFromUrl(location, "access_token");
                }
            }

            // 200 - need to parse and submit form
            if (code == 200) {
                String body = readResponse(conn);
                System.out.println("[CookieAlt] Got form page, length: " + body.length());

                // Extract all input fields (hidden, etc)
                Map<String, String> inputs = extractAllHiddenInputs(body);
                System.out.println("[CookieAlt] Extracted " + inputs.size() + " form inputs");

                // Debug inputs
                for (String key : inputs.keySet()) {
                    if (key.equals("PPFT")) {
                        System.out.println("[CookieAlt] Found PPFT: " + inputs.get(key).length() + " chars");
                    } else {
                        // Print other keys (values might be sensitive or long, so just keys)
                        System.out.println("[CookieAlt] Input: " + key);
                    }
                }

                // Extract urlPost using regex
                String postUrl = extractWithRegex(body, "urlPost\"?\\s*[:\"]\\s*\"?(https?://[^\"']+)");
                if (postUrl == null) {
                    postUrl = extractWithRegex(body, "urlPost\\s*[:=]\\s*['\"]([^'\"]+)");
                }

                if (postUrl == null || !postUrl.startsWith("http")) {
                    lastError = "No form POST URL found";
                    return null;
                }

                System.out.println("[CookieAlt] Post URL: " + postUrl.substring(0, Math.min(60, postUrl.length())));

                // Submit the form
                return submitForm(postUrl, inputs, 0);
            }

            lastError = "OAuth failed: " + code;
            return null;

        } catch (Exception e) {
            lastError = "OAuth error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, String> extractAllHiddenInputs(String html) {
        Map<String, String> inputs = new HashMap<>();

        // 1. Regex for standard HTML input tags
        Pattern inputPattern = Pattern.compile("<input[^>]+>", Pattern.CASE_INSENSITIVE);
        Matcher m = inputPattern.matcher(html);
        while (m.find()) {
            String tag = m.group();
            String name = extractWithRegex(tag, "name=['\"]?([^'\"]+)['\"]?");
            String value = extractWithRegex(tag, "value=['\"]?([^'\"]*)['\"]?");

            if (name != null) {
                inputs.put(name, value != null ? value : "");
            }
        }

        // 2. Extract JSON escaped inputs (sFTTag)
        // Format: sFTTag":"<input ...>"
        // Capture strictly the content inside quotes after sFTTag":
        String sftTag = extractWithRegex(html, "sFTTag\\\"?\\s*:\\s*\\\"((?:[^\\\\\\\"]|\\\\.)*)\\\"");
        if (sftTag != null) {
            // Unescape
            String unescaped = sftTag.replace("\\\"", "\"").replace("\\/", "/");
            Matcher m2 = inputPattern.matcher(unescaped);
            while (m2.find()) {
                String tag = m2.group();
                String name = extractWithRegex(tag, "name=['\"]?([^'\"]+)['\"]?");
                String value = extractWithRegex(tag, "value=['\"]?([^'\"]*)['\"]?");
                if (name != null) {
                    inputs.put(name, value != null ? value : "");
                }
            }
        }
        return inputs;
    }

    private String submitForm(String postUrl, Map<String, String> inputs, int depth) {
        if (depth > 5) {
            System.out.println("[CookieAlt] Max form recursion reached aborting.");
            lastError = "Max form recursion reached";
            return null;
        }

        try {
            System.out
                    .println("[CookieAlt] Submitting form (depth " + depth + ") with " + inputs.size() + " params...");

            HttpURLConnection conn = createConnection(postUrl, "POST");
            conn.setDoOutput(true);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                if (postData.length() > 0)
                    postData.append("&");
                postData.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"));
                postData.append("=");
                postData.append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            byte[] postBytes = postData.toString().getBytes("UTF-8");
            conn.setRequestProperty("Content-Length", String.valueOf(postBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(postBytes);
            }

            int code = conn.getResponseCode();
            updateCookiesFromResponse(conn);
            String location = conn.getHeaderField("Location");

            System.out.println("[CookieAlt] Form POST code: " + code);

            // Check for token in redirect
            if (location != null) {
                System.out.println("[CookieAlt] Redirect: " + location.substring(0, Math.min(80, location.length())));
                if (location.contains("access_token=")) {
                    return extractFromUrl(location, "access_token");
                }

                // Follow redirect
                if (code == 302) {
                    return followRedirectChain(location, 5);
                }
            }

            // If 200, check body for token or another form
            if (code == 200) {
                String body = readResponse(conn);

                // Check for access_token in body
                String token = extractValue(body, "access_token", "=", "&");
                if (token != null) {
                    return java.net.URLDecoder.decode(token, "UTF-8");
                }

                // Check for another form
                String newPostUrl = extractWithRegex(body, "urlPost\"?\\s*[:\"]\\s*\"?(https?://[^\"']+)");
                if (newPostUrl == null) {
                    newPostUrl = extractWithRegex(body, "urlPost\\s*[:=]\\s*['\"]([^'\"]+)");
                }

                if (newPostUrl != null && newPostUrl.startsWith("http")) {
                    // Prevent infinite loops: if new URL is same as old URL, and we just posted,
                    // assume failure unless we have new inputs
                    if (newPostUrl.equals(postUrl)) {
                        System.out.println("[CookieAlt] Loop detected: new form has same URL as previous POST.");
                        // Check for password field
                        if (body.contains("name=\"passwd\"") || body.contains("type=\"password\"")) {
                        }

                        lastError = "Login requires password - cookies may be expired";
                        return null;
                    }

                    System.out.println("[CookieAlt] Found another form, submitting...");
                    Map<String, String> newInputs = extractAllHiddenInputs(body);
                    return submitForm(newPostUrl, newInputs, depth + 1);
                }

                // Debug
                int idx = body.indexOf("access_token");
                if (idx == -1)
                    idx = body.indexOf("error");
                if (idx == -1)
                    idx = 0;
                System.out.println(
                        "[CookieAlt] Body snippet: " + body.substring(idx, Math.min(idx + 150, body.length())));
            }

            lastError = "Form submission failed: " + code;
            return null;

        } catch (Exception e) {
            lastError = "Form error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String followRedirectChain(String url, int maxRedirects) {
        try {
            for (int i = 0; i < maxRedirects; i++) {
                if (url.contains("access_token=")) {
                    return extractFromUrl(url, "access_token");
                }

                HttpURLConnection conn = createConnection(url, "GET");
                int code = conn.getResponseCode();
                updateCookiesFromResponse(conn);
                String location = conn.getHeaderField("Location");

                System.out.println("[CookieAlt] Redirect " + (i + 1) + ": " + code);

                if (location == null) {
                    if (code == 200) {
                        String body = readResponse(conn);
                        String token = extractValue(body, "access_token", "=", "&");
                        if (token != null) {
                            return java.net.URLDecoder.decode(token, "UTF-8");
                        }
                    }
                    break;
                }

                url = location;
            }
            lastError = "Redirect chain exhausted without token";
            return null;
        } catch (Exception e) {
            lastError = "Redirect error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private HttpURLConnection createConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Cookie", buildCookieHeader(sessionCookies));
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private String extractValue(String text, String marker, String start, String end) {
        int markerIdx = text.indexOf(marker);
        if (markerIdx == -1)
            return null;

        int startIdx = text.indexOf(start, markerIdx + marker.length());
        if (startIdx == -1)
            return null;
        startIdx += start.length();

        int endIdx = text.indexOf(end, startIdx);
        if (endIdx == -1)
            return null;

        return text.substring(startIdx, endIdx);
    }

    private String extractWithRegex(String text, String pattern) {
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractFromUrl(String url, String param) {
        try {
            int start = url.indexOf(param + "=");
            if (start == -1)
                return null;
            start += param.length() + 1;

            int end = url.indexOf("&", start);
            if (end == -1)
                end = url.indexOf("#", start);
            if (end == -1)
                end = url.length();

            return java.net.URLDecoder.decode(url.substring(start, end), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    private String getXboxLiveToken(String msToken) {
        try {
            URL url = new URL("https://user.auth.xboxlive.com/user/authenticate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Try t= prefix (Legacy/Compact Ticket format which Ew tokens often are)
            // If d= failed, t= is the next logical attempt.
            String rpsTicket = "t=" + msToken;

            JsonObject properties = new JsonObject();
            properties.addProperty("AuthMethod", "RPS");
            properties.addProperty("SiteName", "user.auth.xboxlive.com");
            properties.addProperty("RpsTicket", rpsTicket);

            JsonObject reqCtx = new JsonObject();
            reqCtx.add("Properties", properties);
            reqCtx.addProperty("RelyingParty", "http://auth.xboxlive.com");
            reqCtx.addProperty("TokenType", "JWT");

            String body = reqCtx.toString();
            byte[] bodyBytes = body.getBytes("UTF-8");

            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            // Simple User-Agent
            conn.setRequestProperty("User-Agent", "HousingClient/1.0");

            System.out.println("[CookieAlt] Sending XBL Body: " + body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }

            int code = conn.getResponseCode();
            String msg = conn.getResponseMessage();
            String response = readResponse(conn);

            if (code != 200) {
                System.out.println("[CookieAlt] XBL failed: " + code + " " + msg);
                System.out.println("[CookieAlt] XBL Response Body: " + response);
                lastError = "XBL failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            return json.get("Token").getAsString();

        } catch (Exception e) {
            lastError = "XBL error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String[] getXSTSToken(String xblToken) {
        try {
            URL url = new URL("https://xsts.auth.xboxlive.com/xsts/authorize");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String body = "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken
                    + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "XSTS failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            String token = json.get("Token").getAsString();
            String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs")
                    .getAsString();
            return new String[] { token, uhs };

        } catch (Exception e) {
            lastError = "XSTS error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String getMinecraftToken(String xstsToken, String userHash) {
        try {
            URL url = new URL("https://api.minecraftservices.com/authentication/login_with_xbox");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String body = "{\"identityToken\":\"XBL3.0 x=" + userHash + ";" + xstsToken + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "MC auth failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            return json.get("access_token").getAsString();

        } catch (Exception e) {
            lastError = "MC token error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String[] getMinecraftProfile(String accessToken) {
        try {
            URL url = new URL("https://api.minecraftservices.com/minecraft/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int code = conn.getResponseCode();
            String response = readResponse(conn);

            if (code != 200) {
                lastError = "Profile failed (" + code + ")";
                return null;
            }

            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            String uuid = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            return new String[] { uuid, name };

        } catch (Exception e) {
            lastError = "Profile error: " + e.getMessage();
            e.printStackTrace();
            return null;
        }
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        InputStream is;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            System.out.println("[CookieAlt] IOException reading InputStream (expected for 400+): " + e.getMessage());
            is = conn.getErrorStream();
            if (is == null) {
                System.out.println("[CookieAlt] ErrorStream is NULL!");
            } else {
                System.out.println("[CookieAlt] Got ErrorStream");
            }
        }
        if (is == null)
            return "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private void setSession(Session session) {
        try {
            java.lang.reflect.Field sessionField = null;
            for (java.lang.reflect.Field f : Minecraft.class.getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    sessionField = f;
                    break;
                }
            }

            if (sessionField != null) {
                sessionField.setAccessible(true);
                sessionField.set(mc, session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCurrentUsername() {
        return mc.getSession().getUsername();
    }

    public void pickFileAndLogin() {
        new Thread(() -> {
            try {
                // Use AWT FileDialog which is native MacOS compliant
                java.awt.Frame parent = null;
                try {
                } catch (Exception e) {
                }

                java.awt.FileDialog dialog = new java.awt.FileDialog(parent, "Select Cookie File",
                        java.awt.FileDialog.LOAD);
                dialog.setFile("*.txt");
                dialog.setVisible(true);

                String dir = dialog.getDirectory();
                String file = dialog.getFile();

                if (dir != null && file != null) {
                    File selectedFile = new File(dir, file);
                    System.out.println("[CookieAlt] Selected: " + selectedFile.getAbsolutePath());

                    mc.addScheduledTask(() -> {
                        if (mc.thePlayer != null)
                            mc.thePlayer.addChatMessage(
                                    new net.minecraft.util.ChatComponentText("\u00A7e[CookieAlt] Logging in..."));
                    });

                    boolean success = addAccountFromCookies(selectedFile);

                    mc.addScheduledTask(() -> {
                        if (success) {
                            String msg = "\u00A7a[CookieAlt] Login successful! Logged in as: " + getCurrentUsername();
                            System.out.println(msg);
                            if (mc.thePlayer != null)
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
                        } else {
                            String msg = "\u00A7c[CookieAlt] Login failed: " + getLastError();
                            System.out.println(msg);
                            if (mc.thePlayer != null)
                                mc.thePlayer.addChatMessage(new net.minecraft.util.ChatComponentText(msg));
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String getLastError() {
        return lastError != null ? lastError : "";
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        if (status != Status.FAILED) {
            this.lastError = "";
        }
    }

    public void setFailed(String message) {
        this.status = Status.FAILED;
        this.lastError = message;
    }

    public List<AltAccount> getAccounts() {
        return accounts;
    }

    public void addAccount(AltAccount account) {
        // Remove if exists
        accounts.removeIf(a -> a.uuid.equalsIgnoreCase(account.uuid));
        accounts.add(account);
        save();
    }

    public void removeAccount(AltAccount account) {
        accounts.remove(account);
        save();
    }

    public void save() {
        try {
            if (!altsFile.getParentFile().exists()) {
                altsFile.getParentFile().mkdirs();
            }
            JsonObject json = new JsonObject();
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();
            for (AltAccount acc : accounts) {
                JsonObject accJson = new JsonObject();
                accJson.addProperty("username", acc.username);
                accJson.addProperty("uuid", acc.uuid);
                accJson.addProperty("valid", acc.valid);

                JsonObject cookieJson = new JsonObject();
                for (Map.Entry<String, String> entry : acc.cookies.entrySet()) {
                    cookieJson.addProperty(entry.getKey(), entry.getValue());
                }
                accJson.add("cookies", cookieJson);
                array.add(accJson);
            }
            json.add("accounts", array);

            try (FileWriter writer = new FileWriter(altsFile)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        if (!altsFile.exists())
            return;
        try (Reader reader = new FileReader(altsFile)) {
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if (json.has("accounts")) {
                com.google.gson.JsonArray array = json.getAsJsonArray("accounts");
                accounts.clear();
                for (com.google.gson.JsonElement el : array) {
                    JsonObject accJson = el.getAsJsonObject();
                    String username = accJson.get("username").getAsString();
                    String uuid = accJson.has("uuid") ? accJson.get("uuid").getAsString() : "";
                    boolean valid = accJson.has("valid") ? accJson.get("valid").getAsBoolean() : true;

                    Map<String, String> cookies = new HashMap<>();
                    JsonObject cookieJson = accJson.getAsJsonObject("cookies");
                    for (Map.Entry<String, com.google.gson.JsonElement> entry : cookieJson.entrySet()) {
                        cookies.put(entry.getKey(), entry.getValue().getAsString());
                    }
                    AltAccount acc = new AltAccount(username, uuid, cookies);
                    acc.valid = valid;
                    accounts.add(acc);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loginWithAccount(AltAccount account) {
        status = Status.IDLE;
        sessionCookies = new HashMap<>(account.cookies);

        try {
            // Step 1: Get Microsoft OAuth token using cookies
            status = Status.OAUTH;
            String msToken = getMicrosoftToken();
            if (msToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

            // Step 2: Get Xbox Live token
            status = Status.XBL;
            String xblToken = getXboxLiveToken(msToken);
            if (xblToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

            // Step 3: Get XSTS token
            status = Status.XSTS;
            String[] xstsData = getXSTSToken(xblToken);
            if (xstsData == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }
            String xstsToken = xstsData[0];
            String userHash = xstsData[1];

            // Step 4: Get Minecraft token
            status = Status.MC_AUTH;
            String accessToken = getMinecraftToken(xstsToken, userHash);
            if (accessToken == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }

            // Step 5: Get profile
            status = Status.PROFILE;
            String[] profileData = getMinecraftProfile(accessToken);
            if (profileData == null) {
                status = Status.FAILED;
                account.valid = false;
                save();
                return false;
            }
            String uuid = profileData[0];
            String username = profileData[1];

            // Step 6: Set session
            Session session = new Session(username, uuid, accessToken, "mojang");
            setSession(session);

            // Update account info
            account.username = username;
            account.uuid = uuid;
            account.cookies = new HashMap<>(sessionCookies);
            account.valid = true;
            save();

            status = Status.SUCCESS;
            return true;

        } catch (Exception e) {
            status = Status.FAILED;
            account.valid = false;
            save();
            lastError = "Error: " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

}
