package com.speedruntools.client;

import com.speedruntools.config.SpeedrunConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

public class TimerHandler {

    public enum TimerState { IDLE, WAITING_FOR_START, RUNNING, STOPPED }

    public static TimerState state   = TimerState.IDLE;
    public static long  startTimeMs  = 0;
    public static long  stopTimeMs   = 0;
    public static long  elapsedMs    = 0;

    // Called by GUI when user presses Start
    public static void beginWaiting() {
        state       = TimerState.WAITING_FOR_START;
        elapsedMs   = 0;
    }

    public static void stop() {
        if (state == TimerState.RUNNING) {
            stopTimeMs = System.currentTimeMillis();
            elapsedMs  = stopTimeMs - startTimeMs;
            state      = TimerState.STOPPED;
        }
    }

    public static void reset() {
        state     = TimerState.IDLE;
        elapsedMs = 0;
    }

    // ── Auto-start on first player action ──────────────────────

    @SubscribeEvent
    public void onPlayerBreakBlock(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        tryStart();
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        tryStart();
    }

    @SubscribeEvent
    public void onPlayerMove(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity p = event.player;
        if (p.level.isClientSide &&
            (p.getDeltaMovement().lengthSqr() > 0.0001)) {
            tryStart();
        }
    }

    private void tryStart() {
        if (state == TimerState.WAITING_FOR_START) {
            startTimeMs = System.currentTimeMillis();
            state       = TimerState.RUNNING;
        }
    }

    // ── Tick: update elapsed ────────────────────────────────────

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (state == TimerState.RUNNING) {
            elapsedMs = System.currentTimeMillis() - startTimeMs;
        }
    }

    // ── Stop conditions ─────────────────────────────────────────

    // 1. Ender Dragon death
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!SpeedrunConfig.timerEnabled) return;
        if (SpeedrunConfig.stopCond != SpeedrunConfig.TimerStopCondition.ENDER_DRAGON_DEATH) return;
        if (event.getEntity() instanceof EnderDragonEntity) {
            stop();
        }
    }

    // 2. Player enters End portal (detected by dimension change to Overworld after being in The End)
    //    We track dimension transitions client-side via PlayerChangedDimensionEvent
    private boolean wasInEnd = false;

    @SubscribeEvent
    public void onDimensionChange(net.minecraftforge.event.entity.EntityTravelToDimensionEvent event) {
        if (!SpeedrunConfig.timerEnabled) return;
        if (SpeedrunConfig.stopCond != SpeedrunConfig.TimerStopCondition.END_PORTAL_ENTER) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // If player is in The End and travelling to Overworld → they entered end portal
        if (mc.player.level.dimension() == World.END) {
            stop();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    public static String getFormattedTime() {
        long ms = (state == TimerState.RUNNING)
            ? System.currentTimeMillis() - startTimeMs
            : elapsedMs;
        long totalSec = ms / 1000;
        long hours    = totalSec / 3600;
        long mins     = (totalSec % 3600) / 60;
        long secs     = totalSec % 60;
        long centis   = (ms % 1000) / 10;
        if (hours > 0) {
            return String.format("%d:%02d:%02d.%02d", hours, mins, secs, centis);
        }
        return String.format("%d:%02d.%02d", mins, secs, centis);
    }
}
