package com.jojomod.util;

import com.jojomod.network.TimeStopPayload;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.network.PacketDistributor;

/** Za Warudo: usa o mesmo mecanismo do comando /tick freeze. */
public final class TimeStop {
    private static UUID owner = null;

    private TimeStop() {}

    public static boolean active() {
        return owner != null;
    }

    public static void start(ServerPlayer p, int ticks) {
        MinecraftServer server = p.server;
        owner = p.getUUID();
        server.tickRateManager().setFrozen(true);
        PacketDistributor.sendToAllPlayers(new TimeStopPayload(true));

        // Outros jogadores tambem ficam travados (o /tick freeze nao congela jogadores)
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            if (o != p) {
                o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false));
                o.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 4, false, false));
                o.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 4, false, false));
                Combat.chat(o, "time_stopped");
            }
        }

        ServerLevel lvl = (ServerLevel) p.level();
        lvl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        lvl.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 60, 3, 1.5, 3, 0.05);

        Scheduler.later(ticks, () -> end(server));
    }

    public static void end(MinecraftServer server) {
        if (owner == null) return;
        owner = null;
        server.tickRateManager().setFrozen(false);
        PacketDistributor.sendToAllPlayers(new TimeStopPayload(false));
    }
}
