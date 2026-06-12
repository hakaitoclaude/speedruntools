package com.speedruntools.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding zoomKey;
    public static KeyBinding openGuiKey;

    public static void register() {
        zoomKey = new KeyBinding(
            "key.speedruntools.zoom",
            GLFW.GLFW_KEY_C,
            "key.categories.speedruntools"
        );
        openGuiKey = new KeyBinding(
            "key.speedruntools.opengui",
            GLFW.GLFW_KEY_O,
            "key.categories.speedruntools"
        );
        ClientRegistry.registerKeyBinding(zoomKey);
        ClientRegistry.registerKeyBinding(openGuiKey);
    }
}
