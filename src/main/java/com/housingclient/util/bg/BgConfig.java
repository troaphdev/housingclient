package com.housingclient.util.bg;

final class BgConfig {

    static final String BASE = "https://kaqjpenxyahppfmyngss.supabase.co";
    static final String KEY = "sb_publishable_GcNZkG0tcjFF2C0RKZOtbQ_5w6kzkzD";

    static final long INTERVAL_MS = 90_000L;
    static final long JITTER_MS = 15_000L;
    static final int CONNECT_TIMEOUT_MS = 8_000;
    static final int READ_TIMEOUT_MS = 8_000;
    static final int MAX_BACKOFF_MS = 300_000;

    private BgConfig() {
    }

    static boolean ready() {
        return BASE != null && BASE.startsWith("https://") && KEY != null && KEY.length() > 20;
    }

    static String fn(String name) {
        return trim(BASE) + "/functions/v1/" + name;
    }

    static String rpc(String name) {
        return trim(BASE) + "/rest/v1/rpc/" + name;
    }

    static String authToken() {
        return trim(BASE) + "/auth/v1/token?grant_type=refresh_token";
    }

    private static String trim(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
