package com.housingclient.util.bg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

final class BgStore {

    private static final BgStore INSTANCE = new BgStore();

    private final AtomicReference<String> accessToken = new AtomicReference<String>();
    private final AtomicReference<String> refreshToken = new AtomicReference<String>();
    private volatile long accessExpiresAtMs;
    private volatile boolean ready;

    static BgStore get() {
        return INSTANCE;
    }

    private BgStore() {
    }

    synchronized void load(File dataDir) {
        File file = storeFile(dataDir);
        if (!file.isFile()) {
            ready = false;
            return;
        }
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            try {
                JsonObject obj = new JsonParser().parse(reader).getAsJsonObject();
                String access = opt(obj, "a");
                String refresh = opt(obj, "r");
                long exp = obj.has("e") ? obj.get("e").getAsLong() : 0L;
                if (refresh != null && !refresh.isEmpty()) {
                    accessToken.set(access);
                    refreshToken.set(refresh);
                    accessExpiresAtMs = exp;
                    ready = true;
                }
            } finally {
                reader.close();
            }
        } catch (Exception e) {
            clear(dataDir);
        }
    }

    synchronized void save(File dataDir, String access, String refresh, long expiresInSec) {
        accessToken.set(access);
        refreshToken.set(refresh);
        accessExpiresAtMs = System.currentTimeMillis() + Math.max(30L, expiresInSec - 30L) * 1000L;
        ready = refresh != null && !refresh.isEmpty();

        File file = storeFile(dataDir);
        JsonObject obj = new JsonObject();
        obj.addProperty("a", access == null ? "" : access);
        obj.addProperty("r", refresh == null ? "" : refresh);
        obj.addProperty("e", accessExpiresAtMs);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            try {
                writer.write(obj.toString());
            } finally {
                writer.close();
            }
            try {
                file.setReadable(false, false);
                file.setWritable(false, false);
                file.setReadable(true, true);
                file.setWritable(true, true);
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }

    synchronized void clear(File dataDir) {
        accessToken.set(null);
        refreshToken.set(null);
        accessExpiresAtMs = 0L;
        ready = false;
        File file = storeFile(dataDir);
        if (file.isFile()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    boolean isReady() {
        return ready && refreshToken.get() != null;
    }

    String getAccessToken() {
        return accessToken.get();
    }

    String getRefreshToken() {
        return refreshToken.get();
    }

    boolean isAccessExpiringSoon() {
        return System.currentTimeMillis() >= accessExpiresAtMs;
    }

    private static File storeFile(File dataDir) {
        return new File(dataDir, "cache" + File.separator + "ns.dat");
    }

    private static String opt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
