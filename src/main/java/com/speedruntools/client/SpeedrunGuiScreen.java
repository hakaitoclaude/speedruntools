package com.speedruntools.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.speedruntools.config.SpeedrunConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;

public class SpeedrunGuiScreen extends Screen {

    private final Screen parent;

    // Which tab is open: MAIN, TIMER, NETHER, STRONGHOLD
    private enum Tab { MAIN, TIMER_CONFIG, TIMER_START, NETHER, STRONGHOLD }
    private Tab currentTab = Tab.MAIN;

    // Timer config state
    private boolean timerStopDragon = false;
    private boolean timerStopPortal = false;
    private boolean timerStopManual = false;

    // Nether calc state
    private String netherInputX = "", netherInputZ = "";
    private boolean netherDirOwToN = true; // true = OW→Nether, false = Nether→OW
    private String netherResultX = "—", netherResultZ = "—";

    public SpeedrunGuiScreen(Screen parent) {
        super(new StringTextComponent("SpeedrunTools"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildButtons();
    }

    private void buildButtons() {
        this.buttons.clear();
        this.children.clear();

        switch (currentTab) {
            case MAIN:       buildMain();       break;
            case TIMER_CONFIG: buildTimerConfig(); break;
            case TIMER_START:  buildTimerStart();  break;
            case NETHER:     buildNether();     break;
            case STRONGHOLD: buildStronghold(); break;
        }
    }

    // ── MAIN ────────────────────────────────────────────────────

    private void buildMain() {
        int cx = width / 2;
        int cy = height / 2 - 60;

        addButton(new Button(cx - 100, cy, 200, 20,
            new StringTextComponent("暗視: " + (SpeedrunConfig.fullbrightEnabled ? "§aオン" : "§cオフ")),
            b -> {
                SpeedrunConfig.fullbrightEnabled = !SpeedrunConfig.fullbrightEnabled;
                buildButtons();
            }));

        addButton(new Button(cx - 100, cy + 26, 200, 20,
            new StringTextComponent("タイマー"),
            b -> { currentTab = Tab.TIMER_CONFIG; buildButtons(); }));

        addButton(new Button(cx - 100, cy + 52, 200, 20,
            new StringTextComponent("現世とネザーの距離計測"),
            b -> { currentTab = Tab.NETHER; buildButtons(); }));

        addButton(new Button(cx - 100, cy + 78, 200, 20,
            new StringTextComponent("エンド要塞特定"),
            b -> { currentTab = Tab.STRONGHOLD; buildButtons(); }));

        addButton(new Button(cx - 100, cy + 110, 200, 20,
            new StringTextComponent("閉じる"),
            b -> onClose()));
    }

    // ── TIMER CONFIG ─────────────────────────────────────────────

    private void buildTimerConfig() {
        int cx = width / 2;
        int cy = height / 2 - 80;

        // Enable/disable
        addButton(new Button(cx - 100, cy, 200, 20,
            new StringTextComponent("タイマー: " + (SpeedrunConfig.timerEnabled ? "§aオン" : "§cオフ")),
            b -> {
                SpeedrunConfig.timerEnabled = !SpeedrunConfig.timerEnabled;
                buildButtons();
            }));

        // Stop conditions (multi-select)
        addButton(new Button(cx - 100, cy + 30, 200, 20,
            new StringTextComponent((timerStopDragon ? "§a[✓] " : "§7[ ] ") + "エンドラが死んだ瞬間に止まる"),
            b -> { timerStopDragon = !timerStopDragon; buildButtons(); }));

        addButton(new Button(cx - 100, cy + 55, 200, 20,
            new StringTextComponent((timerStopPortal ? "§a[✓] " : "§7[ ] ") + "エンドポータルに入った瞬間に止まる"),
            b -> { timerStopPortal = !timerStopPortal; buildButtons(); }));

        addButton(new Button(cx - 100, cy + 80, 200, 20,
            new StringTextComponent((timerStopManual ? "§a[✓] " : "§7[ ] ") + "どちらも選択しない（手動で止める）"),
            b -> { timerStopManual = !timerStopManual; buildButtons(); }));

        // Decide button
        addButton(new Button(cx - 100, cy + 112, 200, 20,
            new StringTextComponent("§e決定"),
            b -> {
                applyTimerConfig();
                currentTab = Tab.TIMER_START;
                buildButtons();
            }));

        addButton(new Button(cx - 100, cy + 138, 200, 20,
            new StringTextComponent("← 戻る"),
            b -> { currentTab = Tab.MAIN; buildButtons(); }));
    }

    private void applyTimerConfig() {
        // Priority: dragon > portal > manual
        if (timerStopDragon && timerStopPortal) {
            // both → use dragon (first to trigger)
            SpeedrunConfig.stopCond = SpeedrunConfig.TimerStopCondition.ENDER_DRAGON_DEATH;
        } else if (timerStopDragon) {
            SpeedrunConfig.stopCond = SpeedrunConfig.TimerStopCondition.ENDER_DRAGON_DEATH;
        } else if (timerStopPortal) {
            SpeedrunConfig.stopCond = SpeedrunConfig.TimerStopCondition.END_PORTAL_ENTER;
        } else {
            SpeedrunConfig.stopCond = SpeedrunConfig.TimerStopCondition.MANUAL;
        }
    }

    // ── TIMER START ──────────────────────────────────────────────

    private void buildTimerStart() {
        int cx = width / 2;
        int cy = height / 2 - 50;

        String condText = "";
        switch (SpeedrunConfig.stopCond) {
            case ENDER_DRAGON_DEATH: condText = "停止: エンドラ撃破"; break;
            case END_PORTAL_ENTER:   condText = "停止: エンドポータル侵入"; break;
            case MANUAL:             condText = "停止: 手動 (設定→タイマー→オフ)"; break;
        }

        // Current timer status
        String statusText = "状態: ";
        switch (TimerHandler.state) {
            case IDLE:              statusText += "待機中"; break;
            case WAITING_FOR_START: statusText += "§eスタート待ち..."; break;
            case RUNNING:           statusText += "§a計測中 " + TimerHandler.getFormattedTime(); break;
            case STOPPED:           statusText += "§c停止 " + TimerHandler.getFormattedTime(); break;
        }

        addButton(new Button(cx - 100, cy, 200, 20,
            new StringTextComponent("スタート"),
            b -> {
                TimerHandler.beginWaiting();
                // Close GUI — time spent in this screen IS counted (timer starts on first action)
                onClose();
            }));

        if (SpeedrunConfig.stopCond == SpeedrunConfig.TimerStopCondition.MANUAL
                && TimerHandler.state == TimerHandler.TimerState.RUNNING) {
            addButton(new Button(cx - 100, cy + 26, 200, 20,
                new StringTextComponent("§cオフ（タイマーを止める）"),
                b -> { TimerHandler.stop(); buildButtons(); }));
        }

        addButton(new Button(cx - 100, cy + 52, 200, 20,
            new StringTextComponent("リセット"),
            b -> { TimerHandler.reset(); buildButtons(); }));

        addButton(new Button(cx - 100, cy + 84, 200, 20,
            new StringTextComponent("← 戻る"),
            b -> { currentTab = Tab.TIMER_CONFIG; buildButtons(); }));
    }

    // ── NETHER CALC ──────────────────────────────────────────────

    private void buildNether() {
        int cx = width / 2;
        int cy = height / 2 - 90;

        // Direction toggle
        addButton(new Button(cx - 100, cy, 200, 20,
            new StringTextComponent("方向: " + (netherDirOwToN ? "§a現世 → ネザー (÷8)" : "§c ネザー → 現世 (×8)")),
            b -> { netherDirOwToN = !netherDirOwToN; recalcNether(); buildButtons(); }));

        // X input field (simulated with +/- buttons for simplicity in 1.16 Forge)
        addButton(new Button(cx - 100, cy + 30, 90, 20,
            new StringTextComponent("X: " + (netherInputX.isEmpty() ? "0" : netherInputX)),
            b -> {
                // Open text input prompt via chat-style approach
                minecraft.keyboardHandler.setSendRepeatsToGui(true);
            }));

        addButton(new Button(cx + 10, cy + 30, 90, 20,
            new StringTextComponent("Z: " + (netherInputZ.isEmpty() ? "0" : netherInputZ)),
            b -> { }));

        // We use simple increment buttons for X and Z
        addButton(new Button(cx - 155, cy + 56, 20, 20, new StringTextComponent("-"), b -> { adjustVal("x", -1); }));
        addButton(new Button(cx - 130, cy + 56, 60, 20, new StringTextComponent("X:" + getIntVal("x")), b -> {}));
        addButton(new Button(cx - 65,  cy + 56, 20, 20, new StringTextComponent("+"), b -> { adjustVal("x",  1); }));

        addButton(new Button(cx - 35,  cy + 56, 20, 20, new StringTextComponent("-"), b -> { adjustVal("z", -1); }));
        addButton(new Button(cx - 10,  cy + 56, 60, 20, new StringTextComponent("Z:" + getIntVal("z")), b -> {}));
        addButton(new Button(cx + 55,  cy + 56, 20, 20, new StringTextComponent("+"), b -> { adjustVal("z",  1); }));

        // ×10 versions
        addButton(new Button(cx - 155, cy + 82, 45, 16, new StringTextComponent("-100"), b -> { adjustVal("x", -100); }));
        addButton(new Button(cx - 105, cy + 82, 45, 16, new StringTextComponent("+100"), b -> { adjustVal("x",  100); }));
        addButton(new Button(cx - 55,  cy + 82, 45, 16, new StringTextComponent("-100"), b -> { adjustVal("z", -100); }));
        addButton(new Button(cx + 0,   cy + 82, 45, 16, new StringTextComponent("+100"), b -> { adjustVal("z",  100); }));

        recalcNether();

        // Result display area (drawn in render)
        addButton(new Button(cx - 100, cy + 110, 200, 20,
            new StringTextComponent("← 戻る"),
            b -> { currentTab = Tab.MAIN; buildButtons(); }));
    }

    private int xVal = 0, zVal = 0;

    private int getIntVal(String axis) {
        return axis.equals("x") ? xVal : zVal;
    }

    private void adjustVal(String axis, int delta) {
        if (axis.equals("x")) xVal += delta; else zVal += delta;
        recalcNether();
        buildButtons();
    }

    private void recalcNether() {
        if (netherDirOwToN) {
            netherResultX = String.valueOf(xVal / 8);
            netherResultZ = String.valueOf(zVal / 8);
        } else {
            netherResultX = String.valueOf(xVal * 8);
            netherResultZ = String.valueOf(zVal * 8);
        }
    }

    // ── STRONGHOLD ───────────────────────────────────────────────

    private void buildStronghold() {
        int cx = width / 2;
        int cy = height / 2 - 80;

        addButton(new Button(cx - 100, cy + 120, 200, 20,
            new StringTextComponent("§cスローを消去"),
            b -> { StrongholdFinder.clearThrows(); buildButtons(); }));

        addButton(new Button(cx - 100, cy + 146, 200, 20,
            new StringTextComponent("← 戻る"),
            b -> { currentTab = Tab.MAIN; buildButtons(); }));
    }

    // ── RENDER ───────────────────────────────────────────────────

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        renderBackground(ms);
        super.render(ms, mouseX, mouseY, partialTicks);

        int cx = width / 2;

        switch (currentTab) {
            case MAIN: {
                drawCenteredString(ms, font, "§e§lSpeedrunTools 設定", cx, height / 2 - 80, 0xFFFFFF);
                drawCenteredString(ms, font, "Oキーで開閉", cx, height / 2 - 65, 0x888888);
                break;
            }
            case TIMER_CONFIG: {
                drawCenteredString(ms, font, "§e§lタイマー設定", cx, height / 2 - 95, 0xFFFFFF);
                drawCenteredString(ms, font, "複数選択可能", cx, height / 2 - 82, 0x888888);
                break;
            }
            case TIMER_START: {
                int cy = height / 2 - 50;
                drawCenteredString(ms, font, "§e§lタイマー", cx, cy - 20, 0xFFFFFF);
                String condText = "";
                switch (SpeedrunConfig.stopCond) {
                    case ENDER_DRAGON_DEATH: condText = "停止条件: エンドラ撃破"; break;
                    case END_PORTAL_ENTER:   condText = "停止条件: エンドポータル侵入"; break;
                    case MANUAL:             condText = "停止条件: 手動"; break;
                }
                drawCenteredString(ms, font, condText, cx, cy - 8, 0xAAAAAA);
                // Current time
                if (TimerHandler.state != TimerHandler.TimerState.IDLE) {
                    drawCenteredString(ms, font, "§e" + TimerHandler.getFormattedTime(), cx, cy + 75, 0xFFFF55);
                }
                break;
            }
            case NETHER: {
                int cy = height / 2 - 90;
                drawCenteredString(ms, font, "§e§l現世とネザーの距離計測", cx, cy - 14, 0xFFFFFF);
                drawCenteredString(ms, font, "結果 → X: " + netherResultX + "  Z: " + netherResultZ,
                    cx, cy + 102, 0x55FF55);
                break;
            }
            case STRONGHOLD: {
                int cy = height / 2 - 80;
                drawCenteredString(ms, font, "§e§lエンド要塞特定", cx, cy - 14, 0xFFFFFF);
                drawCenteredString(ms, font, "使い方: FOVを30に設定 → アイを投げて視点を合わせ → F3+C", cx, cy, 0x888888);
                drawCenteredString(ms, font, "2回以上投げると自動計算されます", cx, cy + 12, 0x888888);

                int ty = cy + 28;
                if (StrongholdFinder.lastError != null) {
                    drawCenteredString(ms, font, "§c" + StrongholdFinder.lastError, cx, ty, 0xFF4444);
                    ty += 14;
                }

                for (int i = 0; i < StrongholdFinder.throws_.size(); i++) {
                    StrongholdFinder.Throw t = StrongholdFinder.throws_.get(i);
                    drawCenteredString(ms, font,
                        String.format("§7投げ%d: (%.0f, %.0f) 角度%.1f°", i+1, t.x, t.z, t.yaw),
                        cx, ty, 0xAAAAAA);
                    ty += 12;
                }

                if (!Double.isNaN(StrongholdFinder.resultX)) {
                    long nx = Math.round(StrongholdFinder.resultX / 8.0);
                    long nz = Math.round(StrongholdFinder.resultZ / 8.0);
                    drawCenteredString(ms, font, "§a§l★ 要塞の場所 ★", cx, ty + 4, 0x55FF55);
                    drawCenteredString(ms, font,
                        String.format("§e現世  X: %.0f  Z: %.0f", StrongholdFinder.resultX, StrongholdFinder.resultZ),
                        cx, ty + 18, 0xFFFF55);
                    drawCenteredString(ms, font,
                        String.format("§6ネザー X: %d  Z: %d  §7(÷8)", nx, nz),
                        cx, ty + 30, 0xFF8800);
                    if (!Double.isNaN(StrongholdFinder.uncertainty)) {
                        drawCenteredString(ms, font,
                            String.format("§7誤差: ±%.0f ブロック", StrongholdFinder.uncertainty),
                            cx, ty + 42, 0xAAAAAA);
                    }
                } else if (StrongholdFinder.throws_.size() == 1) {
                    drawCenteredString(ms, font, "§7もう1回投げてください...", cx, ty + 4, 0x888888);
                } else if (StrongholdFinder.throws_.size() == 0) {
                    drawCenteredString(ms, font, "§7F3+Cを押してデータを取得してください", cx, ty + 4, 0x888888);
                }
                break;
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        // If timer was set to start, it already started waiting — no extra action needed
    }
}
