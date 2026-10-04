package com.dontcam.fabric.mixin;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.gui.DontCamButtonWidget;
import com.dontcam.fabric.gui.DontCamMenuScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lunar-style main menu: full dark background (hides vanilla panorama / logo /
 * splash), banner with tag.png, vertical centered layout:
 * [Singleplayer][Multiplayer] in one row, then Minecraft Settings,
 * DontCam Mods, Quit. Footer shows game + mod version.
 */
@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dontcam$replaceButtons(CallbackInfo ci) {
        try {
            this.clearChildren();
            int cx = this.width / 2;
            int y = this.height / 2 + 8;
            this.addDrawableChild(new DontCamButtonWidget(cx - 100, y, 98, 20,
                    new LiteralText("Gra Jednoosobowa"),
                    button -> {
                        try {
                            if (this.client != null) {
                                this.client.setScreen(new SelectWorldScreen(this));
                            }
                        } catch (Exception ignored) {
                        }
                    }));
            this.addDrawableChild(new DontCamButtonWidget(cx + 2, y, 98, 20,
                    new LiteralText("Wielosobowa"),
                    button -> {
                        try {
                            if (this.client != null) {
                                this.client.setScreen(new MultiplayerScreen(this));
                            }
                        } catch (Exception ignored) {
                        }
                    }));
            this.addDrawableChild(new DontCamButtonWidget(cx - 100, y + 24, 200, 20,
                    new LiteralText("Ustawienia Minecraft"),
                    button -> {
                        try {
                            if (this.client != null && this.client.options != null) {
                                this.client.setScreen(new OptionsScreen(this, this.client.options));
                            }
                        } catch (Exception ignored) {
                        }
                    }));
            this.addDrawableChild(new DontCamButtonWidget(cx - 100, y + 48, 200, 20,
                    new LiteralText("Ustawienia Launchera"),
                    button -> {
                        try {
                            if (this.client != null) {
                                this.client.setScreen(new DontCamMenuScreen());
                            }
                        } catch (Exception ignored) {
                        }
                    }));
            this.addDrawableChild(new DontCamButtonWidget(cx - 100, y + 72, 200, 20,
                    new LiteralText("Wyjscie"),
                    button -> {
                        try {
                            if (this.client != null) {
                                this.client.scheduleStop();
                            }
                        } catch (Exception ignored) {
                        }
                    }));
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dontcam$banner(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        try {
            if (this.textRenderer == null) {
                return;
            }
            TextRenderer tr = this.textRenderer;
            int cx = this.width / 2;
            DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xFF0A0A12);
            DrawableHelper.fill(matrices, 0, 0, this.width, this.height / 2, 0xFF141422);
            DrawableHelper.fill(matrices, cx - 230, 18, cx + 230, 92, 0xCC0A0A12);
            DrawableHelper.fill(matrices, cx - 230, 18, cx + 230, 19, 0xFF2E9BFF);
            DrawableHelper.fill(matrices, cx - 230, 91, cx + 230, 92, 0xFF2E9BFF);
            DontCamBadges.drawTagIcon(matrices, cx - 118, 30, 24);
            String title = "DontCam Client";
            tr.drawWithShadow(matrices, title, cx + 12 - tr.getWidth(title) / 2.0f, 34, 0xFFFFFFFF);
            String sub = "minimal minecraft client";
            tr.drawWithShadow(matrices, sub, cx + 12 - tr.getWidth(sub) / 2.0f, 52, 0xFF7DD3FC);
            String ver = "v0.1.0";
            try {
                if (this.client != null && this.client.getGameVersion() != null) {
                    ver += "  \u2022  " + this.client.getGameVersion();
                } else {
                    ver += "  \u2022  1.18.2";
                }
            } catch (Exception ignored) {
                ver = "v0.1.0  \u2022  1.18.2";
            }
            tr.drawWithShadow(matrices, ver, cx + 12 - tr.getWidth(ver) / 2.0f, 68, 0xFF64748B);
            String left = "Minecraft 1.18.2";
            try {
                if (this.client != null && this.client.getGameVersion() != null) {
                    left = "Minecraft " + this.client.getGameVersion();
                }
            } catch (Exception ignored) {
            }
            tr.drawWithShadow(matrices, left, 4, this.height - 12, 0xFF64748B);
            String right = "DontCam v0.1.0";
            tr.drawWithShadow(matrices, right, this.width - tr.getWidth(right) - 4, this.height - 12, 0xFF64748B);
        } catch (Exception ignored) {
        }
    }
}
