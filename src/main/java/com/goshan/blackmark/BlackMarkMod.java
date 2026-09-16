package com.goshan.blackmark;

import com.goshan.blackmark.curse.CurseRegistry;
import com.goshan.blackmark.net.ModNetwork;
import com.goshan.blackmark.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * "It answers to no living master. It simply carries out a last will."
 */
@Mod(BlackMarkMod.MODID)
public class BlackMarkMod {

    public static final String MODID = "blackmark";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlackMarkMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);

        modBus.addListener(this::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CurseRegistry.bootstrap();
            ModNetwork.init();
            LOGGER.info("Black Mark loaded with {} curse(s) registered.", CurseRegistry.all().size());
        });
    }
}
