package com.thegods.god;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Stato divino di un giocatore: quale dio è, se è manifestato,
 * energia divina e tempi di ricarica.
 */
public class GodData implements INBTSerializable<CompoundTag> {
    private String godId = "";
    private boolean manifested;
    private float energy;
    private final Map<String, Integer> cooldowns = new HashMap<>();
    /** Ricarica del passivo che salva dalla morte. */
    private int escapeCooldown;
    /** Tick rimanenti dell'ultimate attiva. */
    private int ultimateTicks;
    /** True se il volo è stato concesso dalla forma divina (e va tolto alla fine). */
    private boolean grantedFlight;
    /** Avatar (trasformazione nel titano): tick rimanenti e crescita 0..1. */
    private int avatarTicks;
    private float avatarGrow;

    /** Stato dei 4 arti (vedi {@link Limb}); sopravvive anche al cambio di divinità. */
    private final byte[] limbs = new byte[Limb.ALL.length];

    // Solo client: animazione di formazione dei costrutti (0..1 per arto)
    public final float[] clientLimbForm = new float[Limb.ALL.length];
    public final float[] clientLimbFormPrev = new float[Limb.ALL.length];

    // Solo client: crescita dell'avatar interpolata (0..1)
    public float clientAvatar, clientAvatarPrev;

    // Solo client: animazione di comparsa della forma (0..1)
    public float clientForm, clientFormPrev;
    public float clientUltimate, clientUltimatePrev;

    @Nullable
    public God god() {
        return Gods.get(godId);
    }

    public boolean hasGod() {
        return god() != null;
    }

    public boolean is(God god) {
        return god.id().equals(godId);
    }

    public void setGod(@Nullable God god) {
        this.godId = god == null ? "" : god.id();
        this.manifested = false;
        this.cooldowns.clear();
        this.ultimateTicks = 0;
        this.escapeCooldown = 0;
        this.avatarTicks = 0;
        this.avatarGrow = 0;
        this.energy = god == null ? 0 : god.maxEnergy();
    }

    public boolean isManifested() {
        return manifested && hasGod();
    }

    public void setManifested(boolean manifested) {
        this.manifested = manifested;
    }

    public float energy() {
        return energy;
    }

    public void setEnergy(float energy) {
        God g = god();
        this.energy = Mth.clamp(energy, 0, g == null ? 0 : g.maxEnergy());
    }

    public int cooldown(String abilityId) {
        return cooldowns.getOrDefault(abilityId, 0);
    }

    public void setCooldown(String abilityId, int ticks) {
        if (ticks <= 0) cooldowns.remove(abilityId);
        else cooldowns.put(abilityId, ticks);
    }

    public void clearCooldowns() {
        cooldowns.clear();
        escapeCooldown = 0;
    }

    public void tickCooldowns() {
        cooldowns.replaceAll((k, v) -> v - 1);
        cooldowns.values().removeIf(v -> v <= 0);
        if (escapeCooldown > 0) escapeCooldown--;
        if (ultimateTicks > 0) ultimateTicks--;
    }

    public int escapeCooldown() {
        return escapeCooldown;
    }

    public void setEscapeCooldown(int ticks) {
        this.escapeCooldown = ticks;
    }

    public int ultimateTicks() {
        return ultimateTicks;
    }

    public void setUltimateTicks(int ticks) {
        this.ultimateTicks = ticks;
    }

    public boolean grantedFlight() {
        return grantedFlight;
    }

    public void setGrantedFlight(boolean grantedFlight) {
        this.grantedFlight = grantedFlight;
    }

    public int avatarTicks() {
        return avatarTicks;
    }

    public void setAvatarTicks(int ticks) {
        this.avatarTicks = Math.max(0, ticks);
    }

    public boolean isAvatar() {
        return avatarTicks > 0;
    }

    public float avatarGrow() {
        return avatarGrow;
    }

    public void setAvatarGrow(float grow) {
        this.avatarGrow = Mth.clamp(grow, 0, 1);
    }

    public LimbState limb(Limb limb) {
        return LimbState.of(limbs[limb.ordinal()]);
    }

    public void setLimb(Limb limb, LimbState state) {
        limbs[limb.ordinal()] = (byte) state.ordinal();
    }

    public int count(LimbState state, boolean arms) {
        int n = 0;
        for (Limb l : Limb.ALL) if (l.isArm() == arms && limb(l) == state) n++;
        return n;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("god", godId);
        tag.putBoolean("manifested", manifested);
        tag.putFloat("energy", energy);
        tag.putInt("escape", escapeCooldown);
        tag.putInt("ultimate", ultimateTicks);
        tag.putBoolean("grantedFlight", grantedFlight);
        CompoundTag cds = new CompoundTag();
        cooldowns.forEach(cds::putInt);
        tag.put("cooldowns", cds);
        tag.putByteArray("limbs", limbs.clone());
        tag.putInt("avatar", avatarTicks);
        tag.putFloat("avatarGrow", avatarGrow);
        return tag;
    }

    public void read(CompoundTag tag) {
        godId = tag.getString("god");
        manifested = tag.getBoolean("manifested");
        energy = tag.getFloat("energy");
        escapeCooldown = tag.getInt("escape");
        ultimateTicks = tag.getInt("ultimate");
        grantedFlight = tag.getBoolean("grantedFlight");
        cooldowns.clear();
        CompoundTag cds = tag.getCompound("cooldowns");
        for (String key : cds.getAllKeys()) cooldowns.put(key, cds.getInt(key));
        avatarTicks = tag.getInt("avatar");
        avatarGrow = tag.getFloat("avatarGrow");
        byte[] l = tag.getByteArray("limbs");
        for (int i = 0; i < limbs.length; i++) limbs[i] = i < l.length ? l[i] : 0;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return write();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        read(tag);
    }
}
