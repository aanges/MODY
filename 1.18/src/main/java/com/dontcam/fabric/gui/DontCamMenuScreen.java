package com.dontcam.fabric.gui;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.DontCamFabricMod;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class DontCamMenuScreen extends Screen {

    public DontCamMenuScreen() {
        super(new LiteralText("DontCam Client"));
    }

    private void addToggle(String label, boolean value, java.util.function.Consumer<Boolean> setter, int y) {
        int cx = this.width / 2;
        String state = value ? "\u00A7aON" : "\u00A7cOFF";
        this.addDrawableChild(new DontCamButtonWidget(cx - 100, y, 200, 20,
                new LiteralText(label + ": " + state),
                button -> {
                    setter.accept(!value);
                    DontCamFabricMod.config.save();
                    if (this.client != null) {
                        this.client.setScreen(new DontCamMenuScreen());
                    }
                }));
    }

    @Override
    protected void init() {
        int y = this.height / 2 - 44;
        addToggle("FPS Counter", DontCamFabricMod.config.fps, v -> DontCamFabricMod.config.fps = v, y);
        addToggle("CPS Counter", DontCamFabricMod.config.cps, v -> DontCamFabricMod.config.cps = v, y + 24);
        addToggle("Keystrokes", DontCamFabricMod.config.keystrokes, v -> DontCamFabricMod.config.keystrokes = v, y + 48);
        addToggle("DC Badges", DontCamFabricMod.config.badges, v -> DontCamFabricMod.config.badges = v, y + 72);
        this.addDrawableChild(new DontCamButtonWidget(this.width / 2 - 100, y + 100, 200, 20,
                new LiteralText("Done"),
                button -> {
                    DontCamFabricMod.config.save();
                    if (this.client != null) {
                        this.client.setScreen(null);
                    }
                }));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        TextRenderer tr = this.textRenderer;
        int cx = this.width / 2;
        int titleY = this.height / 2 - 70;
        String header = "\u00A7bDontCam \u00A79Client";
        int tw = tr.getWidth(header);
        DontCamBadges.drawTagIcon(matrices, cx - tw / 2 - 18, titleY - 4, 14);
        tr.drawWithShadow(matrices, header, cx - tw / 2.0f, titleY, 0xFFFFFF);
        String auth = DontCamFabricMod.localMicrosoft ? "\u00A7aMicrosoft" : "\u00A77Offline";
        int aw = tr.getWidth(auth);
        tr.drawWithShadow(matrices, auth, cx - aw / 2.0f, this.height / 2 - 58, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
