package com.jojomod.network;

import com.jojomod.JojoMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = JojoMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, ActionPayload::handle);
        registrar.playToClient(SyncPayload.TYPE, SyncPayload.CODEC, SyncPayload::handle);
        registrar.playToClient(TimeStopPayload.TYPE, TimeStopPayload.CODEC, TimeStopPayload::handle);
    }
}
