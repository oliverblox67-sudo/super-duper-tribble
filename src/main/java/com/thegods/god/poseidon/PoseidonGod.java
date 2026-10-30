package com.thegods.god.poseidon;

import com.thegods.TheGods;
import com.thegods.god.Ability;
import com.thegods.god.DivineModifier;
import com.thegods.god.God;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * POSEIDONE — Signore dei Sette Mari.
 * Ali d'oceano, tridente di marea, tsunami, maelstrom e l'Ira degli Abissi.
 */
public class PoseidonGod extends God {
    public static final int COLOR = 0x2FB8FF;

    private final List<Ability> abilities = List.of(
            new Ability("poseidon_trident", 30, 12, false, PoseidonPowers::tidalTrident),
            new Ability("poseidon_torrent", 160, 22, false, PoseidonPowers::abyssalTorrent),
            new Ability("poseidon_tide_step", 50, 8, false, PoseidonPowers::tideStep),
            new Ability("poseidon_prison", 240, 20, false, PoseidonPowers::waterPrison),
            new Ability("poseidon_maelstrom", 360, 35, false, PoseidonPowers::maelstrom),
            new Ability("poseidon_tsunami", 300, 40, false, PoseidonPowers::tsunami),
            new Ability("poseidon_wrath", 1800, 100, true, PoseidonPowers::wrathOfTheSeas)
    );

    private final List<DivineModifier> modifiers = List.of(
            mod(Attributes.MAX_HEALTH, "poseidon_health", 20, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.ARMOR, "poseidon_armor", 8, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.ARMOR_TOUGHNESS, "poseidon_toughness", 4, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.ATTACK_DAMAGE, "poseidon_strength", 6, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.KNOCKBACK_RESISTANCE, "poseidon_knockback", 0.6, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.WATER_MOVEMENT_EFFICIENCY, "poseidon_swim", 1.0, AttributeModifier.Operation.ADD_VALUE),
            mod(Attributes.SCALE, "poseidon_scale", 0.25, AttributeModifier.Operation.ADD_VALUE)
    );

    public PoseidonGod() {
        super("poseidon", COLOR);
    }

    private static DivineModifier mod(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                      String name, double amount, AttributeModifier.Operation op) {
        return new DivineModifier(attr, new AttributeModifier(TheGods.id(name), amount, op));
    }

    @Override
    public List<Ability> abilities() {
        return abilities;
    }

    @Override
    public List<DivineModifier> modifiers() {
        return modifiers;
    }

    @Override
    public float energyRegen(ServerPlayer player) {
        // Il mare nutre il suo signore: più veloce in acqua o sotto la pioggia
        return player.isInWaterRainOrBubble() ? 0.65f : 0.3f;
    }

    @Override
    public void onManifest(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2.0f, 0.6f);
        for (int ring = 0; ring < 3; ring++) {
            GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.2 + ring * 0.8, 0), 1.5 + ring, 40, 0.6);
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1.5, c.z, 120, 1.5, 1.5, 1.5, 0.15);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 3, c.z, 150, 3, 1, 3, 0);
        for (LivingEntity e : GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(5))) {
            Vec3 push = e.position().subtract(c).normalize().scale(1.2);
            e.push(push.x, 0.4, push.z);
            e.hurtMarked = true;
        }
    }

    @Override
    public void onDismiss(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CONDUIT_DEACTIVATE, SoundSource.PLAYERS, 1.5f, 1.0f);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 1.5, c.z, 120, 1.2, 1.2, 1.2, 0);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.3, c.z, 80, 1.5, 0.2, 1.5, 0.3);
    }

    @Override
    public void tickManifested(ServerPlayer player, GodData data) {
        if (player.tickCount % 40 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 100, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 100, 0, true, false, true));
        }
        if (player.isOnFire()) player.clearFire();
        player.fallDistance = 0;

        if (data.ultimateTicks() > 0) PoseidonPowers.tickWrath(player, data);
    }

    /** Passivo "Marea Eterna": un colpo mortale ti dissolve nel mare (una volta al minuto). */
    @Override
    public boolean tryEscapeDeath(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        player.setHealth(player.getMaxHealth() * 0.5f);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 2));
        player.clearFire();
        data.setEscapeCooldown(1200);

        level.playSound(null, c.x, c.y, c.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.5f, 1.4f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2.5f, 0.5f);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1, c.z, 400, 2, 2, 2, 0.5);
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1, c.z, 200, 2, 2, 2, 0.3);
        for (int i = 0; i < 4; i++) GodUtil.particleRing(level, ParticleTypes.CLOUD, c.add(0, 0.3 + i, 0), 2 + i, 48, 0.5);

        for (LivingEntity e : GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(8))) {
            Vec3 push = e.position().subtract(c).normalize().scale(2.2);
            e.push(push.x, 0.8, push.z);
            e.hurtMarked = true;
        }
        player.setDeltaMovement(0, 1.2, 0);
        player.hurtMarked = true;

        GodManager.sendTitle(player, "title.thegods.poseidon_escape", "title.thegods.poseidon_escape.sub", COLOR);
        GodManager.shakeArea(level, c, 24, 1.0f, 30);
        return true;
    }
}
