package com.jojomod.network;

import com.jojomod.ModAttachments;
import com.jojomod.ModItems;
import com.jojomod.data.JojoData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.network.PacketDistributor;

/** Envia o estado do jogador ao cliente somente quando mudou. */
public final class Sync {
    private static final Map<UUID, SyncPayload> LAST = new HashMap<>();

    private Sync() {}

    public static SyncPayload snapshot(ServerPlayer p) {
        JojoData d = p.getData(ModAttachments.JOJO);
        return new SyncPayload(d.stand, d.standActive, d.standXp, d.standUnlocks, List.copyOf(d.stats),
                d.hamon, d.hamonStage, d.hamonProgress, d.hamonEnergy, d.vampire, d.aja, d.blood,
                (d.sunscreen + 19) / 20, p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STONE_MASK.get()));
    }

    public static void tick(ServerPlayer p) {
        SyncPayload s = snapshot(p);
        if (!s.equals(LAST.get(p.getUUID()))) {
            LAST.put(p.getUUID(), s);
            PacketDistributor.sendToPlayer(p, s);
        }
    }

    public static void forget(ServerPlayer p) {
        LAST.remove(p.getUUID());
    }

    public static void clear() {
        LAST.clear();
    }
}
