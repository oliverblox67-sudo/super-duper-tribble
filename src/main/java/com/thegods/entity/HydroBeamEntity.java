package com.thegods.entity;

import com.thegods.util.GodUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Getto Abissale: raggio d'acqua che segue lo sguardo del lanciatore. */
public class HydroBeamEntity extends SpellEntity {
    public static final int LIFETIME = 45;
    public static final double RANGE = 64;

    public HydroBeamEntity(EntityType<? extends HydroBeamEntity> type, Level level) {
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
        if (!(owner instanceof LivingEntity caster) || !caster.isAlive()) {
            if (!level().isClientSide) discard();
            return;
        }
        Vec3 eye = caster.getEyePosition();
        setPos(eye.x, eye.y, eye.z);
        if (level().isClientSide) return;

        ServerLevel level = (ServerLevel) level();
        Vec3 dir = caster.getLookAngle();
        Vec3 end = GodUtil.raycastBlocks(level, caster, eye, dir, RANGE);

        if (tickCount % 2 == 0) {
            for (LivingEntity e : GodUtil.targetsIn(level, caster, new AABB(eye, end).inflate(1.5))) {
                AABB box = e.getBoundingBox().inflate(0.6);
                if (box.clip(eye, end).isEmpty() && !box.contains(eye)) continue;
                GodUtil.hurtNow(e, spellDamage(), 3.0f);
                e.push(dir.x * 0.35, dir.y * 0.35 + 0.05, dir.z * 0.35);
                e.hurtMarked = true;
                e.clearFire();
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 20, 0.6, 0.6, 0.6, 0.4);
        level.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 4, 0.4, 0.4, 0.4, 0.1);
        if (tickCount % 3 == 0) level.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 2, 0.3, 0.3, 0.3, 0.05);
        if (tickCount % 8 == 0) {
            level.playSound(null, end.x, end.y, end.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 1.6f);
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1.5f, 0.6f);
        }
        BlockPos firePos = BlockPos.containing(end);
        for (BlockPos p : BlockPos.betweenClosed(firePos.offset(-1, -1, -1), firePos.offset(1, 1, 1))) {
            if (level.getBlockState(p).is(BlockTags.FIRE)) level.removeBlock(p, false);
        }
    }
}
