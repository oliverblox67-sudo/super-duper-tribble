package com.thegods.util;

import com.thegods.config.GodsConfig;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Funzioni condivise dai poteri. */
public final class GodUtil {

    /** Può il potere di {@code caster} colpire {@code target}? */
    public static boolean isValidTarget(@Nullable Entity caster, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == caster) return false;
        if (target instanceof ArmorStand) return false;
        if (target instanceof Player p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (caster instanceof Player cp && (!GodsConfig.PVP_DAMAGE.get() || !cp.canHarmPlayer(p))) return false;
        }
        if (caster != null) {
            if (target.isAlliedTo(caster)) return false;
            if (target instanceof OwnableEntity own && own.getOwner() == caster) return false;
        }
        return true;
    }

    public static List<LivingEntity> targetsIn(Level level, @Nullable Entity caster, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box, e -> isValidTarget(caster, e));
    }

    /** Danno che ignora i frame di invulnerabilità (per poteri continui). */
    public static boolean hurtNow(LivingEntity target, DamageSource source, float amount) {
        target.invulnerableTime = 0;
        return target.hurt(source, GodsConfig.damage(amount));
    }

    public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
        return target.hurt(source, GodsConfig.damage(amount));
    }

    /** Punto colpito guardando da {@code from} verso {@code dir} (solo blocchi solidi). */
    public static Vec3 raycastBlocks(Level level, Entity viewer, Vec3 from, Vec3 dir, double range) {
        Vec3 to = from.add(dir.scale(range));
        HitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** Particelle lungo un segmento. */
    public static void particleLine(ServerLevel level, ParticleOptions p, Vec3 a, Vec3 b, double step, int count, double spread) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1e-4) return;
        Vec3 n = d.scale(1 / len);
        for (double t = 0; t <= len; t += step) {
            Vec3 at = a.add(n.scale(t));
            level.sendParticles(p, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
        }
    }

    /** Anello di particelle orizzontale. */
    public static void particleRing(ServerLevel level, ParticleOptions p, Vec3 c, double radius, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            double x = Math.cos(a), z = Math.sin(a);
            // count=0: la "spread" diventa la direzione di velocità
            level.sendParticles(p, c.x + x * radius, c.y, c.z + z * radius, 0, x, 0.1, z, speed);
        }
    }

    /**
     * Fulmine divino: effetto visivo vanilla (niente incendi) + danno solo ai bersagli validi.
     */
    public static void divineLightning(ServerLevel level, @Nullable Entity owner, Vec3 at, float damage) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        if (damage <= 0) return;
        DamageSource src = owner instanceof Player p ? level.damageSources().playerAttack(p)
                : owner instanceof LivingEntity le ? level.damageSources().mobAttack(le)
                : level.damageSources().lightningBolt();
        for (LivingEntity e : targetsIn(level, owner, new AABB(at, at).inflate(3, 4, 3))) {
            hurtNow(e, src, damage);
            e.igniteForSeconds(3);
        }
    }

    public static Vec3 horizontalDir(float yRotDeg) {
        double r = Math.toRadians(yRotDeg);
        return new Vec3(-Math.sin(r), 0, Math.cos(r));
    }

    private GodUtil() {}
}
