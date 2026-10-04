package com.dontcam.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

/**
 * DontCam main menu 1.8.9 (Lunar style): fully custom dark background +
 * gradient, banner with tag.png, no vanilla panorama / logo / splash / dirt.
 * Installed by replacing {@code GuiMainMenu} in {@code GuiOpenEvent}.
 *
 * <p>Layout (vertical, centered, cx = width/2):
 * [Singleplayer][Multiplayer] (one row, side by side)
 * [Minecraft Settings]
 * [DontCam Mods]
 * [Quit Game]
 */
public class GuiCustomMainMenu extends GuiScreen {

    private static final ResourceLocation TAG = new ResourceLocation("dontcam", "textures/tag.png");

    private static final int ID_SINGLE = 0;
    private static final int ID_MULTI = 1;
    private static final int ID_OPTIONS = 2;
    private static final int ID_MODS = 3;
    private static final int ID_QUIT = 4;

    @Override
    public void initGui() {
        int cx = this.width / 2;
        int y = this.height / 2 + 8;
        this.buttonList.add(new DontCamButton(ID_SINGLE, cx - 100, y, 98, 20, "Gra Jednoosobowa"));
        this.buttonList.add(new DontCamButton(ID_MULTI, cx + 2, y, 98, 20, "Wielosobowa"));
        this.buttonList.add(new DontCamButton(ID_OPTIONS, cx - 100, y + 24, 200, 20, "Ustawienia Minecraft"));
        this.buttonList.add(new DontCamButton(ID_MODS, cx - 100, y + 48, 200, 20, "Ustawienia Launchera"));
        this.buttonList.add(new DontCamButton(ID_QUIT, cx - 100, y + 72, 200, 20, "Wyjscie"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case ID_SINGLE:
                this.mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case ID_MULTI:
                this.mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case ID_OPTIONS:
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                break;
            case ID_MODS:
                this.mc.displayGuiScreen(new GuiDontCamMenu());
                break;
            case ID_QUIT:
                this.mc.shutdown();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Fully custom background: opaque dark + subtle vertical gradient. No dirt, no panorama.
        drawRect(0, 0, this.width, this.height, 0xFF0A0A12);
        drawGradientRect(0, 0, this.width, this.height / 2, 0xFF141422, 0x00141422);
        drawGradientRect(0, this.height / 2, this.width, this.height, 0x00000000, 0xFF050508);

        int cx = this.width / 2;
        drawRect(cx - 230, 18, cx + 230, 92, 0xCC0A0A12);
        drawRect(cx - 230, 18, cx + 230, 19, 0xFF2E9BFF);
        drawRect(cx - 230, 91, cx + 230, 92, 0xFF2E9BFF);
        try {
            Minecraft.getMinecraft().getTextureManager().bindTexture(TAG);
            drawModalRectWithCustomSizedTexture(cx - 118, 30, 0, 0, 24, 24, 24, 24);
        } catch (Exception ignored) {
        }
        this.drawCenteredString(this.fontRendererObj, "DontCam Client", cx + 12, 34, 0xFFFFFFFF);
        this.drawCenteredString(this.fontRendererObj, "\u00A7bminimal minecraft client", cx + 12, 52, 0xFFFFFFFF);
        this.drawCenteredString(this.fontRendererObj, "\u00A77v0.1.0  \u2022  1.8.9", cx + 12, 68, 0xFFFFFFFF);

        super.drawScreen(mouseX, mouseY, partialTicks);

        try {
            String left = "Minecraft 1.8.9";
            this.drawString(this.fontRendererObj, left, 4, this.height - 12, 0xFF64748B);
            String right = "DontCam v0.1.0";
            this.drawString(this.fontRendererObj, right,
                    this.width - this.fontRendererObj.getStringWidth(right) - 4, this.height - 12, 0xFF64748B);
        } catch (Exception ignored) {
        }
    }
}
