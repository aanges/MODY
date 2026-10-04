package com.dontcam.render;

import com.dontcam.DontCamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class NametagRenderer {

    private static final ResourceLocation TAG = new ResourceLocation("dontcam", "textures/tag.png");
    private static final int GOLD = 0xFFFFAA00;

    @SubscribeEvent
    public void onRenderSpecials(RenderPlayerEvent.Specials.Pre event) {
        try {
            if (event == null || DontCamMod.config == null || !DontCamMod.config.badges) {
                return;
            }
            EntityPlayer player = event.getEntityPlayer();
            if (player == null || player.isSneaking() || player.isInvisible()) {
                return;
            }
            UUID uuid;
            try {
                uuid = player.getUniqueID();
            } catch (Exception ignored) {
                return;
            }
            if (!DontCamMod.isDontCam(uuid)) {
                return;
            }
            event.setCanceled(true);

            boolean owner = false;
            try {
                owner = DontCamMod.isOwner(uuid);
            } catch (Exception ignored) {
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.fontRenderer == null || mc.getRenderManager() == null) {
                return;
            }
            FontRenderer font = mc.fontRenderer;
            RenderManager rm = mc.getRenderManager();

            String name = "";
            try {
                if (player.getDisplayName() != null) {
                    name = player.getDisplayName().getFormattedText();
                }
                if (name == null) {
                    name = "";
                }
            } catch (Exception ignored) {
                name = "";
            }
            String badge;
            String crown;
            try {
                badge = DontCamMod.badgePrefix(owner);
                crown = DontCamMod.crownSuffix(owner);
            } catch (Exception ignored) {
                badge = "";
                crown = "";
            }
            int icon = 8;
            int gap = 2;
            int total;
            try {
                total = icon + gap + font.getStringWidth(badge + name + crown);
            } catch (Exception ignored) {
                return;
            }

            float scale = 0.016666668F * 1.6F;
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate((float) event.getX(), (float) event.getY() + player.height + 0.5F, (float) event.getZ());
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-rm.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(rm.playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(-scale, -scale, scale);
                GlStateManager.disableLighting();
                GlStateManager.depthMask(false);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

                Gui.drawRect(-total / 2 - 1, -1, total / 2 + 1, 9, 0x80000000);

                float cx = -total / 2.0F;
                drawTagIcon(cx, -1.0F, icon);
                float textX = cx + icon + gap;
                font.drawString(badge + name + crown, (int) textX, 0, 0xFFFFFFFF);
                if (owner) {
                    drawCrown(-5, -8);
                }
            } finally {
                try {
                    GlStateManager.depthMask(true);
                    GlStateManager.enableDepth();
                    GlStateManager.disableBlend();
                    GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                    GlStateManager.popMatrix();
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void drawTagIcon(float x, float y, float size) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getTextureManager() == null) {
                return;
            }
            mc.getTextureManager().bindTexture(TAG);
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(x, y + size, 0.0D).tex(0.0D, 1.0D).endVertex();
            buffer.pos(x + size, y + size, 0.0D).tex(1.0D, 1.0D).endVertex();
            buffer.pos(x + size, y, 0.0D).tex(1.0D, 0.0D).endVertex();
            buffer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();
        } catch (Exception ignored) {
        }
    }

    private void drawCrown(int x, int y) {
        try {
            Gui.drawRect(x, y + 3, x + 10, y + 5, GOLD);
            Gui.drawRect(x, y, x + 2, y + 3, GOLD);
            Gui.drawRect(x + 4, y - 1, x + 6, y + 3, GOLD);
            Gui.drawRect(x + 8, y, x + 10, y + 3, GOLD);
        } catch (Exception ignored) {
        }
    }
}
