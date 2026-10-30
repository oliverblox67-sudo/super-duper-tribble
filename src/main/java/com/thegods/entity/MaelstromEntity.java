package com.thegods.entity;

import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Vortice degli Abissi: risucchia, fa girare, poi esplode in un geyser. size = raggio. */
public class MaelstromEntity extends SpellEntity {
    public static final int LIFETIME = 160;
    public static final float COLUMN_HEIGHT = 16f;

    public MaelstromEntity(EntityType<? extends MaelstromEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    /** 0..1 intensità: cresce all'inizio. */
    public float intensity(float partialTick) {
        return Mth.clamp((tickCount + partialTick) / 20f, 0, 1);
    }

    @Override
    public void tick() {
        super.tick();
        float radius = getSize();
        if (level().isClientSide) {
            for (int i = 0; i < 6; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * radius;
                double x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                double ta = a + Math.PI / 2;
                level().addParticle(ParticleTypes.SPLASH, x, getY() + 0.2, z, Math.cos(ta) * 0.4, 0.15, Math.sin(ta) * 0.4);
            }
            for (int i = 0; i < 3; i++) {
                double hgt = random.nextDouble() * COLUMN_HEIGHT * intensity(0);
                double a = random.nextDouble() * Math.PI * 2;
                double r = 1.0 + hgt * 0.12;
                level().addParticle(ParticleTypes.FALLING_WATER, getX() + Math.cos(a) * r, getY() + hgt, getZ() + Math.sin(a) * r, 0, 0, 0);
                level().addParticle(ParticleTypes.CLOUD, getX() + Math.cos(a) * r, getY() + hgt, getZ() + Math.sin(a) * r, -Math.sin(a) * 0.2, 0.05, Math.cos(a) * 0.2);
            }
            return;
        }

        double reach = radius * 1.3;
        AABB box = new AABB(getX() - reach, getY() - 2, getZ() - reach, getX() + reach, getY() + COLUMN_HEIGHT, getZ() + reach);
        Vec3 center = position();
        for (LivingEntity e : GodUtil.targetsIn(level(), getOwner(), box)) {
            Vec3 rel = e.position().subtract(center);
            double dist = Math.sqrt(rel.x * rel.x + rel.z * rel.z);
            if (dist > reach) continue;
            Vec3 inward = dist > 0.01 ? new Vec3(-rel.x / dist, 0, -rel.z / dist) : Vec3.ZERO;
            Vec3 tangent = new Vec3(-inward.z, 0, inward.x);
            double pull = 0.12 + 0.25 * (1 - dist / reach);
            double lift = dist < 3 ? 0.32 : 0.06;
            Vec3 v = e.getDeltaMovement().scale(0.4).add(tangent.scale(0.45)).add(inward.scale(pull));
            double vy = e.getY() > getY() + COLUMN_HEIGHT * 0.7 ? 0.02 : lift;
            e.setDeltaMovement(v.x, vy, v.z);
            e.hurtMarked = true;
            e.fallDistance = 0;
            if (tickCount % 10 == 0 && dist < radius) GodUtil.hurtNow(e, spellDamage(), 3);
        }
        if (tickCount % 20 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 2.5f, 0.5f);
        }
    }

    /** Il geyser finale. */
    @Override
    protected void onExpire() {
        ServerLevel level = (ServerLevel) level();
        float radius = getSize();
        AABB box = new AABB(getX() - radius, getY() - 2, getZ() - radius, getX() + radius, getY() + COLUMN_HEIGHT, getZ() + radius);
        for (LivingEntity e : GodUtil.targetsIn(level, getOwner(), box)) {
            GodUtil.hurtNow(e, spellDamage(), 10);
            e.setDeltaMovement(e.getDeltaMovement().x * 0.2, 2.0, e.getDeltaMovement().z * 0.2);
            e.hurtMarked = true;
        }
        for (int y = 0; y < 18; y++) {
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + y, getZ(), 40, 1.2, 0.5, 1.2, 0.4);
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + y, getZ(), 4, 0.8, 0.5, 0.8, 0.05);
        }
        level.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 18, getZ(), 300, 5, 1, 5, 0);
        GodUtil.particleRing(level, ParticleTypes.SPLASH, position().add(0, 0.3, 0), 2, 64, 1.5);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 4.0f, 0.4f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.7f);
        GodManager.shakeArea(level, position(), 32, 1.0f, 25);
    }
}
