package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Il giocatore usa il potere numero {@code index} del suo dio. */
public record UseAbilityPayload(int index) implements CustomPacketPayload {
    public static final Type<UseAbilityPayload> TYPE = new Type<>(TheGods.id("use_ability"));
    public static final StreamCodec<ByteBuf, UseAbilityPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(UseAbilityPayload::new, UseAbilityPayload::index);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
