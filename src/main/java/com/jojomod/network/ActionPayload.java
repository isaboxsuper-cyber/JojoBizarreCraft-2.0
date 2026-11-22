package com.jojomod.network;

import com.jojomod.AbilityHandler;
import com.jojomod.JojoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Cliente -> servidor. kind: 0 = stand on/off, 1 = usar habilidade (arg = id), 2 = forma vampiro on/off, 3 = comprar no/arvore (arg = id) */
public record ActionPayload(int kind, String arg) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, "action"));

    public static final StreamCodec<ByteBuf, ActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActionPayload::kind,
            ByteBufCodecs.STRING_UTF8, ActionPayload::arg,
            ActionPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                AbilityHandler.handle(sp, payload.kind(), payload.arg());
            }
        });
    }
}
