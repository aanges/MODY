package com.dontcam.hud;

import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;

public class ClickTracker {

    private static final long WINDOW_NS = 1000000000L;

    private final Deque<Long> left = new ArrayDeque<Long>();
    private final Deque<Long> right = new ArrayDeque<Long>();

    private static ClickTracker instance;

    public ClickTracker() {
        instance = this;
    }

    public static ClickTracker get() {
        return instance;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent event) {
        if (event == null || !event.isButtonstate()) {
            return;
        }
        long now = System.nanoTime();
        if (event.getButton() == 0) {
            synchronized (left) {
                left.addLast(now);
            }
        } else if (event.getButton() == 1) {
            synchronized (right) {
                right.addLast(now);
            }
        }
    }

    private static int prune(Deque<Long> queue, long now) {
        synchronized (queue) {
            while (!queue.isEmpty() && now - queue.peekFirst() > WINDOW_NS) {
                queue.pollFirst();
            }
            return queue.size();
        }
    }

    public int leftCps() {
        return prune(left, System.nanoTime());
    }

    public int rightCps() {
        return prune(right, System.nanoTime());
    }
}
