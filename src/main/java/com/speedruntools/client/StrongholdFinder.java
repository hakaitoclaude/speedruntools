package com.speedruntools.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Stronghold Finder using the NinjabrainBot-style approach:
 *
 *  1. Player sets FOV to 30 in vanilla settings.
 *  2. Player throws an Eye of Ender and looks directly at it.
 *  3. Player presses F3+C (Minecraft copies coords+angles to clipboard).
 *  4. This mod reads the clipboard and parses:
 *       /execute in minecraft:overworld run tp @s X Y Z Yaw Pitch
 *  5. Two or more throws are triangulated to find the stronghold.
 */
public class StrongholdFinder {

    public static class Throw {
        public double x, z, yaw; // yaw in degrees (Minecraft convention)
        public Throw(double x, double z, double yaw) {
            this.x = x; this.z = z; this.yaw = yaw;
        }
        @Override
        public String toString() {
            return String.format("(%.1f, %.1f) → %.1f°", x, z, yaw);
        }
    }

    public static List<Throw> throws_ = new ArrayList<>();
    public static double resultX = Double.NaN;
    public static double resultZ = Double.NaN;
    public static double uncertainty = Double.NaN;
    public static String lastError = null;

    // ── Keyboard listener ────────────────────────────────────────

    private boolean f3Held = false;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long win = mc.getWindow().getWindow();
        boolean f3Now = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
        boolean cNow  = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_C)  == GLFW.GLFW_PRESS;

        if (f3Now && cNow && !f3Held) {
            f3Held = true;
            // Give Minecraft a tick to actually write clipboard, then read it
            mc.execute(() -> {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException ignored) {}
                String clip = GLFW.glfwGetClipboardString(win);
                if (clip != null) parseClipboard(clip);
            });
        }
        if (!f3Now) f3Held = false;
    }

    // ── Clipboard parser ─────────────────────────────────────────
    // Expected format from MC 1.16:
    // /execute in minecraft:overworld run tp @s X Y Z YAW PITCH

    public static void parseClipboard(String clip) {
        lastError = null;
        try {
            String[] parts = clip.trim().split("\\s+");
            // indices: 0=/execute 1=in 2=dim 3=run 4=tp 5=@s 6=X 7=Y 8=Z 9=YAW 10=PITCH
            if (parts.length < 11) {
                lastError = "クリップボードのフォーマットが不正です";
                return;
            }
            if (!parts[2].contains("overworld")) {
                lastError = "現世(Overworld)でのみ使用できます";
                return;
            }
            double x   = Double.parseDouble(parts[6]);
            double z   = Double.parseDouble(parts[8]);
            double yaw = Double.parseDouble(parts[9]);

            throws_.add(new Throw(x, z, yaw));

            if (throws_.size() >= 2) {
                triangulate();
            }
        } catch (Exception e) {
            lastError = "パース失敗: " + e.getMessage();
        }
    }

    // ── Triangulation ────────────────────────────────────────────

    private static void triangulate() {
        if (throws_.size() < 2) return;

        // Use all throws and find weighted least-squares intersection
        // For 2 throws: direct line intersection
        // For 3+: average of all pairwise intersections weighted by sin(angle between lines)

        double sumX = 0, sumZ = 0, totalWeight = 0;
        double minDist = Double.MAX_VALUE, maxDist = 0;

        List<double[]> intersections = new ArrayList<>();

        for (int i = 0; i < throws_.size(); i++) {
            for (int j = i + 1; j < throws_.size(); j++) {
                double[] pt = lineIntersect(throws_.get(i), throws_.get(j));
                if (pt == null) continue;
                double angleDiff = Math.abs(angleDifference(throws_.get(i).yaw, throws_.get(j).yaw));
                double weight = Math.abs(Math.sin(Math.toRadians(angleDiff)));
                sumX += pt[0] * weight;
                sumZ += pt[1] * weight;
                totalWeight += weight;
                intersections.add(new double[]{pt[0], pt[1], weight});
            }
        }

        if (totalWeight == 0) {
            lastError = "直線が平行です。別の方向から投げ直してください。";
            return;
        }

        resultX = sumX / totalWeight;
        resultZ = sumZ / totalWeight;

        // Uncertainty = weighted std dev of intersections
        if (intersections.size() > 1) {
            double varX = 0, varZ = 0;
            for (double[] pt : intersections) {
                varX += pt[2] * Math.pow(pt[0] - resultX, 2);
                varZ += pt[2] * Math.pow(pt[1] - resultZ, 2);
            }
            varX /= totalWeight;
            varZ /= totalWeight;
            uncertainty = Math.sqrt(varX + varZ);
        } else {
            uncertainty = 0;
        }
    }

    private static double[] lineIntersect(Throw a, Throw b) {
        // Minecraft yaw: 0=south(+Z), 90=west(-X), -90=east(+X), 180/-180=north(-Z)
        double dxA = -Math.sin(Math.toRadians(a.yaw));
        double dzA =  Math.cos(Math.toRadians(a.yaw));
        double dxB = -Math.sin(Math.toRadians(b.yaw));
        double dzB =  Math.cos(Math.toRadians(b.yaw));

        // a + t*dA = b + s*dB  →  solve for t
        double det = dxA * (-dzB) - dzA * (-dxB);
        if (Math.abs(det) < 1e-9) return null;
        double dx = b.x - a.x, dz = b.z - a.z;
        double t = (dx * (-dzB) - dz * (-dxB)) / det;
        if (t < 0) return null; // behind — skip
        return new double[]{a.x + t * dxA, a.z + t * dzA};
    }

    private static double angleDifference(double a, double b) {
        double diff = ((a - b) % 360 + 360) % 360;
        if (diff > 180) diff -= 360;
        return diff;
    }

    public static void clearThrows() {
        throws_.clear();
        resultX = Double.NaN;
        resultZ = Double.NaN;
        uncertainty = Double.NaN;
        lastError = null;
    }
}
