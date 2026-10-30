package com.thegods.god;

import com.thegods.TheGods;
import com.thegods.util.GodUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * La Forgia del Corpo: recidere gli arti e sostituirli con costrutti di acqua solida.
 *
 * Braccio d'acqua: +3 danno, +1 portata, scavo più veloce, i pugni scatenano onde d'urto.
 * Gamba d'acqua: +15% velocità, salto più alto, gradini più alti, cadute attutite.
 * Entrambe le gambe d'acqua: cammini sull'acqua.
 * Arti recisi: penalità (meno danno / meno velocità).
 */
public final class LimbManager {
    public static final float FORGE_COST = 25f;
    public static final float RESTORE_COST = 40f;

    public enum Action { SEVER, FORGE, RESTORE }

    private static final Map<Limb, Map<LimbState, List<DivineModifier>>> MODIFIERS = new EnumMap<>(Limb.class);

    static {
        for (Limb limb : Limb.ALL) {
            Map<LimbState, List<DivineModifier>> byState = new EnumMap<>(LimbState.class);
            List<DivineModifier> construct = new ArrayList<>();
            List<DivineModifier> severed = new ArrayList<>();
            String p = "limb_" + limb.id() + "_";
            if (limb.isArm()) {
                construct.add(mod(Attributes.ATTACK_DAMAGE, p + "construct_damage", 3, AttributeModifier.Operation.ADD_VALUE));
                construct.add(mod(Attributes.ENTITY_INTERACTION_RANGE, p + "construct_reach", 1, AttributeModifier.Operation.ADD_VALUE));
                construct.add(mod(Attributes.BLOCK_BREAK_SPEED, p + "construct_mining", 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                severed.add(mod(Attributes.ATTACK_DAMAGE, p + "severed_damage", -0.25, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                severed.add(mod(Attributes.BLOCK_BREAK_SPEED, p + "severed_mining", -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                construct.add(mod(Attributes.MOVEMENT_SPEED, p + "construct_speed", 0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                construct.add(mod(Attributes.JUMP_STRENGTH, p + "construct_jump", 0.06, AttributeModifier.Operation.ADD_VALUE));
                construct.add(mod(Attributes.STEP_HEIGHT, p + "construct_step", 0.25, AttributeModifier.Operation.ADD_VALUE));
                construct.add(mod(Attributes.SAFE_FALL_DISTANCE, p + "construct_fall", 6, AttributeModifier.Operation.ADD_VALUE));
                severed.add(mod(Attributes.MOVEMENT_SPEED, p + "severed_speed", -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                severed.add(mod(Attributes.JUMP_STRENGTH, p + "severed_jump", -0.12, AttributeModifier.Operation.ADD_VALUE));
            }
            byState.put(LimbState.FLESH, List.of());
            byState.put(LimbState.SEVERED, List.copyOf(severed));
            byState.put(LimbState.CONSTRUCT, List.copyOf(construct));
            MODIFIERS.put(limb, byState);
        }
    }

    private static DivineModifier mod(Holder<Attribute> attr, String name, double amount, AttributeModifier.Operation op) {
        return new DivineModifier(attr, new AttributeModifier(TheGods.id(name), amount, op));
    }

    public static List<DivineModifier> modifiers(Limb limb, LimbState state) {
        return MODIFIERS.get(limb).get(state);
    }

    // ------------------------------------------------------------------ azioni dalla GUI

    public static void perform(ServerPlayer player, Limb limb, Action action) {
        GodData data = GodManager.data(player);
        God god = data.god();
        if (god == null) {
            player.displayClientMessage(Component.translatable("message.thegods.not_a_god").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        LimbState state = data.limb(limb);
        boolean free = player.isCreative();
        switch (action) {
            case SEVER -> {
                if (state == LimbState.SEVERED) return;
                data.setLimb(limb, LimbState.SEVERED);
                if (!free) player.hurt(player.damageSources().magic(), 3f);
                severEffects(player, limb, state == LimbState.CONSTRUCT);
            }
            case FORGE -> {
                if (state == LimbState.CONSTRUCT) return;
                if (state == LimbState.FLESH) {
                    // recide e forgia in un solo gesto
                    if (!free) player.hurt(player.damageSources().magic(), 3f);
                    severEffects(player, limb, false);
                }
                if (!free && data.energy() < FORGE_COST) {
                    player.displayClientMessage(Component.translatable("message.thegods.no_energy").withStyle(ChatFormatting.AQUA), true);
                    data.setLimb(limb, LimbState.SEVERED);
                    break;
                }
                if (!free) data.setEnergy(data.energy() - FORGE_COST);
                data.setLimb(limb, LimbState.CONSTRUCT);
                forgeEffects(player, limb);
            }
            case RESTORE -> {
                if (state == LimbState.FLESH) return;
                if (!free && data.energy() < RESTORE_COST) {
                    player.displayClientMessage(Component.translatable("message.thegods.no_energy").withStyle(ChatFormatting.AQUA), true);
                    return;
                }
                if (!free) data.setEnergy(data.energy() - RESTORE_COST);
                data.setLimb(limb, LimbState.FLESH);
                ServerLevel level = player.serverLevel();
                Vec3 at = limbPos(player, limb);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y, at.z, 12, 0.2, 0.3, 0.2, 0);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 0.8f);
            }
        }
        refresh(player, data);
        GodManager.syncAll(player);
    }

    private static void severEffects(ServerPlayer player, Limb limb, boolean wasConstruct) {
        ServerLevel level = player.serverLevel();
        Vec3 at = limbPos(player, limb);
        if (!wasConstruct) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.55f, 0.02f, 0.05f), 1.4f), at.x, at.y, at.z, 30, 0.15, 0.25, 0.15, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.6f);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0f, 0.5f);
        }
        level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 50, 0.2, 0.3, 0.2, 0.3);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 1.3f);
    }

    private static void forgeEffects(ServerPlayer player, Limb limb) {
        ServerLevel level = player.serverLevel();
        Vec3 at = limbPos(player, limb);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 6, r = 1.2 - i * 0.04;
            level.sendParticles(ParticleTypes.SPLASH, at.x + Math.cos(a) * r, at.y - 0.4 + i * 0.04, at.z + Math.sin(a) * r, 2, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.BUBBLE_POP, at.x, at.y, at.z, 40, 0.3, 0.4, 0.3, 0.08);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    /** Posizione approssimativa dell'arto nel mondo (per particelle). */
    public static Vec3 limbPos(Player player, Limb limb) {
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        double side = (limb == Limb.RIGHT_ARM || limb == Limb.RIGHT_LEG ? 1 : -1) * (limb.isArm() ? 0.4 : 0.15);
        double h = player.getBbHeight() * (limb.isArm() ? 0.62 : 0.25);
        return player.position().add(right.scale(side)).add(0, h, 0);
    }

    // ------------------------------------------------------------------ effetti continui

    /** Applica/rimuove gli attributi degli arti. Idempotente. */
    public static void refresh(ServerPlayer player, GodData data) {
        for (Limb limb : Limb.ALL) {
            LimbState current = data.limb(limb);
            for (LimbState state : LimbState.values()) {
                for (DivineModifier m : modifiers(limb, state)) {
                    AttributeInstance inst = player.getAttribute(m.attribute());
                    if (inst == null) continue;
                    boolean want = state == current;
                    boolean has = inst.hasModifier(m.modifier().id());
                    if (want && !has) inst.addTransientModifier(m.modifier());
                    else if (!want && has) inst.removeModifier(m.modifier().id());
                }
            }
        }
    }

    /**
     * Camminare sull'acqua con due gambe d'acqua. Gira su client e server:
     * il movimento del giocatore è calcolato dal client.
     */
    public static void waterWalk(Player player, GodData data) {
        if (data.count(LimbState.CONSTRUCT, false) < 2) return;
        if (player.isShiftKeyDown() || player.getAbilities().flying || player.isPassenger()) return;
        Level level = player.level();
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos pos = BlockPos.containing(player.getX(), player.getY() + dy, player.getZ());
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.is(FluidTags.WATER) || !level.getFluidState(pos.above()).isEmpty()) continue;
            double surface = pos.getY() + fluid.getHeight(level, pos);
            if (player.getY() < surface - 0.6 || player.getY() > surface + 0.05) continue;
            Vec3 v = player.getDeltaMovement();
            if (v.y <= 0.05) {
                player.setPos(player.getX(), surface, player.getZ());
                player.setDeltaMovement(v.x, 0, v.z);
                player.setOnGround(true);
                player.resetFallDistance();
                if (level.isClientSide && player.tickCount % 3 == 0 && v.horizontalDistanceSqr() > 0.001) {
                    level.addParticle(ParticleTypes.SPLASH, player.getX(), surface + 0.05, player.getZ(), 0, 0.1, 0);
                }
            }
            return;
        }
    }

    /** Pugno con braccia d'acqua: onda d'urto extra. */
    public static void onMeleeHit(ServerPlayer player, LivingEntity target) {
        GodData data = GodManager.data(player);
        int arms = data.count(LimbState.CONSTRUCT, true);
        if (arms == 0) return;
        Vec3 dir = target.position().subtract(player.position()).multiply(1, 0, 1).normalize();
        target.push(dir.x * 0.5 * arms, 0.15 * arms, dir.z * 0.5 * arms);
        target.hurtMarked = true;
        target.clearFire();
        ServerLevel level = player.serverLevel();
        Vec3 c = target.position().add(0, target.getBbHeight() * 0.6, 0);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 30 * arms, 0.4, 0.4, 0.4, 0.4);
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.8f, 1.5f);
        if (arms == 2) {
            for (LivingEntity e : GodUtil.targetsIn(level, player, target.getBoundingBox().inflate(2.5))) {
                if (e == target) continue;
                GodUtil.hurt(e, player.damageSources().playerAttack(player), 4);
                e.push(dir.x * 0.6, 0.25, dir.z * 0.6);
                e.hurtMarked = true;
            }
        }
    }

    private LimbManager() {}
}
