package com.thegods.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Base per tutti gli incantesimi visibili (onde, vortici, raggi...).
 * Non vengono salvati: esistono solo finché il potere è attivo.
 */
public abstract class SpellEntity extends Entity {
    private static final EntityDataAccessor<Integer> OWNER_ID = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.FLOAT);

    protected SpellEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, -1);
        builder.define(SIZE, 1f);
    }

    public void setOwner(Entity owner) {
        this.entityData.set(OWNER_ID, owner.getId());
    }

    @Nullable
    public Entity getOwner() {
        int id = this.entityData.get(OWNER_ID);
        return id < 0 ? null : level().getEntity(id);
    }

    public float getSize() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float size) {
        this.entityData.set(SIZE, size);
    }

    /** Durata in tick. */
    public abstract int lifetime();

    /** 0..1: quanto della vita è trascorsa. */
    public float progress(float partialTick) {
        return Math.min(1f, (tickCount + partialTick) / lifetime());
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= lifetime()) {
            onExpire();
            discard();
        }
    }

    protected void onExpire() {}

    protected DamageSource spellDamage() {
        Entity owner = getOwner();
        if (owner instanceof Player p) return damageSources().playerAttack(p);
        if (owner instanceof LivingEntity le) return damageSources().mobAttack(le);
        return damageSources().magic();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
