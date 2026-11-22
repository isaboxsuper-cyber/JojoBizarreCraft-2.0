package com.jojomod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(JojoMod.MOD_ID)
public class JojoMod {
    public static final String MOD_ID = "jojomod";

    public JojoMod(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
