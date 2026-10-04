package com.dontcam.fabric;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

public final class DontCamBadges {

    public static final Identifier TAG_TEXTURE = new Identifier("dontcam", "textures/tag.png");

    private DontCamBadges() {
    }

    public static MutableText badgeLine(Text name, boolean owner) {
        try {
            Text safe = name != null ? name : new LiteralText("");
            Text copy;
            try {
                copy = safe.copy();
            } catch (Throwable t) {
                copy = new LiteralText("");
            }
            if (copy == null) {
                copy = new LiteralText("");
            }
            MutableText line = new LiteralText("D").formatted(Formatting.BLUE, Formatting.BOLD)
                    .append(new LiteralText("C").formatted(Formatting.AQUA, Formatting.BOLD))
                    .append(" ")
                    .append(copy);
            if (owner) {
                line.append(new LiteralText(" \u2605").formatted(Formatting.GOLD, Formatting.BOLD));
            }
            return line;
        } catch (Throwable t) {
            try {
                return new LiteralText("DC");
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    public static void drawTagIcon(MatrixStack matrices, int x, int y, int size) {
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
            if (client == null || client.getTextureManager() == null) {
                return;
            }
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderTexture(0, TAG_TEXTURE);
            DrawableHelper.drawTexture(matrices, x, y, 0, 0, size, size, size, size);
        } catch (Throwable ignored) {
        }
    }

    public static void drawTagIconWorld(MatrixStack matrices, float x, float y, float size) {
        try {
            if (matrices == null) {
                return;
            }
            if (matrices.peek() == null || matrices.peek().getModel() == null) {
                return;
            }
            MinecraftClient client;
            try {
                client = MinecraftClient.getInstance();
            } catch (Throwable t) {
                return;
            }
            if (client == null || client.getTextureManager() == null) {
                return;
            }
            client.getTextureManager().bindTexture(TAG_TEXTURE);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TAG_TEXTURE);
            Matrix4f matrix = matrices.peek().getModel();
            if (matrix == null) {
                return;
            }
            Tessellator tessellator;
            try {
                tessellator = Tessellator.getInstance();
            } catch (Throwable t) {
                return;
            }
            if (tessellator == null || tessellator.getBuffer() == null) {
                return;
            }
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(matrix, x, y + size, 0.0f).texture(0.0f, 1.0f).next();
            buffer.vertex(matrix, x + size, y + size, 0.0f).texture(1.0f, 1.0f).next();
            buffer.vertex(matrix, x + size, y, 0.0f).texture(1.0f, 0.0f).next();
            buffer.vertex(matrix, x, y, 0.0f).texture(0.0f, 0.0f).next();
            tessellator.draw();
        } catch (Throwable ignored) {
        }
    }
}
