package com.dontcam.fabric.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class DontCamButtonWidget extends ButtonWidget {

    private static final int BG = 0xCC101018;
    private static final int BG_HOVER = 0xFF1B63D6;
    private static final int BORDER = 0xFF2E9BFF;
    private static final int BORDER_DIM = 0x552E9BFF;

    private final int dcX;
    private final int dcY;

    public DontCamButtonWidget(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress);
        this.dcX = x;
        this.dcY = y;
    }

    @Override
    public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        boolean hovered = this.isHovered();
        int bg = hovered ? BG_HOVER : BG;
        int border = hovered ? BORDER : BORDER_DIM;
        int x = this.dcX;
        int y = this.dcY;
        int w = this.width;
        int h = this.height;
        DrawableHelper.fill(matrices, x + 1, y, x + w - 1, y + h, bg);
        DrawableHelper.fill(matrices, x, y + 1, x + w, y + h - 1, bg);
        DrawableHelper.fill(matrices, x + 1, y, x + w - 1, y + 1, border);
        DrawableHelper.fill(matrices, x + 1, y + h - 1, x + w, y + h, border);
        DrawableHelper.fill(matrices, x, y + 1, x + 1, y + h - 1, border);
        DrawableHelper.fill(matrices, x + w - 1, y + 1, x + w, y + h - 1, border);
        if (hovered) {
            DrawableHelper.fill(matrices, x + 1, y + 1, x + w - 1, y + 3, 0x33FFFFFF);
        }
        int color = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        String msg = this.getMessage().getString();
        int tw = tr.getWidth(msg);
        tr.drawWithShadow(matrices, msg, x + (w - tw) / 2.0f, y + (h - 8) / 2.0f, color);
    }
}
