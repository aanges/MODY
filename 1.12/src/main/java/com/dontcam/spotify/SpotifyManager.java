package com.dontcam.spotify;

import com.dontcam.DontCamMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SpotifyManager {

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

    public static Track snapshot() {
        return current;
    }

    public static boolean isLinked() {
        try {
            Object cfg = DontCamMod.config;
            if (cfg == null) {
                return false;
            }
            synchronized (cfg) {
                String refresh = DontCamMod.config.spotifyRefresh;
                return refresh != null && !refresh.isEmpty();
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private static final ScheduledExecutorService POOL = Executors.newSingleThreadScheduledExecutor(new java.util.concurrent.ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "DontCam-Spotify");
            t.setDaemon(true);
            return t;
        }
    });

    private static final AtomicBoolean started = new AtomicBoolean(false);

    public static void ensureStarted() {
        try {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            POOL.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    poll();
                }
            }, 0L, 5L, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static void poll() {
        try {
            if (DontCamMod.config == null) {
                return;
            }
            boolean enabled;
            boolean linked;
            String access;
            long expiry;
            synchronized (DontCamMod.config) {
                enabled = DontCamMod.config.spotifyEnabled;
                String refresh = DontCamMod.config.spotifyRefresh;
                linked = refresh != null && !refresh.isEmpty();
                access = DontCamMod.config.spotifyAccess;
                expiry = DontCamMod.config.spotifyExpiry;
            }
            if (!enabled || !linked) {
                return;
            }
            if (access == null || access.isEmpty()) {
                doRefresh();
                synchronized (DontCamMod.config) {
                    access = DontCamMod.config.spotifyAccess;
                }
                if (access == null || access.isEmpty()) {
                    return;
                }
            } else if (expiry > 0 && System.currentTimeMillis() > expiry - 30000L) {
                doRefresh();
                synchronized (DontCamMod.config) {
                    access = DontCamMod.config.spotifyAccess;
                }
                if (access == null || access.isEmpty()) {
                    return;
                }
            }
            int code = fetchCurrent(access, false);
            if (code == 401) {
                doRefresh();
                String access2;
                synchronized (DontCamMod.config) {
                    access2 = DontCamMod.config.spotifyAccess;
                }
                if (access2 != null && !access2.isEmpty() && !access2.equals(access)) {
                    fetchCurrent(access2, true);
                } else if (access2 != null && !access2.isEmpty()) {
                    fetchCurrent(access2, true);
                }
            }
        } catch (Exception ignored) {
            try {
                current = null;
            } catch (Exception ignored2) {
            }
        }
    }

    private static int fetchCurrent(String access, boolean isRetry) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://api.spotify.com/v1/me/player/currently-playing");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Authorization", "Bearer " + access);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code == 204 || code == 404) {
                current = null;
                return code;
            }
            if (code == 401) {
                return 401;
            }
            if (code != 200) {
                try {
                    InputStream err = conn.getErrorStream();
                    if (err != null) {
                        err.close();
                    }
                } catch (Exception ignored) {
                }
                current = null;
                return code;
            }
            String body = readAll(conn.getInputStream());
            if (body == null || body.trim().isEmpty()) {
                current = null;
                return 200;
            }
            Track t = parseTrack(body);
            if (t == null) {
                current = null;
            } else {
                current = t;
            }
            return 200;
        } catch (Exception ignored) {
            current = null;
            return -1;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Track parseTrack(String body) {
        try {
            JsonElement el = new JsonParser().parse(body);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            JsonObject root = el.getAsJsonObject();
            if (!root.has("item") || root.get("item") == null || root.get("item").isJsonNull()) {
                return null;
            }
            JsonObject item = root.getAsJsonObject("item");
            String title = getStr(item, "name", "");
            long duration = getLong(item, "duration_ms", 0L);
            long progress = 0L;
            if (root.has("progress_ms") && !root.get("progress_ms").isJsonNull()) {
                try {
                    progress = root.get("progress_ms").getAsLong();
                } catch (Exception ignored) {
                }
            }
            boolean playing = false;
            if (root.has("is_playing") && !root.get("is_playing").isJsonNull()) {
                try {
                    playing = root.get("is_playing").getAsBoolean();
                } catch (Exception ignored) {
                }
            }
            String artists = "";
            try {
                if (item.has("artists") && item.get("artists").isJsonArray()) {
                    JsonArray arr = item.getAsJsonArray("artists");
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement a : arr) {
                        if (a == null || !a.isJsonObject()) {
                            continue;
                        }
                        String n = getStr(a.getAsJsonObject(), "name", "");
                        if (n.isEmpty()) {
                            continue;
                        }
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(n);
                    }
                    artists = sb.toString();
                }
            } catch (Exception ignored) {
            }
            String album = "";
            String cover = "";
            try {
                if (item.has("album") && item.get("album").isJsonObject()) {
                    JsonObject albumObj = item.getAsJsonObject("album");
                    album = getStr(albumObj, "name", "");
                    if (albumObj.has("images") && albumObj.get("images").isJsonArray()) {
                        JsonArray imgs = albumObj.getAsJsonArray("images");
                        if (imgs.size() > 0 && imgs.get(0).isJsonObject()) {
                            cover = getStr(imgs.get(0).getAsJsonObject(), "url", "");
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            return new Track(title, artists, album, cover, playing, progress, duration, System.currentTimeMillis());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String getStr(JsonObject o, String key, String def) {
        try {
            if (o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static long getLong(JsonObject o, String key, long def) {
        try {
            if (o.has(key) && !o.get(key).isJsonNull()) {
                return o.get(key).getAsLong();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    public static String beginAuth() {
        try {
            if (DontCamMod.config == null) {
                return "NO_CLIENT_ID";
            }
            String clientId;
            synchronized (DontCamMod.config) {
                clientId = DontCamMod.config.spotifyClientId;
            }
            if (clientId == null || clientId.trim().isEmpty()) {
                return "NO_CLIENT_ID";
            }
            clientId = clientId.trim();
            SecureRandom random = new SecureRandom();
            byte[] vBytes = new byte[48];
            random.nextBytes(vBytes);
            String verifier = base64Url(vBytes);
            String challenge;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
                challenge = base64Url(hash);
            } catch (Exception e) {
                return "NO_CLIENT_ID";
            }
            byte[] sBytes = new byte[8];
            random.nextBytes(sBytes);
            String state = toHex(sBytes);

            int port;
            try {
                ServerSocket probe = new ServerSocket(0);
                try {
                    port = probe.getLocalPort();
                } finally {
                    probe.close();
                }
            } catch (Exception e) {
                return "NO_BROWSER";
            }
            final int callbackPort = port;
            final String finalVerifier = verifier;
            final String finalState = state;
            final String finalClientId = clientId;

            HttpServer server;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", callbackPort), 0);
            } catch (Exception e) {
                return "NO_BROWSER";
            }
            server.setExecutor(Executors.newSingleThreadExecutor(new java.util.concurrent.ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "DontCam-Spotify-Callback");
                    t.setDaemon(true);
                    return t;
                }
            }));
            final HttpServer finalServer = server;
            final CountDownLatch latch = new CountDownLatch(1);
            finalServer.createContext("/callback", new com.sun.net.httpserver.HttpHandler() {
                @Override
                public void handle(com.sun.net.httpserver.HttpExchange exchange) {
                    try {
                        String query = null;
                        try {
                            query = exchange.getRequestURI().getRawQuery();
                        } catch (Exception ignored) {
                        }
                        String code = param(query, "code");
                        String gotState = param(query, "state");
                        String html;
                        if (code != null && !code.isEmpty() && finalState.equals(gotState)) {
                            html = "<html><body><h3>Spotify polaczono. Wroc do gry.</h3></body></html>";
                            final String fCode = code;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        tokenExchange(fCode, finalVerifier, callbackPort);
                                    } catch (Exception ignored) {
                                    } finally {
                                        latch.countDown();
                                        try {
                                            finalServer.stop(0);
                                        } catch (Exception ignored) {
                                        }
                                    }
                                }
                            }, "DontCam-Spotify-Exchange").start();
                        } else {
                            html = "<html><body><h3>Spotify: blad autoryzacji.</h3></body></html>";
                            latch.countDown();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(500L);
                                    } catch (InterruptedException ignored) {
                                    }
                                    try {
                                        finalServer.stop(0);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }).start();
                        }
                        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream os = exchange.getResponseBody();
                        try {
                            os.write(bytes);
                        } finally {
                            try {
                                os.close();
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (Exception ignored) {
                        try {
                            latch.countDown();
                        } catch (Exception ignored2) {
                        }
                    }
                }
            });
            finalServer.start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.await(10L, TimeUnit.MINUTES);
                    } catch (Exception ignored) {
                    }
                    try {
                        finalServer.stop(0);
                    } catch (Exception ignored) {
                    }
                }
            }, "DontCam-Spotify-Timeout").start();

            String redirectUri = "http://127.0.0.1:" + callbackPort + "/callback";
            String authUrl = "https://accounts.spotify.com/authorize"
                    + "?response_type=code"
                    + "&client_id=" + urlEncode(finalClientId)
                    + "&redirect_uri=" + urlEncode(redirectUri)
                    + "&scope=" + urlEncode("user-read-playback-state")
                    + "&state=" + urlEncode(finalState)
                    + "&code_challenge=" + urlEncode(challenge)
                    + "&code_challenge_method=S256";
            try {
                if (!Desktop.isDesktopSupported()) {
                    finalServer.stop(0);
                    return "NO_BROWSER";
                }
                Desktop.getDesktop().browse(new URI(authUrl));
            } catch (Exception e) {
                try {
                    finalServer.stop(0);
                } catch (Exception ignored) {
                }
                return "NO_BROWSER";
            }
            return "OK";
        } catch (Exception e) {
            return "NO_BROWSER";
        }
    }

    private static void tokenExchange(String code, String verifier, int port) {
        try {
            if (DontCamMod.config == null) {
                return;
            }
            String clientId;
            String clientSecret;
            synchronized (DontCamMod.config) {
                clientId = DontCamMod.config.spotifyClientId;
                clientSecret = DontCamMod.config.spotifyClientSecret;
            }
            if (clientId == null) {
                clientId = "";
            }
            if (clientSecret == null) {
                clientSecret = "";
            }
            String redirectUri = "http://127.0.0.1:" + port + "/callback";
            StringBuilder body = new StringBuilder();
            body.append("grant_type=").append(urlEncode("authorization_code"));
            body.append("&code=").append(urlEncode(code));
            body.append("&redirect_uri=").append(urlEncode(redirectUri));
            body.append("&client_id=").append(urlEncode(clientId.trim()));
            body.append("&code_verifier=").append(urlEncode(verifier));
            if (!clientSecret.trim().isEmpty()) {
                body.append("&client_secret=").append(urlEncode(clientSecret.trim()));
            }
            JsonObject json = tokenPost(body.toString());
            if (json == null) {
                return;
            }
            String access = optStr(json, "access_token", "");
            String refresh = optStr(json, "refresh_token", "");
            long expiresIn = 3600L;
            try {
                if (json.has("expires_in") && !json.get("expires_in").isJsonNull()) {
                    expiresIn = json.get("expires_in").getAsLong();
                }
            } catch (Exception ignored) {
            }
            if (access.isEmpty()) {
                return;
            }
            long expiry = System.currentTimeMillis() + expiresIn * 1000L;
            synchronized (DontCamMod.config) {
                DontCamMod.config.spotifyAccess = access;
                if (!refresh.isEmpty()) {
                    DontCamMod.config.spotifyRefresh = refresh;
                }
                DontCamMod.config.spotifyExpiry = expiry;
                try {
                    DontCamMod.config.save();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void doRefresh() {
        try {
            if (DontCamMod.config == null) {
                current = null;
                return;
            }
            String refresh;
            String clientId;
            String clientSecret;
            synchronized (DontCamMod.config) {
                refresh = DontCamMod.config.spotifyRefresh;
                clientId = DontCamMod.config.spotifyClientId;
                clientSecret = DontCamMod.config.spotifyClientSecret;
            }
            if (refresh == null || refresh.isEmpty()) {
                current = null;
                return;
            }
            if (clientId == null) {
                clientId = "";
            }
            if (clientSecret == null) {
                clientSecret = "";
            }
            StringBuilder body = new StringBuilder();
            body.append("grant_type=").append(urlEncode("refresh_token"));
            body.append("&refresh_token=").append(urlEncode(refresh));
            body.append("&client_id=").append(urlEncode(clientId.trim()));
            if (!clientSecret.trim().isEmpty()) {
                body.append("&client_secret=").append(urlEncode(clientSecret.trim()));
            }
            JsonObject json = tokenPost(body.toString());
            if (json == null || !json.has("access_token") || json.get("access_token").isJsonNull()) {
                synchronized (DontCamMod.config) {
                    DontCamMod.config.spotifyAccess = "";
                    DontCamMod.config.spotifyRefresh = "";
                    DontCamMod.config.spotifyExpiry = 0L;
                    try {
                        DontCamMod.config.save();
                    } catch (Exception ignored) {
                    }
                }
                current = null;
                return;
            }
            String access = optStr(json, "access_token", "");
            String newRefresh = optStr(json, "refresh_token", "");
            long expiresIn = 3600L;
            try {
                if (json.has("expires_in") && !json.get("expires_in").isJsonNull()) {
                    expiresIn = json.get("expires_in").getAsLong();
                }
            } catch (Exception ignored) {
            }
            if (access.isEmpty()) {
                synchronized (DontCamMod.config) {
                    DontCamMod.config.spotifyAccess = "";
                    DontCamMod.config.spotifyRefresh = "";
                    DontCamMod.config.spotifyExpiry = 0L;
                    try {
                        DontCamMod.config.save();
                    } catch (Exception ignored) {
                    }
                }
                current = null;
                return;
            }
            long expiry = System.currentTimeMillis() + expiresIn * 1000L;
            synchronized (DontCamMod.config) {
                DontCamMod.config.spotifyAccess = access;
                if (!newRefresh.isEmpty()) {
                    DontCamMod.config.spotifyRefresh = newRefresh;
                }
                DontCamMod.config.spotifyExpiry = expiry;
                try {
                    DontCamMod.config.save();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
            try {
                current = null;
            } catch (Exception ignored2) {
            }
        }
    }

    public static void disconnect() {
        try {
            if (DontCamMod.config != null) {
                synchronized (DontCamMod.config) {
                    DontCamMod.config.spotifyAccess = "";
                    DontCamMod.config.spotifyRefresh = "";
                    DontCamMod.config.spotifyExpiry = 0L;
                    try {
                        DontCamMod.config.save();
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                current = null;
            } catch (Exception ignored) {
            }
        }
    }

    private static JsonObject tokenPost(String formBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            byte[] data = formBody.getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/json");
            conn.setFixedLengthStreamingMode(data.length);
            OutputStream os = conn.getOutputStream();
            try {
                os.write(data);
                os.flush();
            } finally {
                try {
                    os.close();
                } catch (Exception ignored) {
                }
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            if (in == null) {
                return null;
            }
            String body = readAll(in);
            if (body == null || body.isEmpty()) {
                return null;
            }
            if (code < 200 || code >= 300) {
                return null;
            }
            JsonElement el = new JsonParser().parse(body);
            if (el == null || !el.isJsonObject()) {
                return null;
            }
            return el.getAsJsonObject();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String readAll(InputStream in) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = br.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }
            return sb.toString();
        } catch (Exception ignored) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignored) {
                }
            } else if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String optStr(JsonObject o, String key, String def) {
        try {
            if (o.has(key) && !o.get(key).isJsonNull()) {
                String s = o.get(key).getAsString();
                return s != null ? s : def;
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s != null ? s : "", "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String base64Url(byte[] data) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        } catch (Exception e) {
            return "";
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            int v = b & 0xFF;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString();
    }

    private static String param(String query, String key) {
        try {
            if (query == null || query.isEmpty() || key == null) {
                return null;
            }
            String[] pairs = query.split("&");
            for (String p : pairs) {
                int i = p.indexOf('=');
                String k;
                String v;
                if (i >= 0) {
                    k = p.substring(0, i);
                    v = p.substring(i + 1);
                } else {
                    k = p;
                    v = "";
                }
                try {
                    k = java.net.URLDecoder.decode(k, "UTF-8");
                } catch (Exception ignored) {
                }
                if (key.equals(k)) {
                    try {
                        return java.net.URLDecoder.decode(v, "UTF-8");
                    } catch (Exception ignored) {
                        return v;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private SpotifyManager() {
    }
}
