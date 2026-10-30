package com.thegods.network;

import com.thegods.TheGods;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sincronizza lo stato divino di un giocatore verso i client. */
public record SyncGodDataPayload(int entityId, CompoundTag data) implements CustomPacketPayload {
    public static final Type<SyncGodDataPayload> TYPE = new Type<>(TheGods.id("sync_god_data"));
    public static final StreamCodec<ByteBuf, SyncGodDataPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncGodDataPayload::entityId,
            ByteBufCodecs.COMPOUND_TAG, SyncGodDataPayload::data,
            SyncGodDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
