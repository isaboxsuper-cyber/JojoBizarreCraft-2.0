package com.jojomod.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Cooldowns {
    private static final Map<String, Long> MAP = new HashMap<>();

    private Cooldowns() {}

    /** Retorna true (e inicia o cooldown) se a habilidade estiver pronta. */
    public static boolean ready(ServerPlayer p, String key, int ticks) {
        if (p.isCreative()) return true;
        long now = Scheduler.now();
        String k = p.getUUID() + ":" + key;
        Long until = MAP.get(k);
        if (until != null && until > now) {
            long sec = (until - now + 19) / 20;
            p.displayClientMessage(Component.translatable("msg.jojomod.cooldown", sec), true);
            return false;
        }
        MAP.put(k, now + ticks);
        return true;
    }

    public static void clear() {
        MAP.clear();
    }
}
