package com.dontcam.fabric.mixin;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.DontCamFabricMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Custom nametag for DontCam players: tag.png icon + "DC" badge before the nick,
 * gold star for the owner (Microsoft sessions only).
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void dontcam$label(AbstractClientPlayerEntity player, Text text, MatrixStack matrices,
                               VertexConsumerProvider vertexConsumers, int light,
                               CallbackInfo ci) {
        try {
            // config == null means "not loaded yet" -> treat badges as ON, never NPE.
            if (DontCamFabricMod.config != null && !DontCamFabricMod.config.badges) {
                return;
            }
            if (player == null || text == null || matrices == null || vertexConsumers == null || ci == null) {
                return;
            }
            UUID uuid;
            try {
                uuid = player.getUuid();
            } catch (Throwable t) {
                return;
            }
            if (uuid == null || !DontCamFabricMod.isDontCam(uuid)) {
                return;
            }
            boolean owner;
            try {
                owner = DontCamFabricMod.isOwner(uuid);
            } catch (Throwable t) {
                owner = false;
            }
            MutableText line;
            try {
                line = DontCamBadges.badgeLine(text, owner);
            } catch (Throwable t) {
                return;
            }
            if (line == null) {
                return;
            }
            MinecraftClient client;
            try {
                client = MinecraftClient.getInstance();
            } catch (Throwable t) {
                return;
            }
            if (client == null) {
                return;
            }
            TextRenderer textRenderer = client.textRenderer;
            if (textRenderer == null) {
                return;
            }
            try {
                if (client.getEntityRenderDispatcher() == null
                        || client.getEntityRenderDispatcher().getRotation() == null) {
                    return;
                }
            } catch (Throwable t) {
                return;
            }
            float width;
            try {
                width = -(textRenderer.getWidth(line.getString()) + 8.0f + 2.0f) / 2.0f;
            } catch (Throwable t) {
                return;
            }
            float icon = 8.0f;
            float gap = 2.0f;
            try {
                matrices.push();
            } catch (Throwable t) {
                return;
            }
            try {
                float height;
                try {
                    height = player.getHeight();
                } catch (Throwable t) {
                    height = 1.8f;
                }
                matrices.translate(0.0, height + 0.5, 0.0);
                matrices.multiply(client.getEntityRenderDispatcher().getRotation());
                matrices.scale(-0.025f, -0.025f, 0.025f);
                if (matrices.peek() == null || matrices.peek().getModel() == null) {
                    return;
                }
                ci.cancel();
                // Icon first, then text shifted right so they sit side by side.
                DontCamBadges.drawTagIconWorld(matrices, width, -1.0f, icon);
                float textX = width + icon + gap;
                if (matrices.peek() == null || matrices.peek().getModel() == null) {
                    return;
                }
                textRenderer.draw(line, textX, 0.0f, 0xFFFFFFFF, false,
                        matrices.peek().getModel(), vertexConsumers,
                        false, 0x80000000, light);
            } finally {
                try {
                    matrices.pop();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
