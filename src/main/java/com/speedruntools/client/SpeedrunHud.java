package com.speedruntools.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.speedruntools.config.SpeedrunConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SpeedrunHud {

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!SpeedrunConfig.timerEnabled) return;
        if (TimerHandler.state == TimerHandler.TimerState.IDLE) return;

        FontRenderer font = mc.font;
        String time = TimerHandler.getFormattedTime();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - font.width(time) - 4;
        int y = 4;

        MatrixStack ms = event.getMatrixStack();
        // Shadow + yellow text (same size as debug screen = scale 1, 6px font)
        font.drawShadow(ms, time, x, y, 0xFFFF55); // Yellow
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // O key opens the SpeedrunTools GUI
        while (KeyBindings.openGuiKey.consumeClick()) {
            mc.setScreen(new SpeedrunGuiScreen(mc.screen));
        }
    }
}
