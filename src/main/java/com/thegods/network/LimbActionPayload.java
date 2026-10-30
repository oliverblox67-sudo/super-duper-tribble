package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Dalla Forgia del Corpo: azione (recidi/forgia/rigenera) su un arto. */
public record LimbActionPayload(int limb, int action) implements CustomPacketPayload {
    public static final Type<LimbActionPayload> TYPE = new Type<>(TheGods.id("limb_action"));
    public static final StreamCodec<ByteBuf, LimbActionPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LimbActionPayload::limb,
            ByteBufCodecs.VAR_INT, LimbActionPayload::action,
            LimbActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
