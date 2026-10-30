package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Scuote la telecamera del client. */
public record ShakePayload(float intensity, int duration) implements CustomPacketPayload {
    public static final Type<ShakePayload> TYPE = new Type<>(TheGods.id("shake"));
    public static final StreamCodec<ByteBuf, ShakePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ShakePayload::intensity,
            ByteBufCodecs.VAR_INT, ShakePayload::duration,
            ShakePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
