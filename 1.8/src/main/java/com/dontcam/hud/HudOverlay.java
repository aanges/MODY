package com.dontcam.hud;

import com.dontcam.DontCamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class HudOverlay {

    private static final int BG = 0x80000000;
    private static final int BG_PRESSED = 0xCCFFFFFF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xFFAAAAAA;

    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Text event) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.gameSettings == null || mc.gameSettings.showDebugInfo) {
                return;
            }
            if (mc.fontRendererObj == null || event == null || event.resolution == null) {
                return;
            }
            if (DontCamMod.config == null) {
                return;
            }
            if (DontCamMod.config.fps) {
                drawFps(mc);
            }
            if (DontCamMod.config.cps) {
                drawCps(mc);
            }
            if (DontCamMod.config.keystrokes) {
                drawKeystrokes(mc, event.resolution);
            }
        } catch (Exception ignored) {
        }
    }

    private void drawFps(Minecraft mc) {
        try {
            if (mc == null || mc.fontRendererObj == null) {
                return;
            }
            String text = Minecraft.getDebugFPS() + " FPS";
            int w = mc.fontRendererObj.getStringWidth(text);
            Gui.drawRect(5, 5, 9 + w, 18, BG);
            mc.fontRendererObj.drawString(text, 7, 7, TEXT);
        } catch (Exception ignored) {
        }
    }

    private void drawCps(Minecraft mc) {
        try {
            if (mc == null || mc.fontRendererObj == null) {
                return;
            }
            ClickTracker tracker = ClickTracker.get();
            int left = tracker != null ? tracker.leftCps() : 0;
            int right = tracker != null ? tracker.rightCps() : 0;
            String text = left + " | " + right + " CPS";
            int w = mc.fontRendererObj.getStringWidth(text);
            Gui.drawRect(5, 21, 9 + w, 34, BG);
            mc.fontRendererObj.drawString(text, 7, 23, TEXT);
        } catch (Exception ignored) {
        }
    }

    private void key(Minecraft mc, int x, int y, int w, int h, String label, boolean pressed) {
        try {
            if (mc == null || mc.fontRendererObj == null) {
                return;
            }
            Gui.drawRect(x, y, x + w, y + h, pressed ? BG_PRESSED : BG);
            int color = pressed ? 0xFF000000 : TEXT_DIM;
            int tw = mc.fontRendererObj.getStringWidth(label);
            mc.fontRendererObj.drawString(label, x + (w - tw) / 2, y + (h - 8) / 2, color);
        } catch (Exception ignored) {
        }
    }

    private void drawKeystrokes(Minecraft mc, ScaledResolution res) {
        try {
            if (mc == null || res == null) {
                return;
            }
            int key = 22;
            int gap = 2;
            int spaceH = 12;
            int totalW = key * 3 + gap * 2;
            int x0 = res.getScaledWidth() / 2 - totalW / 2;
            int y0 = res.getScaledHeight() - 68;

            boolean w = Keyboard.isKeyDown(Keyboard.KEY_W);
            boolean a = Keyboard.isKeyDown(Keyboard.KEY_A);
            boolean s = Keyboard.isKeyDown(Keyboard.KEY_S);
            boolean d = Keyboard.isKeyDown(Keyboard.KEY_D);
            boolean space = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
            boolean lmb = Mouse.isButtonDown(0);
            boolean rmb = Mouse.isButtonDown(1);

            key(mc, x0 + key + gap, y0, key, key, "W", w);
            key(mc, x0, y0 + key + gap, key, key, "A", a);
            key(mc, x0 + key + gap, y0 + key + gap, key, key, "S", s);
            key(mc, x0 + key * 2 + gap * 2, y0 + key + gap, key, key, "D", d);
            int sy = y0 + key * 2 + gap * 2;
            Gui.drawRect(x0, sy, x0 + totalW, sy + spaceH, space ? BG_PRESSED : BG);
            int my = sy + spaceH + gap;
            int half = (totalW - gap) / 2;
            key(mc, x0, my, half, 12, "LMB", lmb);
            key(mc, x0 + half + gap, my, totalW - half - gap, 12, "RMB", rmb);
        } catch (Exception ignored) {
        }
    }
}
