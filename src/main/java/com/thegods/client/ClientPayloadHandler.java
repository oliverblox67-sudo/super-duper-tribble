package com.thegods.client;

import com.thegods.client.gui.TitleCardOverlay;
import com.thegods.network.ShakePayload;
import com.thegods.network.SyncGodDataPayload;
import com.thegods.network.TitleCardPayload;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Gestione lato client dei pacchetti (caricata solo sul client). */
public final class ClientPayloadHandler {
    public static void handleSync(SyncGodDataPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Entity e = level.getEntity(payload.entityId());
        if (e instanceof Player p) p.getData(ModAttachments.GOD_DATA).read(payload.data());
    }

    public static void handleTitle(TitleCardPayload payload) {
        TitleCardOverlay.show(payload.titleKey(), payload.subtitleKey(), payload.color());
    }

    public static void handleShake(ShakePayload payload) {
        CameraShake.add(payload.intensity(), payload.duration());
    }

    private ClientPayloadHandler() {}
}
