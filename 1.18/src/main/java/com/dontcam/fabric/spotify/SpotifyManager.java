package com.dontcam.fabric.spotify;

import com.dontcam.fabric.DontCamFabricMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SpotifyManager {

    public enum AuthResult {
        OK,
        NO_CLIENT_ID,
        NO_BROWSER
    }

    public static final class Track {
        public final String title;
        public final String artists;
        public final String album;
        public final String coverUrl;
        public final boolean playing;
        public final long progressMs;
        public final long durationMs;
        public final long updatedAt;

        public Track(String title, String artists, String album, String coverUrl,
                     boolean playing, long progressMs, long durationMs, long updatedAt) {
            this.title = title != null ? title : "";
            this.artists = artists != null ? artists : "";
            this.album = album != null ? album : "";
            this.coverUrl = coverUrl != null ? coverUrl : "";
            this.playing = playing;
            this.progressMs = progressMs;
            this.durationMs = durationMs;
            this.updatedAt = updatedAt;
        }
    }

    private static volatile Track current = null;
    private static volatile ScheduledExecutorService scheduler = null;
    private static final Object START_LOCK = new Object();

    private SpotifyManager() {
    }

    public static Track snapshot() {
        try {
            return current;
        } catch (Throwable t) {
            return null;
        }
    }

    public static boolean isLinked() {
        try {
            if (DontCamFabricMod.config == null) {
                return false;
            }
            String r = DontCamFabricMod.config.spotifyRefresh;
            return r != null && !r.isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void ensureStarted() {
        try {
            synchronized (START_LOCK) {
                if (scheduler != null && !scheduler.isShutdown()) {
                    return;
                }
                ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "dontcam-spotify");
                    t.setDaemon(true);
                    return t;
                });
                scheduler = exec;
                exec.scheduleAtFixedRate(() -> {
                    try {
                        poll();
                    } catch (Throwable ignored) {
                    }
                }, 5L, 5L, TimeUnit.SECONDS);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void poll() {
        try {
            if (DontCamFabricMod.config == null) {
                return;
            }
            boolean enabled;
            try {
                enabled = DontCamFabricMod.config.spotifyEnabled;
            } catch (Throwable t) {
                return;
            }
            if (!enabled) {
                return;
            }
            if (!isLinked()) {
                return;
            }
            Track t;
            try {
                t = fetchNowPlaying();
            } catch (Throwable e) {
                t = null;
            }
            current = t;
        } catch (Throwable ignored) {
            try {
                current = null;
            } catch (Throwable ignored2) {
            }
        }
    }

    private static Track fetchNowPlaying() {
        try {
            if (DontCamFabricMod.config == null) {
                return null;
            }
            String access;
            try {
                access = DontCamFabricMod.config.spotifyAccess;
            } catch (Throwable t) {
                return null;
            }
            if (access == null || access.isEmpty()) {
                return null;
            }
            try {
                long expiry = DontCamFabricMod.config.spotifyExpiry;
                if (expiry > 0 && System.currentTimeMillis() >= expiry - 60000L) {
                    boolean ok;
                    try {
                        ok = doRefresh();
                    } catch (Throwable t) {
                        ok = false;
                    }
                    if (ok) {
                        try {
                            access = DontCamFabricMod.config.spotifyAccess;
                        } catch (Throwable t) {
                            return null;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            if (access == null || access.isEmpty()) {
                return null;
            }
            int code;
            String body;
            try {
                HttpURLConnection conn = openGet("https://api.spotify.com/v1/me/player/currently-playing", access);
                code = conn.getResponseCode();
                if (code == 401) {
                    boolean ok;
                    try {
                        ok = doRefresh();
                    } catch (Throwable t) {
                        ok = false;
                    }
                    if (!ok) {
                        return null;
                    }
                    try {
                        access = DontCamFabricMod.config.spotifyAccess;
                    } catch (Throwable t) {
                        return null;
                    }
                    if (access == null || access.isEmpty()) {
                        return null;
                    }
                    conn = openGet("https://api.spotify.com/v1/me/player/currently-playing", access);
                    code = conn.getResponseCode();
                }
                if (code == 204 || code == 404) {
                    return null;
                }
                if (code != 200) {
                    return null;
                }
                body = readBody(conn);
            } catch (Throwable t) {
                return null;
            }
            if (body == null || body.isEmpty()) {
                return null;
            }
            try {
                return parseCurrentlyPlaying(body);
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static Track parseCurrentlyPlaying(String body) {
        try {
            if (body == null || body.isEmpty()) {
                return null;
            }
            JsonElement rootEl = new JsonParser().parse(body);
            if (rootEl == null || !rootEl.isJsonObject()) {
                return null;
            }
            JsonObject root = rootEl.getAsJsonObject();
            JsonElement itemEl = root.has("item") ? root.get("item") : null;
            if (itemEl == null || itemEl.isJsonNull() || !itemEl.isJsonObject()) {
                return null;
            }
            JsonObject item = itemEl.getAsJsonObject();
            String title = optString(item, "name", "");
            long duration = optLong(item, "duration_ms", 0L);
            String artists = "";
            try {
                if (item.has("artists") && item.get("artists") != null && item.get("artists").isJsonArray()) {
                    JsonArray arr = item.getAsJsonArray("artists");
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement e : arr) {
                        try {
                            if (e == null || e.isJsonNull() || !e.isJsonObject()) {
                                continue;
                            }
                            String n = optString(e.getAsJsonObject(), "name", "");
                            if (n == null || n.isEmpty()) {
                                continue;
                            }
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(n);
                        } catch (Throwable ignored) {
                        }
                    }
                    artists = sb.toString();
                }
            } catch (Throwable ignored) {
            }
            String album = "";
            String cover = "";
            try {
                if (item.has("album") && item.get("album") != null && item.get("album").isJsonObject()) {
                    JsonObject alb = item.getAsJsonObject("album");
                    album = optString(alb, "name", "");
                    try {
                        if (alb.has("images") && alb.get("images") != null && alb.get("images").isJsonArray()) {
                            JsonArray imgs = alb.getAsJsonArray("images");
                            if (imgs.size() > 0 && imgs.get(0) != null && imgs.get(0).isJsonObject()) {
                                cover = optString(imgs.get(0).getAsJsonObject(), "url", "");
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            } catch (Throwable ignored) {
            }
            long progress = optLong(root, "progress_ms", 0L);
            boolean playing = false;
            try {
                if (root.has("is_playing") && root.get("is_playing") != null && !root.get("is_playing").isJsonNull()) {
                    playing = root.get("is_playing").getAsBoolean();
                }
            } catch (Throwable ignored) {
            }
            if ((title == null || title.isEmpty()) && (artists == null || artists.isEmpty())) {
                return null;
            }
            return new Track(title, artists, album, cover, playing, progress, duration, System.currentTimeMillis());
        } catch (Throwable t) {
            return null;
        }
    }

    public static AuthResult beginAuth() {
        try {
            String clientId = "";
            try {
                if (DontCamFabricMod.config == null) {
                    return AuthResult.NO_CLIENT_ID;
                }
                clientId = DontCamFabricMod.config.spotifyClientId;
            } catch (Throwable t) {
                return AuthResult.NO_CLIENT_ID;
            }
            if (clientId == null || clientId.isEmpty()) {
                return AuthResult.NO_CLIENT_ID;
            }
            boolean headless = false;
            try {
                headless = java.awt.GraphicsEnvironment.isHeadless();
            } catch (Throwable ignored) {
            }
            if (headless) {
                return AuthResult.NO_BROWSER;
            }
            Desktop desktop = null;
            try {
                if (!Desktop.isDesktopSupported()) {
                    return AuthResult.NO_BROWSER;
                }
                desktop = Desktop.getDesktop();
                if (desktop == null || !desktop.isSupported(Desktop.Action.BROWSE)) {
                    return AuthResult.NO_BROWSER;
                }
            } catch (Throwable t) {
                return AuthResult.NO_BROWSER;
            }

            String verifier;
            String challenge;
            try {
                byte[] rand = new byte[32];
                new SecureRandom().nextBytes(rand);
                verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(rand);
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
                challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } catch (Throwable t) {
                return AuthResult.NO_BROWSER;
            }
            String state;
            try {
                state = UUID.randomUUID().toString().replace("-", "") + Long.toHexString(new SecureRandom().nextLong());
            } catch (Throwable t) {
                state = Long.toHexString(System.nanoTime());
            }

            int port;
            try {
                try (ServerSocket ss = new ServerSocket(0)) {
                    port = ss.getLocalPort();
                }
            } catch (Throwable t) {
                return AuthResult.NO_BROWSER;
            }
            final String redirectUri = "http://127.0.0.1:" + port + "/callback";
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] codeHolder = new String[1];
            final String[] stateHolder = new String[1];
            final String expectedState = state;
            final String verifierFinal = verifier;

            HttpServer server;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            } catch (Throwable t) {
                return AuthResult.NO_BROWSER;
            }
            final HttpServer serverFinal = server;
            try {
                server.createContext("/callback", exchange -> {
                    try {
                        String query = null;
                        try {
                            if (exchange.getRequestURI() != null) {
                                query = exchange.getRequestURI().getRawQuery();
                            }
                        } catch (Throwable ignored) {
                        }
                        String code = null;
                        String gotState = null;
                        try {
                            if (query != null) {
                                for (String kv : query.split("&")) {
                                    int eq = kv.indexOf('=');
                                    if (eq < 0) {
                                        continue;
                                    }
                                    String k = kv.substring(0, eq);
                                    String v = kv.substring(eq + 1);
                                    try {
                                        v = java.net.URLDecoder.decode(v, StandardCharsets.UTF_8.name());
                                    } catch (Throwable ignored) {
                                    }
                                    if ("code".equals(k)) {
                                        code = v;
                                    } else if ("state".equals(k)) {
                                        gotState = v;
                                    }
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                        if (code != null && expectedState.equals(gotState)) {
                            codeHolder[0] = code;
                            stateHolder[0] = gotState;
                        }
                        try {
                            String resp = "<html><body><h3>Spotify polaczony. Wroc do gry.</h3></body></html>";
                            byte[] bytes = resp.getBytes(StandardCharsets.UTF_8);
                            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                            exchange.sendResponseHeaders(200, bytes.length);
                            try (OutputStream os = exchange.getResponseBody()) {
                                os.write(bytes);
                            }
                        } catch (Throwable ignored) {
                        }
                    } catch (Throwable ignored) {
                    } finally {
                        try {
                            latch.countDown();
                        } catch (Throwable ignored) {
                        }
                    }
                });
                server.setExecutor(Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "dontcam-spotify-cb");
                    t.setDaemon(true);
                    return t;
                }));
                server.start();
            } catch (Throwable t) {
                try {
                    server.stop(0);
                } catch (Throwable ignored) {
                }
                return AuthResult.NO_BROWSER;
            }

            String authUrl;
            try {
                authUrl = "https://accounts.spotify.com/authorize"
                        + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8.name())
                        + "&response_type=code"
                        + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name())
                        + "&scope=" + URLEncoder.encode("user-read-currently-playing user-read-playback-state", StandardCharsets.UTF_8.name())
                        + "&code_challenge_method=S256"
                        + "&code_challenge=" + URLEncoder.encode(challenge, StandardCharsets.UTF_8.name())
                        + "&state=" + URLEncoder.encode(expectedState, StandardCharsets.UTF_8.name());
            } catch (Throwable t) {
                try {
                    serverFinal.stop(0);
                } catch (Throwable ignored) {
                }
                return AuthResult.NO_BROWSER;
            }
            try {
                desktop.browse(new URI(authUrl));
            } catch (Throwable t) {
                try {
                    serverFinal.stop(0);
                } catch (Throwable ignored) {
                }
                return AuthResult.NO_BROWSER;
            }

            Thread worker = new Thread(() -> {
                try {
                    boolean done = false;
                    try {
                        done = latch.await(300L, TimeUnit.SECONDS);
                    } catch (Throwable ignored) {
                    }
                    try {
                        serverFinal.stop(0);
                    } catch (Throwable ignored) {
                    }
                    if (!done) {
                        return;
                    }
                    String code = codeHolder[0];
                    if (code == null || code.isEmpty()) {
                        return;
                    }
                    try {
                        exchangeCode(code, verifierFinal, redirectUri);
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable ignored) {
                }
            }, "dontcam-spotify-auth");
            worker.setDaemon(true);
            worker.start();
            return AuthResult.OK;
        } catch (Throwable t) {
            return AuthResult.NO_BROWSER;
        }
    }

    private static void exchangeCode(String code, String verifier, String redirectUri) {
        try {
            if (code == null || code.isEmpty() || verifier == null || redirectUri == null) {
                return;
            }
            String clientId = "";
            String clientSecret = "";
            try {
                if (DontCamFabricMod.config == null) {
                    return;
                }
                clientId = DontCamFabricMod.config.spotifyClientId;
                clientSecret = DontCamFabricMod.config.spotifyClientSecret;
            } catch (Throwable t) {
                return;
            }
            if (clientId == null || clientId.isEmpty()) {
                return;
            }
            StringBuilder body = new StringBuilder();
            body.append("grant_type=").append(URLEncoder.encode("authorization_code", StandardCharsets.UTF_8.name()));
            body.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8.name()));
            body.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.name()));
            body.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.name()));
            body.append("&code_verifier=").append(URLEncoder.encode(verifier, StandardCharsets.UTF_8.name()));
            if (clientSecret != null && !clientSecret.isEmpty()) {
                body.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8.name()));
            }
            String resp = postForm("https://accounts.spotify.com/api/token", body.toString());
            if (resp == null || resp.isEmpty()) {
                return;
            }
            storeTokenResponse(resp);
        } catch (Throwable ignored) {
        }
    }

    public static synchronized boolean doRefresh() {
        try {
            if (DontCamFabricMod.config == null) {
                return false;
            }
            String clientId = "";
            String clientSecret = "";
            String refresh = "";
            try {
                clientId = DontCamFabricMod.config.spotifyClientId;
                clientSecret = DontCamFabricMod.config.spotifyClientSecret;
                refresh = DontCamFabricMod.config.spotifyRefresh;
            } catch (Throwable t) {
                return false;
            }
            if (clientId == null || clientId.isEmpty() || refresh == null || refresh.isEmpty()) {
                return false;
            }
            StringBuilder body = new StringBuilder();
            try {
                body.append("grant_type=").append(URLEncoder.encode("refresh_token", StandardCharsets.UTF_8.name()));
                body.append("&refresh_token=").append(URLEncoder.encode(refresh, StandardCharsets.UTF_8.name()));
                body.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.name()));
                if (clientSecret != null && !clientSecret.isEmpty()) {
                    body.append("&client_secret=").append(URLEncoder.encode(clientSecret, StandardCharsets.UTF_8.name()));
                }
            } catch (Throwable t) {
                return false;
            }
            String resp;
            try {
                resp = postForm("https://accounts.spotify.com/api/token", body.toString());
            } catch (Throwable t) {
                return false;
            }
            if (resp == null || resp.isEmpty()) {
                clearTokens();
                return false;
            }
            boolean ok = storeTokenResponse(resp);
            if (!ok) {
                clearTokens();
                return false;
            }
            return true;
        } catch (Throwable t) {
            try {
                clearTokens();
            } catch (Throwable ignored) {
            }
            return false;
        }
    }

    private static boolean storeTokenResponse(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return false;
            }
            JsonElement el = new JsonParser().parse(json);
            if (el == null || !el.isJsonObject()) {
                return false;
            }
            JsonObject obj = el.getAsJsonObject();
            String access = optString(obj, "access_token", "");
            if (access == null || access.isEmpty()) {
                return false;
            }
            String refresh = optString(obj, "refresh_token", "");
            long expiresIn = optLong(obj, "expires_in", 3600L);
            long expiry = System.currentTimeMillis() + Math.max(0L, expiresIn) * 1000L;
            try {
                if (DontCamFabricMod.config == null) {
                    return false;
                }
                DontCamFabricMod.config.spotifyAccess = access;
                if (refresh != null && !refresh.isEmpty()) {
                    DontCamFabricMod.config.spotifyRefresh = refresh;
                }
                DontCamFabricMod.config.spotifyExpiry = expiry;
                DontCamFabricMod.config.save();
            } catch (Throwable t) {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void clearTokens() {
        try {
            if (DontCamFabricMod.config == null) {
                return;
            }
            try {
                DontCamFabricMod.config.spotifyAccess = "";
                DontCamFabricMod.config.spotifyRefresh = "";
                DontCamFabricMod.config.spotifyExpiry = 0L;
                DontCamFabricMod.config.save();
            } catch (Throwable ignored) {
            }
            try {
                current = null;
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    public static void disconnect() {
        try {
            clearTokens();
        } catch (Throwable ignored) {
        }
    }

    private static HttpURLConnection openGet(String urlStr, String access) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Authorization", "Bearer " + access);
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoInput(true);
        return conn;
    }

    private static String postForm(String urlStr, String formBody) throws Exception {
        byte[] data = formBody.getBytes(StandardCharsets.UTF_8);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setFixedLengthStreamingMode(data.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(data);
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            return "";
        }
        return readBody(conn);
    }

    private static String readBody(HttpURLConnection conn) {
        try {
            InputStream in;
            try {
                in = conn.getInputStream();
            } catch (Throwable t) {
                in = conn.getErrorStream();
            }
            if (in == null) {
                return "";
            }
            try (InputStream autoClose = in; ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = autoClose.read(buf)) >= 0) {
                    bos.write(buf, 0, n);
                }
                return new String(bos.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            return "";
        }
    }

    private static String optString(JsonObject obj, String member, String def) {
        try {
            if (obj == null || member == null || !obj.has(member)) {
                return def;
            }
            JsonElement e = obj.get(member);
            if (e == null || e.isJsonNull()) {
                return def;
            }
            try {
                return e.getAsString();
            } catch (Throwable t) {
                return def;
            }
        } catch (Throwable t) {
            return def;
        }
    }

    private static long optLong(JsonObject obj, String member, long def) {
        try {
            if (obj == null || member == null || !obj.has(member)) {
                return def;
            }
            JsonElement e = obj.get(member);
            if (e == null || e.isJsonNull()) {
                return def;
            }
            try {
                return e.getAsLong();
            } catch (Throwable t) {
                return def;
            }
        } catch (Throwable t) {
            return def;
        }
    }
}
