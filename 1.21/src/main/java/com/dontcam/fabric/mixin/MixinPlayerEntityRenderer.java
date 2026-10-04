package com.dontcam.fabric.mixin;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.DontCamFabricMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Custom nametag for DontCam players: "DC" badge before the nick,
 * gold star for the owner (Microsoft sessions only).
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @Shadow
    @Final
    private EntityRenderDispatcher dispatcher;

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void dontcam$label(AbstractClientPlayerEntity player, Text text, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light, float tickDelta,
                               CallbackInfo ci) {
        try {
            if (player == null || text == null || matrices == null || vertexConsumers == null || ci == null) {
                return;
            }
            // 1.21 has no badges toggle; keep a null-safe config guard so a
            // not-yet-loaded config can never NPE the nametag pipeline.
            if (DontCamFabricMod.config == null) {
                return;
            }
            UUID uuid;
            try {
                uuid = player.getUuid();
            } catch (Exception ignored) {
                return;
            }
            if (uuid == null || !DontCamFabricMod.isDontCam(uuid)) {
                return;
            }
            ci.cancel();

            boolean owner;
            try {
                owner = DontCamFabricMod.isOwner(uuid);
            } catch (Exception ignored) {
                owner = false;
            }
            MutableText line = DontCamBadges.badgeLine(text, owner);
            if (line == null) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) {
                return;
            }
            TextRenderer textRenderer = client.textRenderer;
            if (textRenderer == null) {
                return;
            }
            if (this.dispatcher == null || this.dispatcher.getRotation() == null) {
                return;
            }
            if (matrices.peek() == null) {
                return;
            }
            float icon = 8.0f;
            float gap = 2.0f;
            float width = -(textRenderer.getWidth(line) + icon + gap) / 2.0f;

            matrices.push();
            try {
                matrices.translate(0.0, player.getHeight() + 0.5, 0.0);
                matrices.multiply(this.dispatcher.getRotation());
                matrices.scale(-0.025f, -0.025f, 0.025f);
                // Icon first, then text shifted right so they sit side by side.
                DontCamBadges.drawTagIconWorld(matrices, width, -1.0f, icon);
                if (matrices.peek() == null) {
                    return;
                }
                float textX = width + icon + gap;
                textRenderer.draw(line, textX, 0.0f, 0xFFFFFFFF, false,
                        matrices.peek().getPositionMatrix(), vertexConsumers,
                        TextRenderer.TextLayerType.NORMAL, 0x80000000, light);
            } finally {
                matrices.pop();
            }
        } catch (Exception ignored) {
        }
    }
}
