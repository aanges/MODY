package com.dontcam.net;

import com.dontcam.DontCamMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RosterSync {

    @SubscribeEvent
    public void onCustomPacket(FMLNetworkEvent.ClientCustomPacketEvent event) {
        try {
            if (event == null || event.getPacket() == null || event.getPacket().payload() == null) {
                return;
            }
            String channel;
            try {
                channel = event.getPacket().channel();
            } catch (Exception e) {
                return;
            }
            if (!DontCamMod.CHANNEL_ROSTER.equals(channel)) {
                return;
            }
            try {
                byte[] bytes = new byte[event.getPacket().payload().readableBytes()];
                event.getPacket().payload().readBytes(bytes);
                String json = new String(bytes, StandardCharsets.UTF_8);
                JsonObject root = new JsonParser().parse(json).getAsJsonObject();
                if (root == null) {
                    return;
                }
                JsonArray players = root.getAsJsonArray("players");
                if (players == null) {
                    return;
                }
                if (DontCamMod.roster == null || DontCamMod.rosterMicrosoft == null) {
                    return;
                }
                DontCamMod.roster.clear();
                DontCamMod.rosterMicrosoft.clear();
                for (JsonElement element : players) {
                    if (element == null || !element.isJsonObject()) {
                        continue;
                    }
                    JsonObject player = element.getAsJsonObject();
                    if (player == null || !player.has("uuid")) {
                        continue;
                    }
                    try {
                        UUID uuid = UUID.fromString(player.get("uuid").getAsString());
                        boolean microsoft = player.has("microsoft") && player.get("microsoft").getAsBoolean();
                        DontCamMod.roster.add(uuid);
                        DontCamMod.rosterMicrosoft.put(uuid, microsoft);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }
}
