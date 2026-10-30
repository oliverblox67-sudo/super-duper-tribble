package com.thegods.entity;

import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Una sfera d'acqua che imprigiona il bersaglio e lo fa annegare. */
public class WaterPrisonEntity extends SpellEntity {
    public static final int LIFETIME = 120;
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(WaterPrisonEntity.class, EntityDataSerializers.INT);
    @Nullable
    private Vec3 anchor;

    public WaterPrisonEntity(EntityType<? extends WaterPrisonEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_ID, -1);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    public void trap(LivingEntity target) {
        this.entityData.set(TARGET_ID, target.getId());
        this.anchor = target.position().add(0, 1.2, 0);
        float radius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.75f + 0.6f;
        setSize(radius);
        Vec3 c = centerOf(target);
        setPos(c.x, c.y, c.z);
    }

    @Nullable
    public LivingEntity getTarget() {
        Entity e = level().getEntity(this.entityData.get(TARGET_ID));
        return e instanceof LivingEntity le ? le : null;
    }

    public static Vec3 centerOf(Entity e) {
        return e.position().add(0, e.getBbHeight() / 2, 0);
    }

    @Override
    public void tick() {
        super.tick();
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            if (!level().isClientSide && tickCount > 2) {
                burst();
                discard();
            }
            return;
        }
        Vec3 c = centerOf(target);
        setPos(c.x, c.y, c.z);

        if (level().isClientSide) {
            float r = getSize();
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.BUBBLE_POP,
                        c.x + random.nextGaussian() * r * 0.4, c.y + random.nextGaussian() * r * 0.4, c.z + random.nextGaussian() * r * 0.4,
                        0, 0.05, 0);
            }
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.DRIPPING_WATER, c.x + random.nextGaussian() * r * 0.5, c.y - r * 0.9, c.z + random.nextGaussian() * r * 0.5, 0, 0, 0);
            return;
        }

        if (anchor == null) anchor = target.position().add(0, 1.2, 0);
        if (target.position().distanceToSqr(anchor) > 0.04) target.teleportTo(anchor.x, anchor.y, anchor.z);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        target.fallDistance = 0;
        target.clearFire();
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
        target.setAirSupply(Math.max(-19, target.getAirSupply() - 15));
        if (tickCount % 20 == 10) {
            GodUtil.hurtNow(target, spellDamage(), 3.5f);
            level().playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    @Override
    protected void onExpire() {
        burst();
    }

    private void burst() {
        if (!(level() instanceof ServerLevel level)) return;
        Vec3 c = position();
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 120, getSize() * 0.6, getSize() * 0.6, getSize() * 0.6, 0.4);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y, c.z, 60, getSize() * 0.5, getSize() * 0.5, getSize() * 0.5, 0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.5f, 1.2f);
    }
}
