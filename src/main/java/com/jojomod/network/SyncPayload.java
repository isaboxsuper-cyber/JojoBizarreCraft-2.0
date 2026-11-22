package com.jojomod.network;

import com.jojomod.JojoMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Estado do jogador enviado do servidor para o cliente (HUD, roda de habilidades, tela de status). */
public record SyncPayload(String stand, boolean standActive, int xp, int unlocks, List<Integer> stats,
                          boolean hamon, int hamonStage, int hamonProgress, int energy,
                          boolean vampire, boolean aja, int blood, int sunscreenSec, boolean maskOn)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, "sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPayload> CODEC =
            StreamCodec.of(SyncPayload::write, SyncPayload::read);

    public static final SyncPayload EMPTY = new SyncPayload("", false, 0, 0, List.of(0, 0, 0, 0, 0, 0),
            false, 0, -1, 0, false, false, 20, 0, false);

    private static void write(FriendlyByteBuf b, SyncPayload p) {
        b.writeUtf(p.stand);
        b.writeBoolean(p.standActive);
        b.writeVarInt(p.xp);
        b.writeVarInt(p.unlocks);
        for (int i = 0; i < 6; i++) b.writeVarInt(i < p.stats.size() ? p.stats.get(i) : 0);
        b.writeBoolean(p.hamon);
        b.writeVarInt(p.hamonStage);
        b.writeVarInt(p.hamonProgress + 1);
        b.writeVarInt(p.energy);
        b.writeBoolean(p.vampire);
        b.writeBoolean(p.aja);
        b.writeVarInt(p.blood);
        b.writeVarInt(p.sunscreenSec);
        b.writeBoolean(p.maskOn);
    }

    private static SyncPayload read(FriendlyByteBuf b) {
        String stand = b.readUtf();
        boolean active = b.readBoolean();
        int xp = b.readVarInt();
        int unlocks = b.readVarInt();
        List<Integer> stats = new ArrayList<>();
        for (int i = 0; i < 6; i++) stats.add(b.readVarInt());
        boolean hamon = b.readBoolean();
        int stage = b.readVarInt();
        int progress = b.readVarInt() - 1;
        int energy = b.readVarInt();
        boolean vampire = b.readBoolean();
        boolean aja = b.readBoolean();
        int blood = b.readVarInt();
        int sun = b.readVarInt();
        boolean mask = b.readBoolean();
        return new SyncPayload(stand, active, xp, unlocks, stats, hamon, stage, progress, energy,
                vampire, aja, blood, sun, mask);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientState.sync = payload);
    }
}
