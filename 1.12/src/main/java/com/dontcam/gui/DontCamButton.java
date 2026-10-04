package com.dontcam.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;

public class DontCamButton extends GuiButton {

    private static final int BG = 0xCC101018;
    private static final int BG_HOVER = 0xFF1B63D6;
    private static final int BORDER = 0xFF2E9BFF;
    private static final int BORDER_DIM = 0x552E9BFF;

    public DontCamButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        FontRenderer font = mc.fontRenderer;
        boolean hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
        this.hovered = hovered;
        int bg = hovered ? BG_HOVER : BG;
        int border = hovered ? BORDER : BORDER_DIM;
        int x = this.x;
        int y = this.y;
        int w = this.width;
        int h = this.height;
        Gui.drawRect(x + 1, y, x + w - 1, y + h, bg);
        Gui.drawRect(x, y + 1, x + w, y + h - 1, bg);
        Gui.drawRect(x + 1, y, x + w - 1, y + 1, border);
        Gui.drawRect(x + 1, y + h - 1, x + w, y + h, border);
        Gui.drawRect(x, y + 1, x + 1, y + h - 1, border);
        Gui.drawRect(x + w - 1, y + 1, x + w, y + h - 1, border);
        if (hovered) {
            Gui.drawRect(x + 1, y + 1, x + w - 1, y + 3, 0x33FFFFFF);
        }
        int color = this.enabled ? 0xFFFFFFFF : 0xFFA0A0A0;
        this.drawCenteredString(font, this.displayString, x + w / 2, y + (h - 8) / 2, color);
    }
}
