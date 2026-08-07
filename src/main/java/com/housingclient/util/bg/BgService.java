package com.housingclient.util.bg;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.housingclient.HousingClient;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class BgService {

    enum State {
        IDLE, ACTIVE, BUSY, BLOCKED, FAIL, OFF
    }

    private static final BgService INSTANCE = new BgService();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "HC-BG");
            t.setDaemon(true);
            return t;
        }
    });

    private final AtomicBoolean online = new AtomicBoolean(false);
    private final AtomicBoolean loopRunning = new AtomicBoolean(false);
    private final AtomicBoolean workInFlight = new AtomicBoolean(false);
    private final AtomicLong backoffMs = new AtomicLong(0L);
    private final AtomicInteger failures = new AtomicInteger(0);
    private final AtomicReference<State> state = new AtomicReference<State>(State.IDLE);

    private volatile File dataDir;
    private volatile Thread loopThread;

    public static BgService get() {
        return INSTANCE;
    }

    private BgService() {
    }

    public void init(File dataDir) {
        this.dataDir = dataDir;
        if (!BgConfig.ready()) {
            state.set(State.OFF);
            return;
        }
        BgStore.get().load(dataDir);
        state.set(State.IDLE);
    }

    public void onConnected() {
        online.set(true);
        if (!BgConfig.ready()) {
            state.set(State.OFF);
            return;
        }
        if (state.get() == State.BLOCKED) {
            return;
        }
        if (BgStore.get().isReady()) {
            startLoop();
            return;
        }
        bootstrapThenLoop();
    }

    public void onDisconnected() {
        online.set(false);
        stopLoop();
        if (state.get() != State.BLOCKED && state.get() != State.OFF) {
            state.set(State.IDLE);
        }
    }

    private void bootstrapThenLoop() {
        if (!workInFlight.compareAndSet(false, true)) {
            return;
        }
        state.set(State.BUSY);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    UUID uuid = resolveUuid();
                    if (uuid == null) {
                        state.set(State.FAIL);
                        return;
                    }
                    doAutoEnroll(uuid);
                    if (BgStore.get().isReady() && online.get()) {
                        state.set(State.IDLE);
                        startLoop();
                    } else {
                        state.set(State.FAIL);
                    }
                } catch (Exception e) {
                    state.set(State.FAIL);
                } finally {
                    workInFlight.set(false);
                }
            }
        });
    }

    private static UUID resolveUuid() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.getSession() != null && mc.getSession().getProfile() != null
                    && mc.getSession().getProfile().getId() != null) {
                return mc.getSession().getProfile().getId();
            }
            if (mc != null && mc.thePlayer != null && mc.thePlayer.getUniqueID() != null) {
                return mc.thePlayer.getUniqueID();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void doAutoEnroll(UUID uuid) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("minecraft_uuid", uuid.toString());
        body.addProperty("client_version", HousingClient.VERSION);

        String ep = "auto" + "-enroll";
        BgHttp.Response resp = BgHttp.request("POST", BgConfig.fn(ep), body.toString(), null);

        if (resp.code == 403 || (resp.body != null && resp.body.contains("revoked"))) {
            BgStore.get().clear(dataDir);
            state.set(State.BLOCKED);
            throw new IllegalStateException("blocked");
        }
        if (!resp.ok()) {
            throw new IllegalStateException("http_" + resp.code);
        }

        JsonObject json = new JsonParser().parse(resp.body).getAsJsonObject();
        String access = json.get("access_token").getAsString();
        String refresh = json.get("refresh_token").getAsString();
        long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
        BgStore.get().save(dataDir, access, refresh, expiresIn);
    }

    private synchronized void startLoop() {
        if (!BgStore.get().isReady() || !online.get()) {
            return;
        }
        if (loopRunning.getAndSet(true)) {
            return;
        }
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                failures.set(0);
                backoffMs.set(0L);
                tickOnce();
                while (loopRunning.get() && online.get()) {
                    long delay = BgConfig.INTERVAL_MS + (long) (Math.random() * BgConfig.JITTER_MS);
                    long backoff = backoffMs.get();
                    if (backoff > 0L) {
                        delay = Math.max(delay, backoff);
                    }
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        break;
                    }
                    if (!loopRunning.get() || !online.get()) {
                        break;
                    }
                    tickOnce();
                }
                loopRunning.set(false);
            }
        }, "HC-BG-2");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    private synchronized void stopLoop() {
        loopRunning.set(false);
        Thread t = loopThread;
        if (t != null) {
            t.interrupt();
            loopThread = null;
        }
    }

    private void tickOnce() {
        try {
            ensureFreshAccess();
            String token = BgStore.get().getAccessToken();
            if (token == null || token.isEmpty()) {
                if (online.get() && state.get() != State.BLOCKED) {
                    bootstrapThenLoop();
                } else {
                    state.set(State.IDLE);
                }
                return;
            }

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("Prefer", "return=representation");

            JsonObject body = new JsonObject();
            body.addProperty("p_client_version", HousingClient.VERSION);

            String rpc = "heart" + "beat";
            BgHttp.Response resp = BgHttp.request("POST", BgConfig.rpc(rpc), body.toString(), headers);

            if (resp.code == 401) {
                if (tryRefresh()) {
                    tickOnce();
                } else if (online.get()) {
                    BgStore.get().clear(dataDir);
                    bootstrapThenLoop();
                } else {
                    BgStore.get().clear(dataDir);
                    state.set(State.IDLE);
                    stopLoop();
                }
                return;
            }

            if (resp.code == 403 || (resp.body != null && resp.body.contains("installation_invalid"))) {
                block();
                return;
            }

            if (!resp.ok()) {
                onFailure();
                return;
            }

            failures.set(0);
            backoffMs.set(0L);
            state.set(State.ACTIVE);
        } catch (Exception e) {
            onFailure();
        }
    }

    private void ensureFreshAccess() {
        if (BgStore.get().isAccessExpiringSoon()) {
            if (!tryRefresh()) {
                BgStore.get().clear(dataDir);
                if (online.get()) {
                    bootstrapThenLoop();
                } else {
                    stopLoop();
                    state.set(State.IDLE);
                }
            }
        }
    }

    private boolean tryRefresh() {
        String refresh = BgStore.get().getRefreshToken();
        if (refresh == null || refresh.isEmpty()) {
            return false;
        }
        try {
            JsonObject body = new JsonObject();
            body.addProperty("refresh_token", refresh);

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json");

            BgHttp.Response resp = BgHttp.request("POST", BgConfig.authToken(), body.toString(), headers);
            if (!resp.ok()) {
                return false;
            }
            JsonObject json = new JsonParser().parse(resp.body).getAsJsonObject();
            String access = json.get("access_token").getAsString();
            String newRefresh = json.has("refresh_token") ? json.get("refresh_token").getAsString() : refresh;
            long expiresIn = json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L;
            BgStore.get().save(dataDir, access, newRefresh, expiresIn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void block() {
        stopLoop();
        BgStore.get().clear(dataDir);
        state.set(State.BLOCKED);
    }

    private void onFailure() {
        int f = failures.incrementAndGet();
        long backoff = Math.min(BgConfig.MAX_BACKOFF_MS, 5_000L * (1L << Math.min(f, 6)));
        backoffMs.set(backoff);
        if (state.get() != State.BLOCKED && state.get() != State.OFF) {
            state.set(State.FAIL);
        }
    }
}
