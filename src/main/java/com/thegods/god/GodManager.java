package com.thegods.god;

import com.thegods.config.GodsConfig;
import com.thegods.network.ShakePayload;
import com.thegods.network.SyncGodDataPayload;
import com.thegods.network.TitleCardPayload;
import com.thegods.registry.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import com.thegods.network.ModNetwork;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Tutta la logica server dello stato divino. */
public final class GodManager {

    public static GodData data(ServerPlayer player) {
        return player.getData(ModAttachments.GOD_DATA);
    }

    // ---------------------------------------------------------------- concessione

    public static void grant(ServerPlayer player, God god) {
        GodData data = data(player);
        if (data.isManifested()) dismiss(player, data, false);
        data.setGod(god);
        syncAll(player);
        player.sendSystemMessage(Component.translatable("message.thegods.granted", god.displayName())
                .withStyle(s -> s.withColor(god.color())));
    }

    public static void revoke(ServerPlayer player) {
        GodData data = data(player);
        if (data.isManifested()) dismiss(player, data, false);
        data.setGod(null);
        refreshForm(player, data);
        syncAll(player);
    }

    // ---------------------------------------------------------------- forma divina

    public static void toggleManifest(ServerPlayer player) {
        GodData data = data(player);
        God god = data.god();
        if (god == null) {
            player.displayClientMessage(Component.translatable("message.thegods.not_a_god")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (data.isManifested()) dismiss(player, data, true);
        else manifest(player, data, god);
    }

    private static void manifest(ServerPlayer player, GodData data, God god) {
        data.setManifested(true);
        refreshForm(player, data);
        god.onManifest(player, data);
        sendTitle(player, "god.thegods." + god.id(), "god.thegods." + god.id() + ".epithet", god.color());
        shake(player, 0.6f, 25);
        syncAll(player);
    }

    private static void dismiss(ServerPlayer player, GodData data, boolean sync) {
        God god = data.god();
        data.setManifested(false);
        data.setUltimateTicks(0);
        if (god != null) god.onDismiss(player, data);
        refreshForm(player, data);
        if (sync) syncAll(player);
    }

    /** Applica o rimuove volo e attributi in base allo stato attuale. Idempotente. */
    public static void refreshForm(ServerPlayer player, GodData data) {
        God god = data.god();
        boolean active = data.isManifested();

        for (God g : Gods.all()) {
            for (DivineModifier m : g.modifiers()) {
                AttributeInstance inst = player.getAttribute(m.attribute());
                if (inst == null) continue;
                boolean want = active && g == god
                        && (GodsConfig.GIANT_FORM.get() || !m.attribute().equals(Attributes.SCALE));
                boolean has = inst.hasModifier(m.modifier().id());
                if (want && !has) inst.addTransientModifier(m.modifier());
                else if (!want && has) inst.removeModifier(m.modifier().id());
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());

        boolean wantFlight = active && GodsConfig.DIVINE_FLIGHT.get();
        var abilities = player.getAbilities();
        if (wantFlight && !abilities.mayfly) {
            abilities.mayfly = true;
            abilities.setFlyingSpeed(0.08f);
            data.setGrantedFlight(true);
            player.onUpdateAbilities();
        } else if (!wantFlight && data.grantedFlight()) {
            data.setGrantedFlight(false);
            abilities.setFlyingSpeed(0.05f);
            if (!player.isCreative() && !player.isSpectator()) {
                abilities.mayfly = false;
                abilities.flying = false;
            }
            player.onUpdateAbilities();
        }
    }

    // ---------------------------------------------------------------- poteri

    public static void useAbility(ServerPlayer player, int index) {
        GodData data = data(player);
        God god = data.god();
        if (god == null || player.isSpectator()) return;
        if (!data.isManifested()) {
            player.displayClientMessage(Component.translatable("message.thegods.manifest_first")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        List<Ability> abilities = god.abilities();
        if (index < 0 || index >= abilities.size()) return;
        Ability ability = abilities.get(index);

        int cd = data.cooldown(ability.id());
        if (cd > 0) {
            player.displayClientMessage(Component.translatable("message.thegods.cooldown",
                    Component.translatable(ability.nameKey()), String.format("%.1f", cd / 20f))
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean free = player.isCreative();
        if (!free && data.energy() < ability.cost()) {
            player.displayClientMessage(Component.translatable("message.thegods.no_energy")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }
        if (!ability.caster().cast(player, data)) return;

        if (!free) data.setEnergy(data.energy() - ability.cost());
        data.setCooldown(ability.id(), (int) Math.round(ability.cooldown() * GodsConfig.COOLDOWN_MULTIPLIER.get()));
        syncAll(player);
    }

    // ---------------------------------------------------------------- tick

    public static void tick(ServerPlayer player) {
        GodData data = data(player);
        God god = data.god();
        if (god == null) {
            if (data.grantedFlight()) refreshForm(player, data);
            return;
        }
        data.tickCooldowns();
        float regen = god.energyRegen(player) * GodsConfig.ENERGY_REGEN_MULTIPLIER.get().floatValue();
        if (!data.isManifested()) regen *= 1.5f;
        data.setEnergy(data.energy() + regen);

        if (data.isManifested()) {
            if (player.tickCount % 20 == 0) refreshForm(player, data);
            god.tickManifested(player, data);
        }
        if (player.tickCount % 5 == 0) syncSelf(player);
    }

    // ---------------------------------------------------------------- morte

    /** @return true se la morte è stata annullata dal passivo del dio. */
    public static boolean onLethalDamage(ServerPlayer player) {
        GodData data = data(player);
        God god = data.god();
        if (god == null || !data.isManifested() || data.escapeCooldown() > 0) return false;
        if (god.tryEscapeDeath(player, data)) {
            syncAll(player);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- rete / effetti

    public static SyncGodDataPayload syncPayload(ServerPlayer player) {
        return new SyncGodDataPayload(player.getId(), data(player).write());
    }

    public static void syncSelf(ServerPlayer player) {
        ModNetwork.send(player, syncPayload(player));
    }

    public static void syncAll(ServerPlayer player) {
        ModNetwork.sendNear(player, syncPayload(player));
    }

    public static void sendTitle(ServerPlayer player, String titleKey, String subtitleKey, int color) {
        ModNetwork.send(player, new TitleCardPayload(titleKey, subtitleKey, color));
    }

    public static void shake(ServerPlayer player, float intensity, int duration) {
        ModNetwork.send(player, new ShakePayload(intensity, duration));
    }

    /** Scuote tutti i giocatori vicini a {@code center}, più forte se più vicini. */
    public static void shakeArea(ServerLevel level, Vec3 center, double radius, float intensity, int duration) {
        for (ServerPlayer p : level.players()) {
            double d = p.position().distanceTo(center);
            if (d > radius) continue;
            float k = (float) (1.0 - d / radius);
            ModNetwork.send(p, new ShakePayload(intensity * (0.35f + 0.65f * k), duration));
        }
    }

    @Nullable
    public static God godOf(ServerPlayer player) {
        return data(player).god();
    }

    private GodManager() {}
}
