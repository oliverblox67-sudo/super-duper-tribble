package com.thegods.network;

import com.thegods.client.ClientPayloadHandler;
import com.thegods.god.GodManager;
import com.thegods.god.Limb;
import com.thegods.god.LimbManager;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");

        // Client -> Server
        r.playToServer(ManifestPayload.TYPE, ManifestPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) GodManager.toggleManifest(sp);
        }));
        r.playToServer(UseAbilityPayload.TYPE, UseAbilityPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) GodManager.useAbility(sp, p.index());
        }));

        r.playToServer(LimbActionPayload.TYPE, LimbActionPayload.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (p.limb() < 0 || p.limb() >= Limb.ALL.length || p.action() < 0 || p.action() >= LimbManager.Action.values().length) return;
            LimbManager.perform(sp, Limb.ALL[p.limb()], LimbManager.Action.values()[p.action()]);
        }));

        // Server -> Client
        r.playToClient(SyncGodDataPayload.TYPE, SyncGodDataPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientPayloadHandler.handleSync(p)));
        r.playToClient(TitleCardPayload.TYPE, TitleCardPayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientPayloadHandler.handleTitle(p)));
        r.playToClient(ShakePayload.TYPE, ShakePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> ClientPayloadHandler.handleShake(p)));
    }

    /** Invia solo se il client ha la mod (evita errori con client vanilla o giocatori finti). */
    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** Invia al giocatore e a tutti quelli abbastanza vicini da vederlo. */
    public static void sendNear(ServerPlayer player, CustomPacketPayload payload) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || other.distanceToSqr(player) < 192 * 192) send(other, payload);
        }
    }

    private ModNetwork() {}
}
