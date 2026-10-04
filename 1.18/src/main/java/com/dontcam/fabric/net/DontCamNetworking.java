package com.dontcam.fabric.net;

import com.dontcam.fabric.DontCamFabricMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Roster sync with a DontCam-aware server (1.20.1-era networking API:
 * raw Identifier + PacketByteBuf, no CustomPayload records).
 *
 * <p>Client announces itself on join; a companion server plugin may answer with a
 * roster payload. Without one, only the local badge/crown render.
 */
public class DontCamNetworking {

    public static final Identifier HELLO = new Identifier("dontcam", "hello");
    public static final Identifier ROSTER = new Identifier("dontcam", "roster");

    public static void init() {
        try {
            ClientPlayNetworking.registerGlobalReceiver(ROSTER, (client, handler, buf, responseSender) -> {
                try {
                    if (buf == null || client == null) {
                        return;
                    }
                    byte[] bytes;
                    try {
                        bytes = new byte[buf.readableBytes()];
                        buf.readBytes(bytes);
                    } catch (Throwable t) {
                        return;
                    }
                    String json;
                    try {
                        json = new String(bytes, StandardCharsets.UTF_8);
                    } catch (Throwable t) {
                        return;
                    }
                    if (json == null || json.isEmpty()) {
                        return;
                    }
                    final String rosterJson = json;
                    try {
                        client.execute(() -> applyRoster(rosterJson));
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }

        try {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
                try {
                    if (handler == null && sender == null && client == null) {
                        return;
                    }
                    if (client == null) {
                        return;
                    }
                    boolean canSend;
                    try {
                        canSend = ClientPlayNetworking.canSend(HELLO);
                    } catch (Throwable t) {
                        return;
                    }
                    if (!canSend) {
                        return;
                    }
                    String uuid = DontCamFabricMod.localUuid != null
                            ? DontCamFabricMod.localUuid.toString().replace("-", "")
                            : "";
                    String hello = "{\"uuid\":\"" + uuid + "\",\"microsoft\":" + DontCamFabricMod.localMicrosoft
                            + ",\"mod\":\"" + DontCamFabricMod.VERSION + "\"}";
                    PacketByteBuf buf;
                    try {
                        buf = new PacketByteBuf(Unpooled.buffer());
                        buf.writeString(hello);
                    } catch (Throwable t) {
                        return;
                    }
                    try {
                        ClientPlayNetworking.send(HELLO, buf);
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    static void applyRoster(String json) {
        try {
            if (json == null || json.isEmpty()) {
                return;
            }
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root == null) {
                return;
            }
            JsonArray players = root.getAsJsonArray("players");
            if (players == null) {
                return;
            }
            if (DontCamFabricMod.roster == null || DontCamFabricMod.rosterMicrosoft == null) {
                return;
            }
            DontCamFabricMod.roster.clear();
            DontCamFabricMod.rosterMicrosoft.clear();
            for (JsonElement element : players) {
                if (element == null || !element.isJsonObject()) {
                    continue;
                }
                JsonObject player = element.getAsJsonObject();
                if (!player.has("uuid")) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(player.get("uuid").getAsString());
                    boolean microsoft = player.has("microsoft") && player.get("microsoft").getAsBoolean();
                    DontCamFabricMod.roster.add(uuid);
                    DontCamFabricMod.rosterMicrosoft.put(uuid, microsoft);
                } catch (IllegalArgumentException ignored) {
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
