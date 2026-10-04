package com.dontcam.fabric.spotify;

import com.dontcam.fabric.DontCamFabricMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Spotify "currently playing" poller + PKCE auth. No Minecraft classes here. */
public final class SpotifyManager {

    public enum AuthResult {
        OK,
        NO_CLIENT_ID,
        NO_BROWSER
    }

    public static final class Track {
        public final String title;
        public final String artists;
        public final boolean playing;
        public final long progressMs;
        public final long durationMs;

        public Track(String title, String artists, boolean playing, long progressMs, long durationMs) {
            this.title = title == null ? "" : title;
            this.artists = artists == null ? "" : artists;
            this.playing = playing;
            this.progressMs = Math.max(0L, progressMs);
            this.durationMs = Math.max(0L, durationMs);
        }
    }

    private static volatile Track current = null;
    private static volatile ScheduledExecutorService exec = null;
    private static final Object START_LOCK = new Object();

    private SpotifyManager() {
    }

    public static Track snapshot() {
        return current;
    }

    public static boolean isLinked() {
        try {
            if (DontCamFabricMod.config == null) {
                return false;
            }
            String refresh = DontCamFabricMod.config.spotifyRefresh;
            return refresh != null && !refresh.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static void ensureStarted() {
        try {
            synchronized (START_LOCK) {
                if (exec != null && !exec.isShutdown()) {
                    return;
                }
                ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "dontcam-spotify");
                    t.setDaemon(true);
                    return t;
                });
                exec = s;
                s.scheduleAtFixedRate(() -> {
                    try {
                        poll();
                    } catch (Exception ignored) {
                    }
                }, 0L, 5L, TimeUnit.SECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    private static void poll() {
        try {
            if (DontCamFabricMod.config == null || !DontCamFabricMod.config.spotifyEnabled) {
                return;
            }
            if (!isLinked()) {
                current = null;
                return;
            }
            String access = DontCamFabricMod.config.spotifyAccess;
            long expiry = DontCamFabricMod.config.spotifyExpiry;
            if ((access == null || access.isEmpty() || expiry - System.currentTimeMillis() < 10_000L) && !doRefresh()) {
                current = null;
                return;
            }
            access = DontCamFabricMod.config.spotifyAccess;
            if (access == null || access.isEmpty()) {
                current = null;
                return;
            }
            Track t = fetch(access, false);
            current = t;
        } catch (Exception ignored) {
            current = null;
        }
    }

    private static Track fetch(String access, boolean retried) {
        HttpURLConnection con = null;
        try {
            URL url = new URL("https://api.spotify.com/v1/me/player/currently-playing");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Authorization", "Bearer " + access);
            int code = con.getResponseCode();
            if (code == 204 || code == 404) {
                return null;
            }
            if (code == 401 && !retried) {
                if (doRefresh()) {
                    String fresh = DontCamFabricMod.config != null ? DontCamFabricMod.config.spotifyAccess : null;
                    if (fresh != null && !fresh.isEmpty()) {
                        return fetch(fresh, true);
                    }
                }
                return null;
            }
            if (code != 200) {
                return null;
            }
            String body = readAll(con.getInputStream());
            if (body == null || body.isEmpty()) {
                return null;
            }
            return parse(body);
        } catch (Exception ignored) {
            return null;
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static Track parse(String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (root == null || !root.has("item") || root.get("item") == null || root.get("item").isJsonNull()) {
                return null;
            }
            JsonObject item = root.getAsJsonObject("item");
            String title = item.has("name") && !item.get("name").isJsonNull() ? item.get("name").getAsString() : "";
            long duration = 0L;
            try {
                if (item.has("duration_ms")) {
                    duration = item.get("duration_ms").getAsLong();
                }
            } catch (Exception ignored) {
            }
            StringBuilder ab = new StringBuilder();
            try {
                if (item.has("artists") && item.get("artists").isJsonArray()) {
                    JsonArray arr = item.getAsJsonArray("artists");
                    for (int i = 0; i < arr.size(); i++) {
                        try {
                            JsonObject a = arr.get(i).getAsJsonObject();
                            if (a.has("name") && !a.get("name").isJsonNull()) {
                                if (ab.length() > 0) {
                                    ab.append(", ");
                                }
                                ab.append(a.get("name").getAsString());
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            boolean playing = false;
            try {
                if (root.has("is_playing")) {
                    playing = root.get("is_playing").getAsBoolean();
                }
            } catch (Exception ignored) {
            }
            long progress = 0L;
            try {
                if (root.has("progress_ms") && !root.get("progress_ms").isJsonNull()) {
                    progress = root.get("progress_ms").getAsLong();
                }
            } catch (Exception ignored) {
            }
            if (title.isEmpty() && ab.length() == 0) {
                return null;
            }
            return new Track(title, ab.toString(), playing, progress, duration);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static AuthResult beginAuth() {
        String clientId;
        String secret;
        try {
            if (DontCamFabricMod.config == null) {
                return AuthResult.NO_CLIENT_ID;
            }
            clientId = DontCamFabricMod.config.spotifyClientId;
            secret = DontCamFabricMod.config.spotifyClientSecret;
        } catch (Exception e) {
            return AuthResult.NO_CLIENT_ID;
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            return AuthResult.NO_CLIENT_ID;
        }
        final String cid = clientId.trim();
        final String csec = secret == null ? "" : secret.trim();
        boolean canBrowse;
        try {
            canBrowse = Desktop.isDesktopSupported() && Desktop.getDesktop() != null
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
        } catch (Exception e) {
            canBrowse = false;
        }
        if (!canBrowse) {
            return AuthResult.NO_BROWSER;
        }
        HttpServer server = null;
        int port;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();
        } catch (Exception e) {
            return AuthResult.NO_BROWSER;
        }
        final HttpServer srv = server;
        String verifier;
        String challenge;
        String state;
        try {
            verifier = genVerifier();
            challenge = genChallenge(verifier);
            state = genState();
        } catch (Exception e) {
            try {
                srv.stop(0);
            } catch (Exception ignored) {
            }
            return AuthResult.NO_BROWSER;
        }
        final String fv = verifier;
        final String redirect = "http://127.0.0.1:" + port + "/callback";
        final CountDownLatch latch = new CountDownLatch(1);
        final String[] codeBox = new String[1];
        final String[] stateBox = new String[1];
        try {
            srv.createContext("/callback", ex -> {
                try {
                    String q = ex.getRequestURI() != null ? ex.getRequestURI().getRawQuery() : null;
                    String code = queryParam(q, "code");
                    String st = queryParam(q, "state");
                    codeBox[0] = code;
                    stateBox[0] = st;
                    String html = "<html><body><h3>DontCam: Spotify connected. Return to Minecraft.</h3></body></html>";
                    byte[] out = html.getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    ex.sendResponseHeaders(200, out.length);
                    try (OutputStream os = ex.getResponseBody()) {
                        os.write(out);
                    }
                } catch (Exception ignored) {
                } finally {
                    latch.countDown();
                }
            });
            srv.setExecutor(Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "dontcam-spotify-callback");
                t.setDaemon(true);
                return t;
            }));
            srv.start();
        } catch (Exception e) {
            try {
                srv.stop(0);
            } catch (Exception ignored) {
            }
            return AuthResult.NO_BROWSER;
        }
        Thread worker = new Thread(() -> {
            try {
                boolean ok = latch.await(180L, TimeUnit.SECONDS);
                if (ok && codeBox[0] != null && !codeBox[0].isEmpty()
                        && stateBox[0] != null && stateBox[0].equals(state)) {
                    exchangeCode(codeBox[0], cid, csec, redirect, fv);
                }
            } catch (Exception ignored) {
            } finally {
                try {
                    srv.stop(0);
                } catch (Exception ignored) {
                }
            }
        }, "dontcam-spotify-auth");
        worker.setDaemon(true);
        worker.start();
        try {
            String scope = URLEncoder.encode("user-read-playback-state user-read-currently-playing", StandardCharsets.UTF_8.name());
            String auth = "https://accounts.spotify.com/authorize?client_id=" + URLEncoder.encode(cid, StandardCharsets.UTF_8.name())
                    + "&response_type=code&redirect_uri=" + URLEncoder.encode(redirect, StandardCharsets.UTF_8.name())
                    + "&code_challenge_method=S256&code_challenge=" + URLEncoder.encode(challenge, StandardCharsets.UTF_8.name())
                    + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8.name())
                    + "&scope=" + scope;
            Desktop.getDesktop().browse(new URI(auth));
        } catch (Exception e) {
            return AuthResult.NO_BROWSER;
        }
        return AuthResult.OK;
    }

    private static void exchangeCode(String code, String clientId, String secret, String redirect, String verifier) {
        HttpURLConnection con = null;
        try {
            StringBuilder b = new StringBuilder();
            b.append("grant_type=authorization_code");
            b.append("&code=").append(URLEncoder.encode(code, StandardCharsets.UTF_8.name()));
            b.append("&redirect_uri=").append(URLEncoder.encode(redirect, StandardCharsets.UTF_8.name()));
            b.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8.name()));
            b.append("&code_verifier=").append(URLEncoder.encode(verifier, StandardCharsets.UTF_8.name()));
            if (secret != null && !secret.isEmpty()) {
                b.append("&client_secret=").append(URLEncoder.encode(secret, StandardCharsets.UTF_8.name()));
            }
            byte[] body = b.toString().getBytes(StandardCharsets.UTF_8);
            URL url = new URL("https://accounts.spotify.com/api/token");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream os = con.getOutputStream()) {
                os.write(body);
            }
            int rc = con.getResponseCode();
            InputStream in = rc == 200 ? con.getInputStream() : con.getErrorStream();
            String resp = in != null ? readAll(in) : "";
            if (rc != 200) {
                return;
            }
            storeTokenResponse(resp);
        } catch (Exception ignored) {
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static synchronized boolean doRefresh() {
        HttpURLConnection con = null;
        try {
            if (DontCamFabricMod.config == null) {
                return false;
            }
            String refresh = DontCamFabricMod.config.spotifyRefresh;
            String clientId = DontCamFabricMod.config.spotifyClientId;
            String secret = DontCamFabricMod.config.spotifyClientSecret;
            if (refresh == null || refresh.isEmpty() || clientId == null || clientId.trim().isEmpty()) {
                return false;
            }
            StringBuilder b = new StringBuilder();
            b.append("grant_type=refresh_token");
            b.append("&refresh_token=").append(URLEncoder.encode(refresh.trim(), StandardCharsets.UTF_8.name()));
            b.append("&client_id=").append(URLEncoder.encode(clientId.trim(), StandardCharsets.UTF_8.name()));
            if (secret != null && !secret.trim().isEmpty()) {
                b.append("&client_secret=").append(URLEncoder.encode(secret.trim(), StandardCharsets.UTF_8.name()));
            }
            byte[] body = b.toString().getBytes(StandardCharsets.UTF_8);
            URL url = new URL("https://accounts.spotify.com/api/token");
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("Content-Length", String.valueOf(body.length));
            try (OutputStream os = con.getOutputStream()) {
                os.write(body);
            }
            int rc = con.getResponseCode();
            InputStream in = rc == 200 ? con.getInputStream() : con.getErrorStream();
            String resp = in != null ? readAll(in) : "";
            if (rc != 200) {
                clearTokens();
                return false;
            }
            return storeTokenResponse(resp);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (con != null) {
                    con.disconnect();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static boolean storeTokenResponse(String resp) {
        try {
            if (resp == null || resp.isEmpty() || DontCamFabricMod.config == null) {
                return false;
            }
            JsonObject o = JsonParser.parseString(resp).getAsJsonObject();
            if (o == null || !o.has("access_token") || o.get("access_token").isJsonNull()) {
                return false;
            }
            String access = o.get("access_token").getAsString();
            if (o.has("refresh_token") && !o.get("refresh_token").isJsonNull()) {
                String r = o.get("refresh_token").getAsString();
                if (r != null && !r.isEmpty()) {
                    DontCamFabricMod.config.spotifyRefresh = r;
                }
            }
            DontCamFabricMod.config.spotifyAccess = access;
            long expiresIn = 3600L;
            try {
                if (o.has("expires_in")) {
                    expiresIn = o.get("expires_in").getAsLong();
                }
            } catch (Exception ignored) {
            }
            DontCamFabricMod.config.spotifyExpiry = System.currentTimeMillis() + Math.max(30L, expiresIn) * 1000L - 10_000L;
            saveConfig();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void clearTokens() {
        try {
            if (DontCamFabricMod.config == null) {
                current = null;
                return;
            }
            DontCamFabricMod.config.spotifyAccess = "";
            DontCamFabricMod.config.spotifyRefresh = "";
            DontCamFabricMod.config.spotifyExpiry = 0L;
            saveConfig();
        } catch (Exception ignored) {
        } finally {
            current = null;
        }
    }

    public static void disconnect() {
        clearTokens();
    }

    private static void saveConfig() {
        try {
            if (DontCamFabricMod.config != null) {
                DontCamFabricMod.config.save();
            }
        } catch (Exception ignored) {
        }
    }

    private static String queryParam(String q, String key) {
        try {
            if (q == null || key == null) {
                return null;
            }
            for (String p : q.split("&")) {
                int i = p.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String k = p.substring(0, i);
                if (k.equals(key)) {
                    return java.net.URLDecoder.decode(p.substring(i + 1), StandardCharsets.UTF_8.name());
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String genVerifier() {
        byte[] b = new byte[64];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String genChallenge(String verifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
    }

    private static String genState() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    private static String readAll(InputStream in) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) >= 0) {
                bos.write(buf, 0, n);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }
}
