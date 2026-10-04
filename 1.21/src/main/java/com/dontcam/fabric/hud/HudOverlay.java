package com.dontcam.fabric.hud;

import com.dontcam.fabric.ClientConfig;
import com.dontcam.fabric.DontCamFabricMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FPS / CPS / keystrokes overlay, fully driven by {@link ClientConfig}.
 *
 * <p>{@link #drawModules} is shared between the in-game HUD and the options
 * screen live preview. Every drawn module registers its screen rect in
 * {@link #lastRects} so the options screen can drag modules around.
 */
public class HudOverlay {

    private static final int TEXT_DARK = 0xFF000000;
    private static final int BG_PRESSED = 0xCCFFFFFF;

    /** Module id -> last drawn rect {x, y, w, h}, refreshed on every draw. */
    public static final Map<String, int[]> lastRects = new LinkedHashMap<>();

    public static void init() {
        HudRenderCallback.EVENT.register(HudOverlay::render);
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        try {
            if (context == null) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.options == null
                    || client.textRenderer == null || client.getWindow() == null) {
                return;
            }
            if (client.options.hudHidden
                    || (client.getDebugHud() != null && client.getDebugHud().shouldShowDebugHud())) {
                return;
            }
            drawModules(context, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight(),
                    client.textRenderer, client, false);
        } catch (Exception ignored) {
        }
    }

    /** Live preview for the options screen (uses live game data, never hides). */
    public static void drawPreview(DrawContext context, int width, int height, TextRenderer tr) {
        try {
            if (context == null || tr == null) {
                return;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            drawModules(context, width, height, tr, client, true);
        } catch (Exception ignored) {
        }
    }

    private static void drawModules(DrawContext context, int width, int height,
                                    TextRenderer tr, MinecraftClient client, boolean preview) {
        try {
            if (context == null || tr == null) {
                return;
            }
            ClientConfig cfg = DontCamFabricMod.config;
            if (cfg == null) {
                return;
            }
            if (cfg.fps != null && cfg.fps.enabled) {
                int fps;
                try {
                    fps = client != null ? client.getCurrentFps() : 0;
                } catch (Exception ignored) {
                    fps = 0;
                }
                String text = cfg.fps.label ? fps + " FPS" : String.valueOf(fps);
                drawTextModule(context, tr, cfg.fps, text, "fps");
            } else {
                lastRects.remove("fps");
            }
            if (cfg.cps != null && cfg.cps.enabled) {
                ClickTracker tracker = ClickTracker.get();
                int left = tracker != null ? tracker.leftCps() : 0;
                int right = tracker != null ? tracker.rightCps() : 0;
                String text = left + " | " + right + (cfg.cps.label ? " CPS" : "");
                drawTextModule(context, tr, cfg.cps, text, "cps");
            } else {
                lastRects.remove("cps");
            }
            if (cfg.keys != null && cfg.keys.enabled) {
                drawKeys(context, width, height, tr, client, cfg.keys);
            } else {
                lastRects.remove("keys");
            }
        } catch (Exception ignored) {
        }
    }

    private static void drawTextModule(DrawContext context, TextRenderer tr,
                                       ClientConfig.Module m, String text, String id) {
        try {
            if (context == null || tr == null || m == null || text == null || id == null) {
                return;
            }
            float s = m.scale;
            int w = (int) (tr.getWidth(text) * s);
            int h = (int) (9 * s);
            if (m.bg) {
                context.fill(m.x - 2, m.y - 2, m.x + w + 2, m.y + h + 2, bgColor());
            }
            if (context.getMatrices() == null || context.getMatrices().peek() == null) {
                return;
            }
            context.getMatrices().push();
            try {
                context.getMatrices().translate(m.x, m.y, 0);
                context.getMatrices().scale(s, s, 1.0f);
                context.drawText(tr, text, 0, 0, resolveColor(m), false);
            } finally {
                context.getMatrices().pop();
            }
            lastRects.put(id, new int[]{m.bg ? m.x - 2 : m.x, m.bg ? m.y - 2 : m.y,
                    w + (m.bg ? 4 : 0), h + (m.bg ? 4 : 0)});
        } catch (Exception ignored) {
        }
    }

    /** {x0, y0, totalW, totalH, key, gap, spaceH} for the keystrokes block. */
    public static int[] keysAnchor(int width, int height, ClientConfig.Module m) {
        if (m == null) {
            return new int[]{width / 2, height - 68, 0, 0, 22, 2, 12};
        }
        float s = m.scale;
        int key = (int) (22 * s);
        int gap = Math.max(1, (int) (2 * s));
        int spaceH = (int) (12 * s);
        int totalW = key * 3 + gap * 2;
        int totalH = key * 2 + gap * 2 + spaceH + gap + (int) (12 * s);
        // Bottom-center anchor + user offsets.
        int x0 = width / 2 - totalW / 2 + m.x;
        int y0 = height - 68 + m.y;
        return new int[]{x0, y0, totalW, totalH, key, gap, spaceH};
    }

    private static void drawKeys(DrawContext context, int width, int height,
                                 TextRenderer tr, MinecraftClient client, ClientConfig.Module m) {
        try {
            if (context == null || tr == null || m == null) {
                return;
            }
            int[] an = keysAnchor(width, height, m);
            if (an == null || an.length < 7) {
                return;
            }
            int x0 = an[0];
            int y0 = an[1];
            int totalW = an[2];
            int totalH = an[3];
            int key = an[4];
            int gap = an[5];
            int spaceH = an[6];
            float s = m.scale;

            boolean fw = client != null && client.options != null && client.options.forwardKey != null
                    && client.options.forwardKey.isPressed();
            boolean lf = client != null && client.options != null && client.options.leftKey != null
                    && client.options.leftKey.isPressed();
            boolean bk = client != null && client.options != null && client.options.backKey != null
                    && client.options.backKey.isPressed();
            boolean rt = client != null && client.options != null && client.options.rightKey != null
                    && client.options.rightKey.isPressed();
            boolean space = client != null && client.options != null && client.options.jumpKey != null
                    && client.options.jumpKey.isPressed();
            boolean lmb = client != null && client.options != null && client.options.attackKey != null
                    && client.options.attackKey.isPressed();
            boolean rmb = client != null && client.options != null && client.options.useKey != null
                    && client.options.useKey.isPressed();

            keyBox(context, tr, x0 + key + gap, y0, key, key, "W", fw, m);
            keyBox(context, tr, x0, y0 + key + gap, key, key, "A", lf, m);
            keyBox(context, tr, x0 + key + gap, y0 + key + gap, key, key, "S", bk, m);
            keyBox(context, tr, x0 + key * 2 + gap * 2, y0 + key + gap, key, key, "D", rt, m);

            int sy = y0 + key * 2 + gap * 2;
            context.fill(x0, sy, x0 + totalW, sy + spaceH, space ? BG_PRESSED : (m.bg ? bgColor() : 0x00000000));

            int my = sy + spaceH + gap;
            int half = (totalW - gap) / 2;
            int mh = (int) (12 * s);
            keyBox(context, tr, x0, my, half, mh, "LMB", lmb, m);
            keyBox(context, tr, x0 + half + gap, my, totalW - half - gap, mh, "RMB", rmb, m);

            lastRects.put("keys", new int[]{x0, y0, totalW, totalH});
        } catch (Exception ignored) {
        }
    }

    private static void keyBox(DrawContext context, TextRenderer tr,
                               int x, int y, int w, int h, String label,
                               boolean pressed, ClientConfig.Module m) {
        try {
            if (context == null || tr == null || m == null || label == null) {
                return;
            }
            context.fill(x, y, x + w, y + h, pressed ? BG_PRESSED : (m.bg ? bgColor() : 0x00000000));
            int color = pressed ? TEXT_DARK : resolveColor(m);
            int tw = tr.getWidth(label);
            float s = m.scale;
            if (context.getMatrices() == null || context.getMatrices().peek() == null) {
                return;
            }
            context.getMatrices().push();
            try {
                context.getMatrices().translate(x + (w - tw * s) / 2.0f, y + (h - 8 * s) / 2.0f, 0);
                context.getMatrices().scale(s, s, 1.0f);
                context.drawText(tr, label, 0, 0, color, false);
            } finally {
                context.getMatrices().pop();
            }
        } catch (Exception ignored) {
        }
    }

    /** ARGB text color for a module: static color or animated chroma/rainbow. */
    public static int resolveColor(ClientConfig.Module m) {
        if (m == null) {
            return 0xFFFFFFFF;
        }
        String mode = m.colorMode == null ? "static" : m.colorMode;
        if ("chroma".equals(mode)) {
            return 0xFF000000 | java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 2000L) / 2000.0f, 1.0f, 1.0f);
        }
        if ("rainbow".equals(mode)) {
            return 0xFF000000 | java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 8000L) / 8000.0f, 0.7f, 1.0f);
        }
        return 0xFF000000 | (m.color & 0xFFFFFF);
    }

    private static int bgColor() {
        return 0x80000000;
    }
}
