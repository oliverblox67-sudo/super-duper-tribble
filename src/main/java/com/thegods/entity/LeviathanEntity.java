package com.thegods.entity;

import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IL LEVIATANO — un serpente marino d'acqua lungo ~50 blocchi.
 *
 * Fasi: EMERSIONE (esce dal terreno a spirale) → CACCIA (insegue, morde, afferra e scaglia)
 *       → SOFFIO (si ferma in alto e spara un torrente d'acqua dalle fauci) → TUFFO (si rituffa nel terreno).
 * Il server muove solo la testa; il client ricostruisce il corpo dalla scia della testa.
 */
public class LeviathanEntity extends SpellEntity {
    public static final int LIFETIME = 220;
    public static final int TRAIL = 48;
    public static final int BREATH_START = LIFETIME - 64, DIVE_START = LIFETIME - 20;
    public static final int PHASE_EMERGE = 0, PHASE_HUNT = 1, PHASE_BREATH = 2, PHASE_DIVE = 3;
    private static final double SPEED = 1.15;

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> JAW = SynchedEntityData.defineId(LeviathanEntity.class, EntityDataSerializers.FLOAT);

    /** Solo client: posizioni passate della testa (la più recente per prima) e fisica dei barbigli. */
    public final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    public float jawAnim, jawAnimPrev;
    public final Vec3[][] barbels = new Vec3[2][];
    public final Vec3[][] barbelsPrev = new Vec3[2][];

    private Vec3 heading = new Vec3(0, 1, 0);
    /** Solo per la demo automatica: percorso lento e prevedibile davanti alla telecamera. */
    public boolean showcasePath;
    @Nullable
    private Vec3 pathOrigin;
    @Nullable
    private LivingEntity target;
    @Nullable
    private LivingEntity carried;
    private int carryTicks;
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    public LeviathanEntity(EntityType<? extends LeviathanEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_EMERGE);
        builder.define(JAW, 0.15f);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    public float jawTarget() {
        return this.entityData.get(JAW);
    }

    public void launch(Vec3 forward) {
        Vec3 f = forward.multiply(1, 0, 1).normalize();
        heading = f.scale(0.25).add(0, 1, 0).normalize();
        face(heading);
    }

    private void face(Vec3 h) {
        setYRot((float) (Mth.atan2(-h.x, h.z) * Mth.RAD_TO_DEG));
        setXRot((float) (-Math.asin(Mth.clamp(h.y, -1, 1)) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        Entity owner = getOwner();
        int phase = tickCount < 16 ? PHASE_EMERGE : tickCount < BREATH_START ? PHASE_HUNT : tickCount < DIVE_START ? PHASE_BREATH : PHASE_DIVE;
        if (phase != phase()) {
            this.entityData.set(PHASE, phase);
            if (phase == PHASE_BREATH) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 3f, 0.3f);
        }
        if ((target == null || !target.isAlive() || target.distanceToSqr(this) > 64 * 64) && carried == null) {
            target = GodUtil.targetsIn(sl, owner, getBoundingBox().inflate(44)).stream()
                    .filter(e -> !(e instanceof WaterGolemEntity) && lastHit.getOrDefault(e.getUUID(), -100) < tickCount - 20)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(this))).orElse(null);
        }

        Vec3 desired;
        double speed = SPEED;
        float jaw = 0.2f;
        if (showcasePath && phase != PHASE_BREATH && phase != PHASE_DIVE) {
            if (pathOrigin == null) pathOrigin = position();
            double a = tickCount * 0.035;
            Vec3 goal = pathOrigin.add(Math.sin(a) * 16, 7 + Math.sin(tickCount * 0.09) * 3, 16 - Math.cos(a) * 16);
            heading = heading.lerp(goal.subtract(position()).normalize(), 0.2).normalize();
            Vec3 next = position().add(heading.scale(0.62));
            setPos(next.x, next.y, next.z);
            face(heading);
            this.entityData.set(JAW, (float) (0.5 + 0.5 * Math.sin(tickCount * 0.15)));
            return;
        }
        switch (phase) {
            case PHASE_EMERGE -> {
                double a = tickCount * 0.5;
                desired = heading.add(Math.cos(a) * 0.35, 0, Math.sin(a) * 0.35).normalize();
            }
            case PHASE_BREATH -> {
                speed = 0.18;
                Vec3 aim = target != null ? target.position().add(0, target.getBbHeight() / 2, 0)
                        : owner != null ? owner.position().add(owner.getLookAngle().scale(30)) : position().add(heading.scale(10));
                desired = aim.subtract(position()).normalize();
                jaw = 1f;
                breathe(sl, owner);
            }
            case PHASE_DIVE -> {
                desired = heading.multiply(1, 0, 1).normalize().add(0, -1.6, 0).normalize();
                speed = 1.4;
                jaw = 0.1f;
            }
            default -> {
                if (carried != null) {
                    // porta la preda in alto, poi la scaglia
                    desired = heading.multiply(1, 0, 1).normalize().add(0, 0.6, 0).normalize();
                    jaw = 0.05f;
                } else if (target != null) {
                    Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5 + Mth.sin(tickCount * 0.2f) * 2.0, 0);
                    desired = aim.subtract(position()).normalize();
                    jaw = distanceToSqr(target) < 64 ? 1f : 0.35f;
                } else if (owner != null) {
                    // nessuna preda: si avvolge a spirale attorno all'evocatore
                    double a = tickCount * 0.09;
                    Vec3 orbit = owner.position().add(Math.cos(a) * 9, 6 + Math.sin(tickCount * 0.05) * 3, Math.sin(a) * 9);
                    desired = orbit.subtract(position()).normalize();
                } else {
                    desired = heading;
                }
            }
        }
        double turn = phase == PHASE_HUNT && target != null ? 0.17 : 0.1;
        heading = heading.lerp(desired, turn).normalize();
        Vec3 next = position().add(heading.scale(speed));
        setPos(next.x, next.y, next.z);
        face(heading);
        this.entityData.set(JAW, jaw);

        Vec3 mouth = position().add(heading.scale(2.6));
        if (carried != null) carry(sl, mouth);
        else if (phase == PHASE_HUNT) bite(sl, owner, mouth);
        ram(sl, owner);

        if (tickCount % 6 == 0) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.6f, 0.5f + random.nextFloat() * 0.2f);
        if (tickCount % 40 == 1) sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.PLAYERS, 2.5f, 0.4f);
    }

    /** Il morso: se la preda è tra le fauci la afferra. */
    private void bite(ServerLevel sl, @Nullable Entity owner, Vec3 mouth) {
        if (target == null || target.position().add(0, target.getBbHeight() / 2, 0).distanceTo(mouth) > 3.2) return;
        carried = target;
        carryTicks = 30;
        GodUtil.hurtNow(carried, spellDamage(), 14);
        sl.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.PLAYERS, 2f, 0.5f);
        sl.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2f, 0.7f);
        sl.sendParticles(ParticleTypes.SPLASH, mouth.x, mouth.y, mouth.z, 120, 0.8, 0.8, 0.8, 0.6);
        GodManager.shakeArea(sl, mouth, 20, 0.6f, 10);
    }

    private void carry(ServerLevel sl, Vec3 mouth) {
        LivingEntity c = carried;
        if (c == null) return;
        if (!c.isAlive() || --carryTicks <= 0) {
            if (c.isAlive()) {
                c.setDeltaMovement(heading.scale(2.2).add(0, 0.8, 0));
                c.hurtMarked = true;
                GodUtil.hurtNow(c, spellDamage(), 10);
                sl.playSound(null, c.getX(), c.getY(), c.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2f, 0.5f);
            }
            lastHit.put(c.getUUID(), tickCount);
            carried = null;
            target = null;
            return;
        }
        c.teleportTo(mouth.x, mouth.y - c.getBbHeight() / 2, mouth.z);
        c.setDeltaMovement(Vec3.ZERO);
        c.fallDistance = 0;
        c.hurtMarked = true;
        if (carryTicks % 10 == 0) GodUtil.hurtNow(c, spellDamage(), 2.5f);
    }

    /** Chi viene travolto dal muso o dal corpo vicino alla testa. */
    private void ram(ServerLevel sl, @Nullable Entity owner) {
        for (LivingEntity e : GodUtil.targetsIn(sl, owner, new AABB(position(), position()).inflate(2.6))) {
            if (e instanceof WaterGolemEntity || e == carried) continue;
            Integer last = lastHit.get(e.getUUID());
            if (last != null && tickCount - last < 10) continue;
            lastHit.put(e.getUUID(), tickCount);
            GodUtil.hurtNow(e, spellDamage(), 11);
            e.push(heading.x * 1.3, 0.6 + Math.max(0, heading.y), heading.z * 1.3);
            e.hurtMarked = true;
            sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY() + 1, e.getZ(), 60, 0.6, 0.6, 0.6, 0.5);
        }
    }

    /** Il soffio: un torrente d'acqua dalle fauci. */
    private void breathe(ServerLevel sl, @Nullable Entity owner) {
        if (tickCount < BREATH_START + 14) return; // prima carica (fauci che si spalancano)
        Vec3 mouth = position().add(heading.scale(2.8));
        HitResult hit = sl.clip(new ClipContext(mouth, mouth.add(heading.scale(48)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? mouth.add(heading.scale(48)) : hit.getLocation();
        if (tickCount % 2 == 0) {
            for (LivingEntity e : GodUtil.targetsIn(sl, owner, new AABB(mouth, end).inflate(1.8))) {
                AABB box = e.getBoundingBox().inflate(0.9);
                if (box.clip(mouth, end).isEmpty()) continue;
                GodUtil.hurtNow(e, spellDamage(), 3.5f);
                e.push(heading.x * 0.5, heading.y * 0.5 + 0.1, heading.z * 0.5);
                e.hurtMarked = true;
            }
        }
        sl.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 30, 1, 1, 1, 0.5);
        if (tickCount % 3 == 0) sl.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 3, 0.6, 0.6, 0.6, 0.05);
        if (tickCount % 6 == 0) sl.playSound(null, end.x, end.y, end.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2f, 1.2f);
    }

    // ---------------------------------------------------------------- client

    private void clientTick() {
        trail.addFirst(position());
        while (trail.size() > TRAIL) trail.removeLast();
        jawAnimPrev = jawAnim;
        jawAnim += (jawTarget() - jawAnim) * 0.35f;
        simulateBarbels();
        if (tickCount < 16) {
            for (int i = 0; i < 14; i++) {
                level().addParticle(ParticleTypes.SPLASH, getX() + random.nextGaussian() * 2, getY() - 1, getZ() + random.nextGaussian() * 2, 0, 0.6, 0);
            }
        }
        int n = 0;
        for (Vec3 p : trail) {
            if (n++ % 4 == 0 && random.nextInt(3) == 0) level().addParticle(ParticleTypes.FALLING_WATER, p.x, p.y - 1, p.z, 0, 0, 0);
        }
        if (phase() == PHASE_BREATH && tickCount >= BREATH_START + 14) {
            Vec3 dir = getViewVector(1);
            Vec3 mouth = position().add(dir.scale(2.8));
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.SPLASH, mouth.x, mouth.y, mouth.z, dir.x + random.nextGaussian() * 0.2, dir.y + random.nextGaussian() * 0.2, dir.z + random.nextGaussian() * 0.2);
            }
        }
    }

    /** Due barbigli (come quelli dei draghi orientali) simulati come corde: Verlet + vincoli di lunghezza. */
    private void simulateBarbels() {
        Vec3 dir = getViewVector(1);
        Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 side = dir.cross(up).normalize();
        Vec3 realUp = side.cross(dir).normalize();
        int segs = 10;
        double segLen = 0.32;
        for (int b = 0; b < 2; b++) {
            double s = b == 0 ? 1 : -1;
            Vec3 anchor = position().add(dir.scale(2.9)).add(side.scale(0.42 * s)).add(realUp.scale(-0.05));
            if (barbels[b] == null) {
                barbels[b] = new Vec3[segs];
                barbelsPrev[b] = new Vec3[segs];
                for (int i = 0; i < segs; i++) barbels[b][i] = barbelsPrev[b][i] = anchor.add(dir.scale(-segLen * i));
            }
            Vec3[] p = barbels[b], o = barbelsPrev[b];
            for (int i = 1; i < segs; i++) {
                Vec3 vel = p[i].subtract(o[i]).scale(0.86);
                o[i] = p[i];
                p[i] = p[i].add(vel).add(0, -0.025, 0).add(side.scale(0.02 * s));
            }
            o[0] = p[0];
            p[0] = anchor;
            for (int iter = 0; iter < 3; iter++) {
                for (int i = 1; i < segs; i++) {
                    Vec3 d = p[i].subtract(p[i - 1]);
                    double len = d.length();
                    if (len < 1e-6) continue;
                    p[i] = p[i - 1].add(d.scale(segLen / len));
                }
            }
        }
    }

    @Override
    protected void onExpire() {
        ServerLevel sl = (ServerLevel) level();
        if (carried != null && carried.isAlive()) carried.setDeltaMovement(0, 0.5, 0);
        sl.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 300, 2, 2, 2, 0.5);
        sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 2, getZ(), 200, 3, 2, 3, 0);
        for (int r = 1; r <= 3; r++) GodUtil.particleRing(sl, ParticleTypes.SPLASH, position().add(0, 0.5, 0), r * 2.0, 32, 0.9);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.4f);
        GodManager.shakeArea(sl, position(), 24, 0.6f, 15);
    }
}
