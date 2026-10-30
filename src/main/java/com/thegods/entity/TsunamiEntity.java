package com.thegods.entity;

import com.thegods.util.GodUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Un muro d'oceano che avanza nella direzione {@code yRot}.
 * size = larghezza, maxHeight = altezza della cresta.
 */
public class TsunamiEntity extends SpellEntity {
    public static final int LIFETIME = 70;
    public static final double SPEED = 0.75;
    private static final EntityDataAccessor<Float> MAX_HEIGHT = SynchedEntityData.defineId(TsunamiEntity.class, EntityDataSerializers.FLOAT);

    private final Set<UUID> alreadyHit = new HashSet<>();

    public TsunamiEntity(EntityType<? extends TsunamiEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MAX_HEIGHT, 8f);
    }

    public void setMaxHeight(float h) {
        this.entityData.set(MAX_HEIGHT, h);
    }

    public float getMaxHeight() {
        return this.entityData.get(MAX_HEIGHT);
    }

    @Override
    public int lifetime() {
        return LIFETIME;
    }

    /** Altezza attuale: si alza, travolge, poi si infrange. */
    public float currentHeight(float partialTick) {
        float age = tickCount + partialTick;
        float rise = Mth.clamp(age / 14f, 0, 1);
        rise = rise * rise * (3 - 2 * rise);
        float fall = Mth.clamp((LIFETIME - age) / 16f, 0, 1);
        return getMaxHeight() * rise * fall;
    }

    public Vec3 direction() {
        return GodUtil.horizontalDir(getYRot());
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 dir = direction();
        Vec3 right = new Vec3(dir.z, 0, -dir.x);
        float width = getSize();
        float h = currentHeight(0);

        if (level().isClientSide) {
            int n = Math.max(4, (int) (width * 0.5f));
            for (int i = 0; i < n; i++) {
                double s = (random.nextDouble() - 0.5) * width;
                Vec3 crest = position().add(right.scale(s)).add(dir.scale(0.8)).add(0, h * (0.85 + random.nextDouble() * 0.2), 0);
                level().addParticle(ParticleTypes.SPLASH, crest.x, crest.y, crest.z, dir.x * 0.3, 0.1, dir.z * 0.3);
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.CLOUD, crest.x, crest.y, crest.z, dir.x * 0.25, 0.02, dir.z * 0.25);
                Vec3 base = position().add(right.scale(s)).add(dir.scale(1.2));
                level().addParticle(ParticleTypes.FALLING_WATER, crest.x, crest.y - 0.5, crest.z, 0, 0, 0);
                level().addParticle(ParticleTypes.SPLASH, base.x, base.y + 0.2, base.z, dir.x * 0.5, 0.2, dir.z * 0.5);
            }
            return;
        }

        // Avanza seguendo il terreno
        Vec3 next = position().add(dir.scale(SPEED));
        int surface = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(next.x), Mth.floor(next.z));
        double y = getY();
        if (Math.abs(surface - y) < 10) y = Mth.lerp(0.35, y, surface);
        setPos(next.x, y, next.z);

        // Travolge
        double r = width / 2 + 3;
        AABB box = new AABB(getX() - r, getY() - 1, getZ() - r, getX() + r, getY() + h + 1.5, getZ() + r);
        for (LivingEntity e : GodUtil.targetsIn(level(), getOwner(), box)) {
            Vec3 rel = e.position().subtract(position());
            double along = rel.dot(dir);
            double side = rel.dot(right);
            if (Math.abs(side) > width / 2 + 0.5 || along < -3.5 || along > 2.5 || rel.y > h + 1) continue;
            if (alreadyHit.add(e.getUUID())) GodUtil.hurt(e, spellDamage(), 14);
            Vec3 v = e.getDeltaMovement().scale(0.3).add(dir.scale(1.15)).add(0, 0.42, 0);
            e.setDeltaMovement(v);
            e.hurtMarked = true;
            e.clearFire();
        }

        // Spegne gli incendi
        if (tickCount % 2 == 0) {
            for (double s = -width / 2; s <= width / 2; s += 1) {
                Vec3 p = position().add(right.scale(s)).add(dir.scale(1.5));
                BlockPos base = BlockPos.containing(p);
                for (int dy = -1; dy <= Math.max(2, (int) h); dy++) {
                    BlockPos bp = base.above(dy);
                    if (level().getBlockState(bp).is(BlockTags.FIRE)) level().removeBlock(bp, false);
                }
            }
        }
        if (tickCount % 14 == 1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3.0f, 0.4f + random.nextFloat() * 0.2f);
        }
    }
}
