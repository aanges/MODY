package com.dontcam.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/** Simple JSON toggles stored in config/dontcam.json */
public class DontCamConfig {

    public boolean fps = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean badges = true;
    public boolean spotifyEnabled = true;
    public boolean spotifyShowProgress = true;
    public int spotifyX = 5;
    public int spotifyY = 40;
    public float spotifyScale = 1.0f;
    public String spotifyClientId = "";
    public String spotifyClientSecret = "";
    public String spotifyAccess = "";
    public String spotifyRefresh = "";
    public long spotifyExpiry = 0L;

    private final File file;

    public DontCamConfig(File configDir) {
        if (configDir == null) {
            this.file = new File("dontcam.json");
        } else {
            if (!configDir.exists()) {
                configDir.mkdirs();
            }
            this.file = new File(configDir, "dontcam.json");
        }
        load();
    }

    public void load() {
        try {
            if (file == null || !file.exists()) {
                save();
                return;
            }
            try (FileReader reader = new FileReader(file)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json == null) {
                    return;
                }
                if (json.has("fps")) {
                    fps = json.get("fps").getAsBoolean();
                }
                if (json.has("cps")) {
                    cps = json.get("cps").getAsBoolean();
                }
                if (json.has("keystrokes")) {
                    keystrokes = json.get("keystrokes").getAsBoolean();
                }
                if (json.has("badges")) {
                    badges = json.get("badges").getAsBoolean();
                }
                if (json.has("spotifyEnabled")) {
                    try { spotifyEnabled = json.get("spotifyEnabled").getAsBoolean(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyShowProgress")) {
                    try { spotifyShowProgress = json.get("spotifyShowProgress").getAsBoolean(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyX")) {
                    try { spotifyX = json.get("spotifyX").getAsInt(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyY")) {
                    try { spotifyY = json.get("spotifyY").getAsInt(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyScale")) {
                    try { spotifyScale = json.get("spotifyScale").getAsFloat(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyClientId")) {
                    try { spotifyClientId = json.get("spotifyClientId").getAsString(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyClientSecret")) {
                    try { spotifyClientSecret = json.get("spotifyClientSecret").getAsString(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyAccess")) {
                    try { spotifyAccess = json.get("spotifyAccess").getAsString(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyRefresh")) {
                    try { spotifyRefresh = json.get("spotifyRefresh").getAsString(); } catch (Exception ignored) {}
                }
                if (json.has("spotifyExpiry")) {
                    try { spotifyExpiry = json.get("spotifyExpiry").getAsLong(); } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            if (file == null) {
                return;
            }
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                JsonObject json = new JsonObject();
                json.addProperty("fps", fps);
                json.addProperty("cps", cps);
                json.addProperty("keystrokes", keystrokes);
                json.addProperty("badges", badges);
                json.addProperty("spotifyEnabled", spotifyEnabled);
                json.addProperty("spotifyShowProgress", spotifyShowProgress);
                json.addProperty("spotifyX", spotifyX);
                json.addProperty("spotifyY", spotifyY);
                json.addProperty("spotifyScale", spotifyScale);
                json.addProperty("spotifyClientId", spotifyClientId == null ? "" : spotifyClientId);
                json.addProperty("spotifyClientSecret", spotifyClientSecret == null ? "" : spotifyClientSecret);
                json.addProperty("spotifyAccess", spotifyAccess == null ? "" : spotifyAccess);
                json.addProperty("spotifyRefresh", spotifyRefresh == null ? "" : spotifyRefresh);
                json.addProperty("spotifyExpiry", spotifyExpiry);
                writer.write(json.toString());
            }
        } catch (Exception ignored) {
        }
    }
}
