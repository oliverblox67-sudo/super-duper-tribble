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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IL LEVIATANO — un serpente marino d'acqua solida lungo ~30 blocchi.
 * Emerge dal terreno, caccia i nemici serpeggiando nell'aria e li travolge.
 * Il server muove solo la testa; il client ricostruisce il corpo dalla scia.
 */
public class LeviathanEntity extends SpellEntity {
    public static final int LIFETIME = 120;
    public static final int TRAIL = 34;
    private static final double SPEED = 1.15;

    /** Solo client: posizioni passate della testa, la più recente per prima. */
    public final ArrayDeque<Vec3> trail = new ArrayDeque<>();

    private Vec3 heading = new Vec3(0, 1, 0);
    @Nullable
    private LivingEntity target;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public LeviathanEntity(EntityType<? extends LeviathanEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    public void launch(Vec3 forward) {
        Vec3 f = forward.multiply(1, 0, 1).normalize();
        heading = f.scale(0.4).add(0, 1, 0).normalize();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            trail.addFirst(position());
            while (trail.size() > TRAIL) trail.removeLast();
            if (tickCount < 12) {
                for (int i = 0; i < 12; i++) {
                    level().addParticle(ParticleTypes.SPLASH, getX() + random.nextGaussian() * 1.5, getY() - 1, getZ() + random.nextGaussian() * 1.5, 0, 0.5, 0);
                }
            }
            int n = 0;
            for (Vec3 p : trail) {
                if (n++ % 4 == 0 && random.nextInt(3) == 0) level().addParticle(ParticleTypes.FALLING_WATER, p.x, p.y - 0.8, p.z, 0, 0, 0);
            }
            return;
        }

        Entity owner = getOwner();
        if (target == null || !target.isAlive() || target.distanceToSqr(this) > 60 * 60) {
            target = GodUtil.targetsIn(level(), owner, getBoundingBox().inflate(40)).stream()
                    .filter(e -> !(e instanceof WaterGolemEntity))
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
        }

        Vec3 desired;
        if (tickCount < 14) {
            desired = heading;
        } else if (target != null) {
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5 + Mth.sin(tickCount * 0.2f) * 2.5, 0);
            desired = aim.subtract(position()).normalize();
        } else {
            Vec3 flat = heading.multiply(1, 0, 1);
            if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
            desired = flat.normalize().add(0, Mth.sin(tickCount * 0.15f) * 0.6, 0).normalize();
        }
        double turn = target != null ? 0.16 : 0.08;
        heading = heading.lerp(desired, turn).normalize();
        Vec3 next = position().add(heading.scale(SPEED));
        setPos(next.x, next.y, next.z);

        ServerLevel sl = (ServerLevel) level();
        for (LivingEntity e : GodUtil.targetsIn(sl, owner, new AABB(position(), position()).inflate(2.4))) {
            if (e instanceof WaterGolemEntity) continue;
            Integer last = lastHit.get(e.getUUID());
            if (last != null && tickCount - last < 10) continue;
            lastHit.put(e.getUUID(), tickCount);
            GodUtil.hurtNow(e, spellDamage(), 11);
            e.push(heading.x * 1.3, 0.6 + Math.max(0, heading.y), heading.z * 1.3);
            e.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 1, e.getZ(), 60, 0.6, 0.6, 0.6, 0.5);
        }
        if (tickCount % 6 == 0) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.6f, 0.5f + random.nextFloat() * 0.2f);
        }
        if (tickCount % 30 == 1) {
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.PLAYERS, 2.5f, 0.4f);
        }
    }

    @Override
    protected void onExpire() {
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 300, 2, 2, 2, 0.5);
        sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 2, getZ(), 200, 3, 2, 3, 0);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.4f);
        GodManager.shakeArea(sl, position(), 24, 0.6f, 15);
    }
}
