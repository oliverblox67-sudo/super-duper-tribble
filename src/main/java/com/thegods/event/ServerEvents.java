package com.thegods.event;

import com.thegods.TheGods;
import com.thegods.command.GodsCommand;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.thegods.network.ModNetwork;

@EventBusSubscriber(modid = TheGods.MODID)
public final class ServerEvents {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        GodsCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) GodManager.tick(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GodManager.refreshForm(sp, GodManager.data(sp));
            GodManager.syncAll(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GodData data = GodManager.data(sp);
            data.setManifested(false);
            data.setUltimateTicks(0);
            data.setGrantedFlight(false);
            GodManager.refreshForm(sp, data);
            GodManager.syncAll(sp);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GodManager.refreshForm(sp, GodManager.data(sp));
            GodManager.syncAll(sp);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            ModNetwork.send(viewer, GodManager.syncPayload(target));
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && GodManager.data(sp).isManifested()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Gli dèi manifestati non temono i fulmini, l'annegamento o il fuoco del loro elemento
        if (event.getEntity() instanceof ServerPlayer sp && GodManager.data(sp).isManifested()) {
            var src = event.getSource();
            if (src.is(DamageTypeTags.IS_LIGHTNING) || src.is(DamageTypeTags.IS_DROWNING)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && GodManager.onLethalDamage(sp)) {
            event.setCanceled(true);
        }
    }

    private ServerEvents() {}
}
