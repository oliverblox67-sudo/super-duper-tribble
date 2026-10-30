package com.thegods.entity;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Una lancia d'acqua solida. Prima resta sospesa in formazione alle spalle
 * dell'evocatore puntando il bersaglio, poi parte.
 */
public class WaterSpearEntity extends SpellEntity {
    public static final int LIFETIME = 90;
    private Vec3 formation = Vec3.ZERO;
    private int delay = 10;
    private boolean launched;
    @Nullable
    private LivingEntity target;

    public WaterSpearEntity(EntityType<? extends WaterSpearEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    public void setup(Vec3 formationOffset, int delay, @Nullable LivingEntity target) {
        this.formation = formationOffset;
        this.delay = delay;
        this.target = target;
    }

    public boolean isLaunched() {
        return launched;
    }

    private void aim(Vec3 dir) {
        setYRot((float) (Mth.atan2(dir.x, dir.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(dir.y, dir.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.DRIPPING_WATER, getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        Entity owner = getOwner();
        if (owner == null) {
            discard();
            return;
        }
        if (!launched) {
            Vec3 pos = owner.position().add(formation.yRot(-owner.getYRot() * Mth.DEG_TO_RAD));
            setPos(pos.x, pos.y, pos.z);
            Vec3 aimAt = target != null && target.isAlive()
                    ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                    : owner.getEyePosition().add(owner.getLookAngle().scale(30));
            Vec3 dir = aimAt.subtract(pos).normalize();
            aim(dir);
            if (tickCount >= delay) {
                launched = true;
                setDeltaMovement(dir.scale(2.8));
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0f, 1.3f + random.nextFloat() * 0.3f);
            }
            return;
        }

        Vec3 vel = getDeltaMovement();
        Vec3 pos = position(), next = pos.add(vel);
        HitResult blockHit = level().clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(level(), this, pos, end,
                getBoundingBox().expandTowards(vel).inflate(1), e -> GodUtil.isValidTarget(owner, e) && !(e instanceof WaterGolemEntity), 0.5f);
        if (entityHit != null) {
            impact(entityHit.getLocation(), (LivingEntity) entityHit.getEntity());
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            impact(blockHit.getLocation(), null);
        } else {
            setPos(next.x, next.y, next.z);
            aim(vel);
        }
    }

    private void impact(Vec3 at, @Nullable LivingEntity hit) {
        ServerLevel sl = (ServerLevel) level();
        Entity owner = getOwner();
        if (hit != null) {
            GodUtil.hurtNow(hit, spellDamage(), 9);
            Vec3 v = getDeltaMovement().normalize();
            hit.push(v.x * 0.6, 0.2, v.z * 0.6);
            hit.hurtMarked = true;
        }
        for (LivingEntity e : GodUtil.targetsIn(sl, owner, new AABB(at, at).inflate(1.8))) {
            if (e != hit && !(e instanceof WaterGolemEntity)) GodUtil.hurt(e, spellDamage(), 4);
        }
        sl.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 50, 0.4, 0.4, 0.4, 0.4);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 12, 0.3, 0.3, 0.3, 0.1);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.9f, 1.4f);
        discard();
    }
}
