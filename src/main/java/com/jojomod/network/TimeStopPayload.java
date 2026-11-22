package com.jojomod.network;

import com.jojomod.JojoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Avisa os clientes que o tempo parou (para inverter as cores). */
public record TimeStopPayload(boolean frozen) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TimeStopPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JojoMod.MOD_ID, "time_stop"));

    public static final StreamCodec<ByteBuf, TimeStopPayload> CODEC =
            ByteBufCodecs.BOOL.map(TimeStopPayload::new, TimeStopPayload::frozen);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TimeStopPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientState.timeStopped = payload.frozen());
    }
}
