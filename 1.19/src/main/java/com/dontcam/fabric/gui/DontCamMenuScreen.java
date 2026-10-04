package com.dontcam.fabric.gui;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.DontCamFabricMod;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** Right-Shift menu: toggle the DontCam modules. */
public class DontCamMenuScreen extends Screen {

    public DontCamMenuScreen() {
        super(Text.literal("DontCam Client"));
    }

    private void addToggle(String label, boolean value, java.util.function.Consumer<Boolean> setter, int y) {
        if (label == null || setter == null) {
            return;
        }
        int cx = this.width / 2;
        String state = value ? "\u00A7aON" : "\u00A7cOFF";
        this.addDrawableChild(new DontCamButtonWidget(cx - 100, y, 200, 20,
                Text.literal(label + ": " + state),
                button -> {
                    try {
                        setter.accept(!value);
                        if (DontCamFabricMod.config != null) {
                            DontCamFabricMod.config.save();
                        }
                        this.clearAndInit();
                    } catch (Exception ignored) {
                    }
                }));
    }

    @Override
    protected void init() {
        try {
            if (DontCamFabricMod.config == null) {
                return;
            }
            int y = this.height / 2 - 44;
            addToggle("FPS Counter", DontCamFabricMod.config.fps, v -> DontCamFabricMod.config.fps = v, y);
            addToggle("CPS Counter", DontCamFabricMod.config.cps, v -> DontCamFabricMod.config.cps = v, y + 24);
            addToggle("Keystrokes", DontCamFabricMod.config.keystrokes, v -> DontCamFabricMod.config.keystrokes = v, y + 48);
            addToggle("DC Badges", DontCamFabricMod.config.badges, v -> DontCamFabricMod.config.badges = v, y + 72);
            this.addDrawableChild(new DontCamButtonWidget(this.width / 2 - 100, y + 100, 200, 20,
                    Text.literal("Done"),
                    button -> {
                        try {
                            if (DontCamFabricMod.config != null) {
                                DontCamFabricMod.config.save();
                            }
                            this.close();
                        } catch (Exception ignored) {
                        }
                    }));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        try {
            if (matrices == null || this.textRenderer == null) {
                super.render(matrices, mouseX, mouseY, delta);
                return;
            }
            this.renderBackground(matrices);
            int cx = this.width / 2;
            int titleY = this.height / 2 - 70;
            String header = "\u00A7bDontCam \u00A79Client";
            int tw = this.textRenderer.getWidth(header);
            DontCamBadges.drawTagIcon(matrices, cx - tw / 2 - 18, titleY - 4, 14);
            this.textRenderer.drawWithShadow(matrices, Text.literal(header), cx - tw / 2.0f, titleY, 0xFFFFFF);
            String auth = DontCamFabricMod.localMicrosoft ? "\u00A7aMicrosoft" : "\u00A77Offline";
            int aw = this.textRenderer.getWidth(auth);
            this.textRenderer.drawWithShadow(matrices, Text.literal(auth), cx - aw / 2.0f, this.height / 2 - 58, 0xFFFFFF);
            super.render(matrices, mouseX, mouseY, delta);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
