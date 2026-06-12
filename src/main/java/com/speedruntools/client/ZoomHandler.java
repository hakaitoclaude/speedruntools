package com.speedruntools.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;

public class ZoomHandler {

    private static final float BASE_ZOOM_FOV = 30f;
    private static final float SCROLL_STEP   = 5f;
    private static final float MIN_FOV       = 5f;
    private static final float MAX_FOV       = 90f;

    private boolean wasZooming = false;
    private float   currentZoomFov = BASE_ZOOM_FOV;

    // Call this to smoothly interpolate FOV
    private float currentFov   = -1f;
    private float targetFov    = -1f;
    private float defaultFov   = -1f;

    @SubscribeEvent
    public void onFOVUpdate(FOVUpdateEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        boolean zooming = KeyBindings.zoomKey.isDown();

        if (zooming) {
            // Store original FOV first time
            if (!wasZooming) {
                defaultFov = event.getFov();
                currentFov = defaultFov;
                targetFov  = currentZoomFov;
            }
            // Smooth interpolation
            currentFov += (targetFov - currentFov) * 0.3f;
            event.setNewfov(currentFov / mc.options.fov);
        } else {
            if (wasZooming) {
                // Snap back
                currentFov   = defaultFov;
                currentZoomFov = BASE_ZOOM_FOV; // reset scroll zoom
            }
        }
        wasZooming = zooming;
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!KeyBindings.zoomKey.isDown()) return;
        if (mc.screen != null) return; // don't interfere with open GUIs

        double delta = event.getScrollDelta();
        currentZoomFov -= (float) delta * SCROLL_STEP;
        currentZoomFov  = Math.max(MIN_FOV, Math.min(MAX_FOV, currentZoomFov));
        targetFov = currentZoomFov;

        event.setCanceled(true);
    }
}
