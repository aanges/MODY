package com.dontcam;

import com.dontcam.gui.GuiCustomMainMenu;
import com.dontcam.gui.GuiDontCamMenu;
import com.dontcam.hud.ClickTracker;
import com.dontcam.hud.HudOverlay;
import com.dontcam.net.RosterSync;
import com.dontcam.render.NametagRenderer;
import com.dontcam.render.TabListSwap;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.lwjgl.input.Keyboard;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod(modid = DontCamMod.MODID, name = "DontCam Client Mod", version = "0.1.0",
        acceptedMinecraftVersions = "[1.8.9]", acceptableRemoteVersions = "*")
public class DontCamMod {

    public static final String MODID = "dontcam";
    public static final String VERSION = "0.1.0";

    public static final UUID OWNER_UUID = UUID.fromString("76b1ef92-f047-428a-a068-ccd4d1eb4863");

    public static final String CHANNEL_HELLO = "DontCam|Hello";
    public static final String CHANNEL_ROSTER = "DontCam|Roster";

    public static KeyBinding openMenu;
    public static DontCamConfig config;

    public static final Set<UUID> roster = Collections.synchronizedSet(new HashSet<UUID>());
    public static final Map<UUID, Boolean> rosterMicrosoft = new ConcurrentHashMap<UUID, Boolean>();

    public static boolean localMicrosoft = false;
    public static UUID localUuid = null;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new DontCamConfig(event.getModConfigurationDirectory());

        localMicrosoft = "msa".equalsIgnoreCase(System.getProperty("dontcam.auth", ""));
        try {
            String raw = System.getProperty("dontcam.uuid", "");
            if (raw != null && !raw.isEmpty()) {
                localUuid = UUID.fromString(raw);
            }
        } catch (Exception ignored) {
            localUuid = null;
        }

        openMenu = new KeyBinding("key.dontcam.menu", Keyboard.KEY_RSHIFT, "key.categories.dontcam");
        ClientRegistry.registerKeyBinding(openMenu);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ClickTracker());
        MinecraftForge.EVENT_BUS.register(new HudOverlay());
        MinecraftForge.EVENT_BUS.register(new NametagRenderer());
        MinecraftForge.EVENT_BUS.register(new RosterSync());
        MinecraftForge.EVENT_BUS.register(new TabListSwap());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        try {
            if (event == null || event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.thePlayer == null || mc.currentScreen != null) {
                return;
            }
            if (openMenu != null && openMenu.isPressed()) {
                mc.displayGuiScreen(new GuiDontCamMenu());
            }
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        try {
            if (event != null && event.gui instanceof GuiMainMenu) {
                event.gui = new GuiCustomMainMenu();
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isDontCam(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        if (uuid.equals(localUuid)) {
            return true;
        }
        return roster.contains(uuid);
    }

    public static boolean isOwner(UUID uuid) {
        if (uuid == null || !OWNER_UUID.equals(uuid)) {
            return false;
        }
        if (uuid.equals(localUuid)) {
            return localMicrosoft;
        }
        Boolean ms = rosterMicrosoft.get(uuid);
        return ms != null && ms.booleanValue();
    }

    public static String badgePrefix(boolean owner) {
        return "\u00A79\u00A7lD\u00A7b\u00A7lC\u00A7r ";
    }

    public static String crownSuffix(boolean owner) {
        return owner ? " \u00A76\u00A7l\u2605" : "";
    }

    public static void sendHello() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) {
                return;
            }
            String register = CHANNEL_HELLO + "\0" + CHANNEL_ROSTER;
            mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload("REGISTER",
                    new PacketBuffer(Unpooled.copiedBuffer(register.getBytes(StandardCharsets.UTF_8)))));
            String uuid = localUuid != null ? localUuid.toString().replace("-", "") : "";
            String hello = "{\"uuid\":\"" + uuid + "\",\"microsoft\":" + localMicrosoft + ",\"mod\":\"" + VERSION + "\"}";
            mc.getNetHandler().addToSendQueue(new C17PacketCustomPayload(CHANNEL_HELLO,
                    new PacketBuffer(Unpooled.copiedBuffer(hello.getBytes(StandardCharsets.UTF_8)))));
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public void onJoinServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        roster.clear();
        rosterMicrosoft.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null) {
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            sendHello();
                        }
                    });
                }
            }
        }, "DontCam-Hello").start();
    }
}
