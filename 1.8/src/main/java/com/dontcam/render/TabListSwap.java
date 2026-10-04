package com.dontcam.render;

import com.dontcam.DontCamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.lang.reflect.Field;
import java.util.UUID;

public class TabListSwap {

    private static Field overlayField;

    public static class DontCamTabOverlay extends GuiPlayerTabOverlay {

        public DontCamTabOverlay(Minecraft mc, GuiIngame gui) {
            super(mc, gui);
        }

        @Override
        public String getPlayerName(NetworkPlayerInfo info) {
            String name;
            try {
                name = super.getPlayerName(info);
            } catch (Exception e) {
                return "";
            }
            try {
                if (DontCamMod.config == null || !DontCamMod.config.badges || info == null || info.getGameProfile() == null) {
                    return name;
                }
                UUID uuid = info.getGameProfile().getId();
                if (!DontCamMod.isDontCam(uuid)) {
                    return name;
                }
                return DontCamMod.badgePrefix(true) + name + DontCamMod.crownSuffix(DontCamMod.isOwner(uuid));
            } catch (Exception ignored) {
                return name;
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event == null || event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.ingameGUI == null) {
                return;
            }
            if (overlayField == null) {
                overlayField = findOverlayField();
            }
            if (overlayField != null) {
                Object current = overlayField.get(mc.ingameGUI);
                if (!(current instanceof DontCamTabOverlay)) {
                    overlayField.set(mc.ingameGUI, new DontCamTabOverlay(mc, mc.ingameGUI));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static Field findOverlayField() {
        for (String name : new String[]{"overlayPlayerList", "field_175196_v"}) {
            try {
                Field f = GuiIngame.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
