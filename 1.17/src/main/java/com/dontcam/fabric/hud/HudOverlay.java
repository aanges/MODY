package com.dontcam.fabric.hud;

import com.dontcam.fabric.DontCamFabricMod;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;

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
            if (client.textRenderer == null || client.getWindow() == null) {
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
        } catch (Throwable ignored) {
        }
    }

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int fpsFrames = 0;
    private static int fpsCached = 60;

    private static void drawFps(MinecraftClient client, MatrixStack matrices) {
        try {
            if (client == null || matrices == null || client.textRenderer == null) {
                return;
            }
            fpsFrames++;
            long now = System.currentTimeMillis();
            if (now - fpsWindowStart >= 1000L) {
                fpsCached = fpsFrames;
                fpsFrames = 0;
                fpsWindowStart = now;
            }
            String text = fpsCached + " FPS";
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

            boolean w = client.options.keyForward != null && client.options.keyForward.isPressed();
            boolean a = client.options.keyLeft != null && client.options.keyLeft.isPressed();
            boolean s = client.options.keyBack != null && client.options.keyBack.isPressed();
            boolean d = client.options.keyRight != null && client.options.keyRight.isPressed();
            boolean space = client.options.keyJump != null && client.options.keyJump.isPressed();
            boolean lmb = client.options.keyAttack != null && client.options.keyAttack.isPressed();
            boolean rmb = client.options.keyUse != null && client.options.keyUse.isPressed();

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
}
