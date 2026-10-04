package com.dontcam.fabric.mixin;

import com.dontcam.fabric.DontCamBadges;
import com.dontcam.fabric.DontCamFabricMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public abstract class MixinPlayerListHud {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void dontcam$badge(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        try {
            if (cir == null) {
                return;
            }
            if (DontCamFabricMod.config != null && !DontCamFabricMod.config.badges) {
                return;
            }
            if (entry == null) {
                return;
            }
            UUID uuid;
            try {
                if (entry.getProfile() == null) {
                    return;
                }
                uuid = entry.getProfile().getId();
            } catch (Throwable t) {
                return;
            }
            if (uuid == null) {
                return;
            }
            boolean isDc;
            boolean owner;
            try {
                isDc = DontCamFabricMod.isDontCam(uuid);
                owner = DontCamFabricMod.isOwner(uuid);
            } catch (Throwable t) {
                return;
            }
            if (!isDc) {
                return;
            }
            Text original;
            try {
                original = cir.getReturnValue();
            } catch (Throwable t) {
                return;
            }
            if (original == null) {
                return;
            }
            Text patched;
            try {
                patched = DontCamBadges.badgeLine(original, owner);
            } catch (Throwable t) {
                return;
            }
            if (patched == null) {
                return;
            }
            try {
                cir.setReturnValue(patched);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dontcam$tagIcons(MatrixStack matrices, int scaledWindowWidth,
                                  Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
        try {
            if (matrices == null) {
                return;
            }
            if (DontCamFabricMod.config != null && !DontCamFabricMod.config.badges) {
                return;
            }
            MinecraftClient client;
            try {
                client = MinecraftClient.getInstance();
            } catch (Throwable t) {
                return;
            }
            if (client == null || client.player == null || client.player.networkHandler == null) {
                return;
            }
            List<PlayerListEntry> entries;
            try {
                entries = client.player.networkHandler.getPlayerList().stream()
                        .limit(80)
                        .toList();
            } catch (Throwable t) {
                return;
            }
            if (entries == null || entries.isEmpty()) {
                return;
            }
            int cols = (entries.size() + 19) / 20;
            cols = Math.max(1, Math.min(4, cols));
            int colWidth = Math.min(300, scaledWindowWidth / Math.max(1, cols) - 40);
            int totalW = cols * colWidth;
            int x0 = scaledWindowWidth / 2 - totalW / 2;
            int y0 = 20;
            for (int idx = 0; idx < entries.size(); idx++) {
                try {
                    PlayerListEntry entry = entries.get(idx);
                    if (entry == null || entry.getProfile() == null) {
                        continue;
                    }
                    UUID uuid = entry.getProfile().getId();
                    if (!DontCamFabricMod.isDontCam(uuid)) {
                        continue;
                    }
                    int col = idx / 20;
                    int row = idx % 20;
                    int x = x0 + col * colWidth + 4;
                    int y = y0 + row * 9;
                    DontCamBadges.drawTagIcon(matrices, x, y - 1, 8);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
