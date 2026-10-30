package com.thegods.entity;

import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * IL TITANO DEGLI ABISSI — un golem d'acqua solida alto quasi 7 blocchi.
 * Combatte per il suo evocatore; il proprietario può salirci sopra (tasto destro)
 * e guidarlo: R = pugno sul bersaglio davanti, o schianto a terra se non c'è nessuno.
 */
public class WaterGolemEntity extends PathfinderMob implements OwnableEntity {
    public static final int LIFETIME = 900;
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(WaterGolemEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public int attackAnim, slamAnim, emerge = 30;
    private int slamCooldown = 80, pendingSlam = -1;

    public WaterGolemEntity(EntityType<? extends WaterGolemEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ATTACK_DAMAGE, 20)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.STEP_HEIGHT, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
    }

    public void setOwner(Player owner) {
        this.entityData.set(OWNER, Optional.of(owner.getUUID()));
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER).orElse(null);
    }

    public boolean isOwnedBy(Entity e) {
        return e != null && e.getUUID().equals(getOwnerUUID());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal());
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new DefendOwnerGoal());
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof Enemy && GodUtil.isValidTarget(getOwner(), e)));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !isOwnedBy(target) && GodUtil.isValidTarget(getOwner(), target) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return isOwnedBy(other) || (other instanceof WaterGolemEntity g && g.getOwnerUUID() != null && g.getOwnerUUID().equals(getOwnerUUID()))
                || super.isAlliedTo(other);
    }

    // ------------------------------------------------------------------ combattimento

    @Override
    public boolean doHurtTarget(Entity target) {
        this.level().broadcastEntityEvent(this, (byte) 4);
        attackAnim = 10;
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            Vec3 dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
            target.push(dir.x * 1.6, 0.7, dir.z * 1.6);
            target.hurtMarked = true;
            if (level() instanceof ServerLevel sl) {
                Vec3 c = target.position().add(0, target.getBbHeight() / 2, 0);
                sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 80, 0.6, 0.6, 0.6, 0.5);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 2f, 0.6f);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.5f, 0.6f);
            }
        }
        return hit;
    }

    /** Schianto a terra: le braccia si alzano, poi un'onda d'urto circolare. */
    public void slam() {
        if (pendingSlam >= 0) return;
        level().broadcastEntityEvent(this, (byte) 60);
        slamAnim = 20;
        pendingSlam = 12;
        slamCooldown = 140;
    }

    private void slamImpact() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        LivingEntity owner = getOwner();
        for (LivingEntity e : GodUtil.targetsIn(sl, owner, new AABB(c, c).inflate(7, 3, 7))) {
            if (e == this || isOwnedBy(e)) continue;
            double d = e.position().distanceTo(c);
            if (d > 7) continue;
            GodUtil.hurtNow(e, damageSources().mobAttack(this), (float) (18 * (1.1 - d / 8)));
            Vec3 push = e.position().subtract(c).normalize();
            e.push(push.x * 1.4, 1.1, push.z * 1.4);
            e.hurtMarked = true;
        }
        for (int r = 1; r <= 4; r++) GodUtil.particleRing(sl, ParticleTypes.SPLASH, c.add(0, 0.2, 0), r * 1.6, 24 + r * 12, 0.9);
        sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, 40, 2.5, 0.3, 2.5, 0.1);
        sl.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 4, c.z, 200, 4, 2, 4, 0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.6f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 3f, 0.4f);
        GodManager.shakeArea(sl, c, 30, 1.0f, 20);
    }

    /** Comando del cavaliere: pugno al bersaglio davanti, altrimenti schianto. */
    public void riderAttack(ServerPlayer rider) {
        if (attackAnim > 0 || pendingSlam >= 0) return;
        Vec3 eye = rider.getEyePosition(), look = rider.getLookAngle();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : GodUtil.targetsIn(level(), rider, getBoundingBox().inflate(7))) {
            if (e == this) continue;
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double d = to.length();
            if (to.normalize().dot(look) < 0.55 || d > 9) continue;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best != null) {
            setTarget(best);
            doHurtTarget(best);
        } else {
            slam();
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) attackAnim = 10;
        else if (id == 60) slamAnim = 20;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (attackAnim > 0) attackAnim--;
        if (slamAnim > 0) slamAnim--;
        if (emerge > 0) emerge--;
        if (level().isClientSide) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.FALLING_WATER, getRandomX(1.2), getY() + random.nextDouble() * getBbHeight(), getRandomZ(1.2), 0, 0, 0);
            }
            if (emerge > 0) {
                for (int i = 0; i < 10; i++) {
                    level().addParticle(ParticleTypes.SPLASH, getRandomX(2), getY() + 0.2, getRandomZ(2), 0, 0.4, 0);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level() || tickCount > LIFETIME) {
            kill();
            return;
        }
        if (isVehicle()) getNavigation().stop();
        if (isInWaterRainOrBubble() && tickCount % 10 == 0) heal(4);
        if (pendingSlam >= 0 && pendingSlam-- == 0) {
            pendingSlam = -1;
            slamImpact();
        }
        if (slamCooldown > 0) slamCooldown--;
        LivingEntity target = getTarget();
        if (!isVehicle() && slamCooldown == 0 && target != null && distanceTo(target) < 7) {
            List<LivingEntity> near = GodUtil.targetsIn(level(), owner, getBoundingBox().inflate(6));
            if (near.size() >= 2 || random.nextInt(3) == 0) slam();
        }
    }

    // ------------------------------------------------------------------ cavalcatura

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isOwnedBy(player) && !player.isSecondaryUseActive() && !isVehicle()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p && isOwnedBy(p) ? p : null;
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        setRot(player.getYRot(), 0);
        this.yRotO = this.yBodyRot = this.yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        float forward = player.zza <= 0 ? player.zza * 0.4f : player.zza;
        return new Vec3(player.xxa * 0.5f, 0, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.85f;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(-1.05, dims.height() * 0.8, -0.1).yRot(-this.yBodyRot * Mth.DEG_TO_RAD);
    }

    // ------------------------------------------------------------------ natura d'acqua

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_FIRE) || isOwnedBy(source.getEntity()) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void tickDeath() {
        if (deathTime == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 3, getZ(), 400, 1.5, 3, 1.5, 0.4);
            sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 4, getZ(), 200, 1.5, 3, 1.5, 0);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.NEUTRAL, 3f, 0.5f);
        }
        ++this.deathTime;
        if (this.deathTime >= 20 && !level().isClientSide()) remove(RemovalReason.KILLED);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_SPLASH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.PLAYER_SPLASH, 0.6f, 0.4f);
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.8f, 0.6f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }

    // ------------------------------------------------------------------ IA

    /** Segue l'evocatore quando non combatte. */
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            return o != null && !isVehicle() && getTarget() == null && distanceToSqr(o) > 64;
        }

        @Override
        public void tick() {
            LivingEntity o = getOwner();
            if (o == null) return;
            getLookControl().setLookAt(o, 10, getMaxHeadXRot());
            if (distanceToSqr(o) > 40 * 40) {
                teleportTo(o.getX() + 2, o.getY(), o.getZ() + 2);
            } else {
                getNavigation().moveTo(o, 1.1);
            }
        }
    }

    /** Attacca chi colpisce l'evocatore e chi l'evocatore colpisce. */
    private class DefendOwnerGoal extends TargetGoal {
        @Nullable
        private LivingEntity candidate;

        DefendOwnerGoal() {
            super(WaterGolemEntity.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity o = getOwner();
            if (o == null) return false;
            LivingEntity attacker = o.getLastHurtByMob();
            if (attacker != null && o.tickCount - o.getLastHurtByMobTimestamp() < 100 && WaterGolemEntity.this.canAttack(attacker)) {
                candidate = attacker;
                return true;
            }
            LivingEntity victim = o.getLastHurtMob();
            if (victim != null && o.tickCount - o.getLastHurtMobTimestamp() < 100 && WaterGolemEntity.this.canAttack(victim)) {
                candidate = victim;
                return true;
            }
            return false;
        }

        @Override
        public void start() {
            mob.setTarget(candidate);
            super.start();
        }
    }
}
