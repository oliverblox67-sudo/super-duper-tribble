package com.thegods.entity;

import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cupola delle Maree: una sfera d'acqua solida attorno all'evocatore.
 * Riflette i proiettili verso chi li ha lanciati e respinge i nemici.
 */
public class TidalDomeEntity extends SpellEntity {
    public static final int LIFETIME = 200;
    private final Set<UUID> reflected = new HashSet<>();

    public TidalDomeEntity(EntityType<? extends TidalDomeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }
        setPos(owner.getX(), owner.getY(), owner.getZ());
        if (level().isClientSide) return;
        ServerLevel sl = (ServerLevel) level();
        float r = getSize();
        Vec3 c = position().add(0, 1, 0);

        for (Projectile p : sl.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(r + 3))) {
            if (p.getOwner() == owner || reflected.contains(p.getUUID())) continue;
            Vec3 rel = p.position().subtract(c);
            if (rel.length() > r + 1.2) continue;
            Vec3 v = p.getDeltaMovement();
            if (v.dot(rel) >= 0) continue; // si sta allontanando
            reflected.add(p.getUUID());
            Entity shooter = p.getOwner();
            p.setDeltaMovement(v.scale(-1.1));
            p.setOwner(owner);
            p.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY(), p.getZ(), 25, 0.2, 0.2, 0.2, 0.3);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1f, 1.6f);
            if (shooter != null) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, shooter.getEyePosition());
        }

        for (LivingEntity e : GodUtil.targetsIn(sl, owner, getBoundingBox().inflate(r + 1))) {
            if (e instanceof WaterGolemEntity) continue;
            Vec3 rel = e.position().add(0, e.getBbHeight() / 2, 0).subtract(c);
            double d = rel.length();
            if (d > r + 0.8) continue;
            Vec3 out = rel.multiply(1, 0, 1).normalize();
            e.setDeltaMovement(out.x * 1.0, 0.35, out.z * 1.0);
            e.hurtMarked = true;
            if (tickCount % 10 == 0) GodUtil.hurtNow(e, spellDamage(), 3);
        }

        if (owner instanceof LivingEntity le && tickCount % 20 == 0) {
            le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, true, false, true));
            le.clearFire();
        }
        if (tickCount % 30 == 0) sl.playSound(null, c.x, c.y, c.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.5f, 0.7f);
    }
}
