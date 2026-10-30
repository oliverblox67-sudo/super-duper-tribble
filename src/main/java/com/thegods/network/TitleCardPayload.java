package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Mostra un titolo cinematografico (chiavi di traduzione). */
public record TitleCardPayload(String titleKey, String subtitleKey, int color) implements CustomPacketPayload {
    public static final Type<TitleCardPayload> TYPE = new Type<>(TheGods.id("title_card"));
    public static final StreamCodec<ByteBuf, TitleCardPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TitleCardPayload::titleKey,
            ByteBufCodecs.STRING_UTF8, TitleCardPayload::subtitleKey,
            ByteBufCodecs.INT, TitleCardPayload::color,
            TitleCardPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
