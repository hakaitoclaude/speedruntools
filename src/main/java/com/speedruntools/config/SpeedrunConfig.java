package com.speedruntools.config;

/**
 * Central config store (in-memory; persisted via ForgeConfigSpec if desired).
 * All fields are static for easy access throughout the mod.
 */
public class SpeedrunConfig {

    // ── Fullbright ──────────────────────────────────────────────
    public static boolean fullbrightEnabled = true;

    // ── Zoom ────────────────────────────────────────────────────
    public static float zoomFovDefault = 30f;   // FOV when fully zoomed
    public static float zoomFovStep   = 5f;     // scroll step

    // ── Timer ────────────────────────────────────────────────────
    public enum TimerStopCondition { ENDER_DRAGON_DEATH, END_PORTAL_ENTER, MANUAL }

    public static boolean timerEnabled        = false;
    public static TimerStopCondition stopCond = TimerStopCondition.MANUAL;

    // ── Nether Calc ──────────────────────────────────────────────
    // (no persistent config needed – purely UI)

    // ── Stronghold Finder ────────────────────────────────────────
    // (no persistent config needed – purely UI)
}
