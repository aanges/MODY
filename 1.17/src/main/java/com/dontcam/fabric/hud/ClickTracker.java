package com.dontcam.fabric.hud;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClickTracker {

    private static final long WINDOW_NS = 1000000000L;

    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();
    private boolean lastAttack = false;
    private boolean lastUse = false;

    private static ClickTracker instance;

    public static ClickTracker get() {
        return instance;
    }

    public static void init() {
        instance = new ClickTracker();
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            try {
                if (client == null || client.player == null || instance == null) {
                    return;
                }
                if (client.options == null) {
                    return;
                }
                if (client.options.keyAttack == null || client.options.keyUse == null) {
                    return;
                }
                long now = System.nanoTime();
                boolean attack;
                boolean use;
                try {
                    attack = client.options.keyAttack.isPressed();
                    use = client.options.keyUse.isPressed();
                } catch (Throwable t) {
                    return;
                }
                if (attack && !instance.lastAttack) {
                    synchronized (instance.left) {
                        instance.left.addLast(now);
                    }
                }
                if (use && !instance.lastUse) {
                    synchronized (instance.right) {
                        instance.right.addLast(now);
                    }
                }
                instance.lastAttack = attack;
                instance.lastUse = use;
            } catch (Throwable ignored) {
            }
        });
    }

    private static int prune(Deque<Long> queue, long now) {
        if (queue == null) {
            return 0;
        }
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() != null && now - queue.peekFirst() > WINDOW_NS) {
                queue.pollFirst();
            }
            return queue.size();
        }
    }

    public int leftCps() {
        try {
            return prune(left, System.nanoTime());
        } catch (Throwable t) {
            return 0;
        }
    }

    public int rightCps() {
        try {
            return prune(right, System.nanoTime());
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void onClientTick(MinecraftClient client) {
    }
}
