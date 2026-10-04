package com.dontcam.fabric.net;

import com.dontcam.fabric.DontCamFabricMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * Roster sync with a DontCam-aware server (1.21.1 CustomPayload API).
 *
 * <p>Client announces itself on join; a companion server plugin may answer with a
 * roster payload. Without one, only the local badge/crown render.
 */
public class DontCamNetworking {

    public record HelloPayload(String json) implements CustomPayload {
        public static final CustomPayload.Id<HelloPayload> ID =
                new CustomPayload.Id<>(Identifier.of("dontcam", "hello"));
        public static final PacketCodec<io.netty.buffer.ByteBuf, HelloPayload> CODEC =
                PacketCodecs.STRING.xmap(HelloPayload::new, HelloPayload::json);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record RosterPayload(String json) implements CustomPayload {
        public static final CustomPayload.Id<RosterPayload> ID =
                new CustomPayload.Id<>(Identifier.of("dontcam", "roster"));
        public static final PacketCodec<io.netty.buffer.ByteBuf, RosterPayload> CODEC =
                PacketCodecs.STRING.xmap(RosterPayload::new, RosterPayload::json);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public static void init() {
        try {
            PayloadTypeRegistry.playC2S().register(HelloPayload.ID, HelloPayload.CODEC);
            PayloadTypeRegistry.playS2C().register(RosterPayload.ID, RosterPayload.CODEC);
        } catch (Throwable ignored) {
        }

        try {
            ClientPlayNetworking.registerGlobalReceiver(RosterPayload.ID, (payload, context) -> {
                try {
                    if (payload == null || context == null || context.client() == null) {
                        return;
                    }
                    String json = payload.json();
                    if (json == null || json.isEmpty()) {
                        return;
                    }
                    try {
                        context.client().execute(() -> applyRoster(json));
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
                    if (handler == null || sender == null || client == null) {
                        return;
                    }
                    String uuid = DontCamFabricMod.localUuid != null
                            ? DontCamFabricMod.localUuid.toString().replace("-", "")
                            : "";
                    String hello = "{\"uuid\":\"" + uuid + "\",\"microsoft\":" + DontCamFabricMod.localMicrosoft
                            + ",\"mod\":\"" + DontCamFabricMod.VERSION + "\"}";
                    try {
                        sender.sendPacket(new HelloPayload(hello));
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
            if (DontCamFabricMod.roster == null || DontCamFabricMod.rosterMicrosoft == null) {
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
