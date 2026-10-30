package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Il giocatore chiede di assumere/lasciare la forma divina. */
public record ManifestPayload() implements CustomPacketPayload {
    public static final ManifestPayload INSTANCE = new ManifestPayload();
    public static final Type<ManifestPayload> TYPE = new Type<>(TheGods.id("manifest"));
    public static final StreamCodec<ByteBuf, ManifestPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
