package com.dontcam;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class DontCamConfig {

    public boolean fps = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean badges = true;

    private final File file;

    public DontCamConfig(File configDir) {
        File dir = configDir;
        if (dir == null) {
            dir = new File("config");
        }
        try {
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception ignored) {
        }
        this.file = new File(dir, "dontcam.json");
        load();
    }

    public void load() {
        try {
            if (file == null || !file.exists()) {
                save();
                return;
            }
            FileReader reader = new FileReader(file);
            try {
                JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
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
            } finally {
                try {
                    reader.close();
                } catch (Exception ignored) {
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
            JsonObject json = new JsonObject();
            json.addProperty("fps", fps);
            json.addProperty("cps", cps);
            json.addProperty("keystrokes", keystrokes);
            json.addProperty("badges", badges);
            FileWriter writer = new FileWriter(file);
            try {
                writer.write(json.toString());
            } finally {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }
}
