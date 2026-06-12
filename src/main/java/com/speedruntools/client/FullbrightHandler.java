package com.speedruntools.client;

import com.speedruntools.config.SpeedrunConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FullbrightHandler {

    private static final float FULLBRIGHT_GAMMA = 100.0f;
    private float originalGamma = -1f;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (SpeedrunConfig.fullbrightEnabled) {
            if (originalGamma < 0) {
                originalGamma = mc.options.gamma;
            }
            // Force gamma every tick so other mods don't override
            mc.options.gamma = FULLBRIGHT_GAMMA;
        } else {
            if (originalGamma >= 0) {
                mc.options.gamma = originalGamma;
                originalGamma = -1f;
            }
        }
    }
}
