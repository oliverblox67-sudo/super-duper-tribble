package com.thegods.event;

import com.thegods.TheGods;
import com.thegods.command.GodsCommand;
import com.thegods.god.GodData;
import com.thegods.god.AvatarManager;
import com.thegods.god.GodManager;
import com.thegods.god.LimbManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
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
    public static void onPlayerTickPre(PlayerTickEvent.Pre event) {
        LimbManager.waterWalk(event.getEntity(), event.getEntity().getData(com.thegods.registry.ModAttachments.GOD_DATA));
    }

    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        var src = event.getSource();
        if (src.getEntity() instanceof ServerPlayer sp && src.getDirectEntity() == sp && src.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)) {
            LimbManager.onMeleeHit(sp, event.getEntity());
            if (GodManager.data(sp).avatarGrow() > 0.5f) AvatarManager.onAvatarMelee(sp, event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            GodManager.refreshForm(sp, GodManager.data(sp));
            LimbManager.refresh(sp, GodManager.data(sp));
            AvatarManager.apply(sp, GodManager.data(sp).avatarGrow());
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
            data.setAvatarTicks(0);
            data.setAvatarGrow(0);
            AvatarManager.apply(sp, 0);
            GodManager.refreshForm(sp, data);
            LimbManager.refresh(sp, data);
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
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        GodData data = GodManager.data(sp);
        if (data.avatarGrow() > 0.5f) AvatarManager.onLanding(sp, event.getDistance());
        if (data.isManifested()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Gli dèi manifestati non temono i fulmini, l'annegamento o il fuoco del loro elemento
        if (event.getEntity() instanceof ServerPlayer sp && GodManager.data(sp).isManifested()) {
            var src = event.getSource();
            if (src.is(DamageTypeTags.IS_LIGHTNING) || src.is(DamageTypeTags.IS_DROWNING)) {
                event.setCanceled(true);
            }
            // l'Avatar cresce anche dentro i blocchi: niente soffocamento
            if (GodManager.data(sp).avatarGrow() > 0 && src.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) {
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
