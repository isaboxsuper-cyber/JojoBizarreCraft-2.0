package com.jojomod.event;

import com.jojomod.JojoMod;
import com.jojomod.StandAbilities;
import com.jojomod.network.Sync;
import com.jojomod.util.Cooldowns;
import com.jojomod.util.Scheduler;
import com.jojomod.util.TimeStop;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = JojoMod.MOD_ID)
public class ServerEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scheduler.tick();
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        TimeStop.end(event.getServer());
        Scheduler.clear();
        Cooldowns.clear();
        StandAbilities.clearState();
        Sync.clear();
    }
}
