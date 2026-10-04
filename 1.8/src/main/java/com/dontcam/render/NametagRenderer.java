package com.dontcam.render;

import com.dontcam.DontCamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.UUID;

public class NametagRenderer {

    private static final int BLUE = 0xFF5555FF;
    private static final int CYAN = 0xFF55FFFF;
    private static final int GOLD = 0xFFFFAA00;

    @SubscribeEvent
    public void onRenderSpecials(RenderPlayerEvent.Specials.Pre event) {
        try {
            if (event == null || DontCamMod.config == null || !DontCamMod.config.badges) {
                return;
            }
            EntityPlayer player = event.entityPlayer;
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
            if (mc == null || mc.fontRendererObj == null || mc.getRenderManager() == null) {
                return;
            }
            FontRenderer font = mc.fontRendererObj;
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
            int nameW;
            try {
                nameW = font.getStringWidth(badge + name + crown);
            } catch (Exception ignored) {
                return;
            }
            int badgeW = 12;
            int total = nameW + badgeW;

            float scale = 0.016666668F * 1.6F;
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate((float) event.x, (float) event.y + player.height + 0.5F, (float) event.z);
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

                int cx = -total / 2 + 1;
                font.drawString("C", cx, 0, CYAN);
                font.drawString("D", cx - 1, 0, BLUE);
                font.drawString(badge + name + crown, cx + badgeW, 0, 0xFFFFFFFF);

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
