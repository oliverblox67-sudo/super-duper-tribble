package com.thegods.entity;

import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Il gigantesco tridente d'acqua di Poseidone. */
public class WaterTridentEntity extends SpellEntity {
    public static final int LIFETIME = 60;
    private boolean exploded;

    public WaterTridentEntity(EntityType<? extends WaterTridentEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    public void launch(Vec3 velocity) {
        setDeltaMovement(velocity);
        alignToVelocity(velocity);
        this.yRotO = getYRot();
        this.xRotO = getXRot();
    }

    private void alignToVelocity(Vec3 v) {
        double h = v.horizontalDistance();
        setYRot((float) (Mth.atan2(v.x, v.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(v.y, h) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 vel = getDeltaMovement();
        Vec3 pos = position();
        Vec3 next = pos.add(vel);

        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                Vec3 p = pos.add(vel.scale(-random.nextDouble()));
                level().addParticle(ParticleTypes.SPLASH, p.x + random.nextGaussian() * 0.3, p.y + random.nextGaussian() * 0.3, p.z + random.nextGaussian() * 0.3, 0, 0, 0);
                level().addParticle(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
            }
            level().addParticle(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z, 0, 0, 0);
            return;
        }
        if (exploded) return;

        Entity owner = getOwner();
        HitResult blockHit = level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, pos, end,
                getBoundingBox().expandTowards(vel).inflate(1.5), e -> GodUtil.isValidTarget(owner, e), 0.8f);
        if (entityHit != null) {
            explode(entityHit.getLocation());
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            explode(blockHit.getLocation());
        } else {
            setPos(next.x, next.y, next.z);
            alignToVelocity(vel);
        }
    }

    @Override
    protected void onExpire() {
        if (!exploded) explode(position());
    }

    private void explode(Vec3 at) {
        exploded = true;
        ServerLevel level = (ServerLevel) level();
        Entity owner = getOwner();
        for (LivingEntity e : GodUtil.targetsIn(level, owner, new net.minecraft.world.phys.AABB(at, at).inflate(5))) {
            double d = e.position().distanceTo(at);
            float falloff = (float) Mth.clamp(1.2 - d / 6, 0.4, 1);
            GodUtil.hurt(e, spellDamage(), 18 * falloff);
            Vec3 push = e.position().subtract(at).normalize().scale(1.6 * falloff);
            e.push(push.x, 0.6 * falloff, push.z);
            e.hurtMarked = true;
            e.clearFire();
        }
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 350, 2.5, 1.5, 2.5, 0.6);
        level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y + 0.5, at.z, 120, 2, 1.5, 2, 0.2);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5, at.z, 40, 1.5, 1, 1.5, 0.15);
        level.sendParticles(ParticleTypes.FALLING_WATER, at.x, at.y + 3, at.z, 150, 3, 2, 3, 0);
        GodUtil.particleRing(level, ParticleTypes.SPLASH, at.add(0, 0.3, 0), 1.0, 48, 1.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3.0f, 0.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 1.3f);
        if (level.isRaining() || level.isThundering()) {
            GodUtil.divineLightning(level, owner, at, 12);
        }
        GodManager.shakeArea(level, at, 20, 0.5f, 12);
        discard();
    }
}
