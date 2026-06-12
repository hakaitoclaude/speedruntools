package com.speedruntools;

import com.speedruntools.client.KeyBindings;
import com.speedruntools.client.SpeedrunHud;
import com.speedruntools.config.SpeedrunConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("speedruntools")
public class SpeedrunToolsMod {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "speedruntools";

    public SpeedrunToolsMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        KeyBindings.register();
        MinecraftForge.EVENT_BUS.register(new SpeedrunHud());
        MinecraftForge.EVENT_BUS.register(new com.speedruntools.client.ZoomHandler());
        MinecraftForge.EVENT_BUS.register(new com.speedruntools.client.FullbrightHandler());
        MinecraftForge.EVENT_BUS.register(new com.speedruntools.client.TimerHandler());
        MinecraftForge.EVENT_BUS.register(new com.speedruntools.client.StrongholdFinder());
    }
}
