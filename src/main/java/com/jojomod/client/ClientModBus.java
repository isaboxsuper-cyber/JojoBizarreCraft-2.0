package com.jojomod.client;

import com.jojomod.JojoMod;
import com.jojomod.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = JojoMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModBus {
    public static final String CATEGORY = "key.categories.jojomod";

    public static final KeyMapping TOGGLE_STAND = new KeyMapping("key.jojomod.toggle_stand",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping USE_ABILITY = new KeyMapping("key.jojomod.use_ability",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping WHEEL = new KeyMapping("key.jojomod.wheel",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final KeyMapping VAMPIRE = new KeyMapping("key.jojomod.vampire",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping STATUS = new KeyMapping("key.jojomod.status",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY);

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_STAND);
        event.register(USE_ABILITY);
        event.register(WHEEL);
        event.register(VAMPIRE);
        event.register(STATUS);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ZEPPELI.get(), ZeppeliRenderer::new);
    }
}
