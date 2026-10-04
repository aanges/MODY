package com.dontcam.fabric.hud;

import com.dontcam.fabric.DontCamFabricMod;
import com.dontcam.fabric.spotify.SpotifyManager;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

/** FPS / CPS / keystrokes overlay. Toggled from the Right-Shift menu. */
public class HudOverlay {

    private static final int BG = 0x80000000;
    private static final int BG_PRESSED = 0xCCFFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int TEXT_DARK = 0xFF000000;

    public static void init() {
        HudRenderCallback.EVENT.register(HudOverlay::render);
    }

    private static void render(MatrixStack matrices, float tickDelta) {
        try {
            if (matrices == null) {
                return;
            }
            MinecraftClient client;
            try {
                client = MinecraftClient.getInstance();
            } catch (Throwable t) {
                return;
            }
            if (client == null || client.player == null) {
                return;
            }
            if (client.options == null) {
                return;
            }
            try {
                if (client.options.hudHidden || client.options.debugEnabled) {
                    return;
                }
            } catch (Throwable t) {
                return;
            }
            if (client.textRenderer == null) {
                return;
            }
            if (DontCamFabricMod.config == null) {
                return;
            }
            try {
                if (DontCamFabricMod.config.fps) {
                    drawFps(client, matrices);
                }
            } catch (Throwable ignored) {
            }
            try {
                if (DontCamFabricMod.config.cps) {
                    drawCps(client, matrices);
                }
            } catch (Throwable ignored) {
            }
            try {
                if (DontCamFabricMod.config.keystrokes) {
                    drawKeystrokes(client, matrices);
                }
            } catch (Throwable ignored) {
            }
            try {
                drawSpotify(client, matrices);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private static void drawFps(MinecraftClient client, MatrixStack matrices) {
        try {
            if (client == null || matrices == null || client.textRenderer == null) {
                return;
            }
            int fps;
            try {
                fps = client.getCurrentFps();
            } catch (Throwable t) {
                return;
            }
            String text = fps + " FPS";
            int w = client.textRenderer.getWidth(text);
            DrawableHelper.fill(matrices, 5, 5, 9 + w, 18, BG);
            client.textRenderer.draw(matrices, text, 7, 7, TEXT);
        } catch (Throwable ignored) {
        }
    }

    private static void drawCps(MinecraftClient client, MatrixStack matrices) {
        try {
            if (client == null || matrices == null || client.textRenderer == null) {
                return;
            }
            ClickTracker tracker = ClickTracker.get();
            int left = tracker != null ? tracker.leftCps() : 0;
            int right = tracker != null ? tracker.rightCps() : 0;
            String text = left + " | " + right + " CPS";
            int w = client.textRenderer.getWidth(text);
            DrawableHelper.fill(matrices, 5, 21, 9 + w, 34, BG);
            client.textRenderer.draw(matrices, text, 7, 23, TEXT);
        } catch (Throwable ignored) {
        }
    }

    private static void key(MinecraftClient client, MatrixStack matrices, int x, int y, int w, int h, String label, boolean pressed) {
        try {
            if (client == null || matrices == null || client.textRenderer == null) {
                return;
            }
            if (label == null) {
                return;
            }
            DrawableHelper.fill(matrices, x, y, x + w, y + h, pressed ? BG_PRESSED : BG);
            int color = pressed ? TEXT_DARK : TEXT_DIM;
            int tw = client.textRenderer.getWidth(label);
            client.textRenderer.draw(matrices, label, x + (w - tw) / 2.0f, y + (h - 8) / 2.0f, color);
        } catch (Throwable ignored) {
        }
    }

    private static void drawKeystrokes(MinecraftClient client, MatrixStack matrices) {
        try {
            if (client == null || matrices == null || client.options == null) {
                return;
            }
            if (client.getWindow() == null) {
                return;
            }
            if (client.textRenderer == null) {
                return;
            }
            int key = 22;
            int gap = 2;
            int spaceH = 12;
            int totalW = key * 3 + gap * 2;
            int x0 = client.getWindow().getScaledWidth() / 2 - totalW / 2;
            int y0 = client.getWindow().getScaledHeight() - 68;

            boolean w = client.options.forwardKey != null && client.options.forwardKey.isPressed();
            boolean a = client.options.leftKey != null && client.options.leftKey.isPressed();
            boolean s = client.options.backKey != null && client.options.backKey.isPressed();
            boolean d = client.options.rightKey != null && client.options.rightKey.isPressed();
            boolean space = client.options.jumpKey != null && client.options.jumpKey.isPressed();
            boolean lmb = client.options.attackKey != null && client.options.attackKey.isPressed();
            boolean rmb = client.options.useKey != null && client.options.useKey.isPressed();

            key(client, matrices, x0 + key + gap, y0, key, key, "W", w);
            key(client, matrices, x0, y0 + key + gap, key, key, "A", a);
            key(client, matrices, x0 + key + gap, y0 + key + gap, key, key, "S", s);
            key(client, matrices, x0 + key * 2 + gap * 2, y0 + key + gap, key, key, "D", d);

            int sy = y0 + key * 2 + gap * 2;
            DrawableHelper.fill(matrices, x0, sy, x0 + totalW, sy + spaceH, space ? BG_PRESSED : BG);

            int my = sy + spaceH + gap;
            int half = (totalW - gap) / 2;
            key(client, matrices, x0, my, half, 12, "LMB", lmb);
            key(client, matrices, x0 + half + gap, my, totalW - half - gap, 12, "RMB", rmb);
        } catch (Throwable ignored) {
        }
    }

    private static void drawSpotify(MinecraftClient client, MatrixStack matrices) {
        try {
            if (client == null || matrices == null || client.textRenderer == null) {
                return;
            }
            if (DontCamFabricMod.config == null || !DontCamFabricMod.config.spotifyEnabled) {
                return;
            }
            SpotifyManager.Track t;
            try {
                t = SpotifyManager.snapshot();
            } catch (Throwable th) {
                return;
            }
            int ox;
            int oy;
            float sc;
            boolean showProgress;
            try {
                ox = DontCamFabricMod.config.spotifyX;
                oy = DontCamFabricMod.config.spotifyY;
                sc = DontCamFabricMod.config.spotifyScale;
                showProgress = DontCamFabricMod.config.spotifyShowProgress;
            } catch (Throwable th) {
                return;
            }
            if (sc <= 0.0f || Float.isNaN(sc) || Float.isInfinite(sc)) {
                sc = 1.0f;
            }
            matrices.push();
            matrices.translate(ox, oy, 0.0);
            matrices.scale(sc, sc, 1.0f);
            try {
                if (t == null) {
                    String text = "Spotify: brak utworu";
                    int w = client.textRenderer.getWidth(text);
                    DrawableHelper.fill(matrices, 0, 0, 8 + w, 14, BG);
                    client.textRenderer.draw(matrices, text, 4, 3, TEXT);
                    return;
                }
                String title = t.title == null ? "" : t.title;
                String artists = t.artists == null ? "" : t.artists;
                String base = artists.isEmpty() ? title : title + " - " + artists;
                if (base.length() > 32) {
                    base = base.substring(0, 31) + "\u2026";
                }
                String icon = t.playing ? "\u25B6" : "\u275A\u275A";
                String line2 = icon + " " + fmtTime(t.progressMs) + "/" + fmtTime(t.durationMs);
                int w1 = client.textRenderer.getWidth(base);
                int w2 = client.textRenderer.getWidth(line2);
                int bw = Math.max(w1, w2) + 8;
                boolean bar = showProgress && t.durationMs > 0;
                int bh = bar ? 32 : 26;
                DrawableHelper.fill(matrices, 0, 0, bw, bh, BG);
                client.textRenderer.draw(matrices, base, 4, 3, TEXT);
                client.textRenderer.draw(matrices, line2, 4, 13, TEXT_DIM);
                if (bar) {
                    long p = Math.max(0L, Math.min(t.progressMs, t.durationMs));
                    double frac = (double) p / (double) t.durationMs;
                    int bwTotal = Math.max(0, bw - 8);
                    int filled = (int) (bwTotal * frac);
                    DrawableHelper.fill(matrices, 4, bh - 6, bw - 4, bh - 4, 0xFF333333);
                    if (filled > 0) {
                        DrawableHelper.fill(matrices, 4, bh - 6, 4 + filled, bh - 4, 0xFF1DB954);
                    }
                }
            } finally {
                try {
                    matrices.pop();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    static String fmtTime(long ms) {
        try {
            if (ms < 0L) {
                ms = 0L;
            }
            long s = ms / 1000L;
            return (s / 60L) + ":" + String.format("%02d", s % 60L);
        } catch (Throwable t) {
            return "0:00";
        }
    }
}
