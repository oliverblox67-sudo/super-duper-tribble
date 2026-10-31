package com.thegods.god;

import com.thegods.TheGods;
import com.thegods.util.GodUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * L'AVATAR: il dio si avvolge in un titano gigantesco del suo elemento e lo guida
 * dall'interno (il giocatore resta visibile nel petto del titano).
 * La crescita è graduale: scala, vita e forza salgono insieme all'animazione.
 */
public final class AvatarManager {
    public static final int DURATION = 1200;
    private static final float GROW_PER_TICK = 1 / 24f;
    private static final float SHRINK_PER_TICK = 1 / 16f;

    private record Scaled(Holder<Attribute> attribute, String id, double amount, AttributeModifier.Operation op) {}

    private static final List<Scaled> MODIFIERS = List.of(
            new Scaled(Attributes.SCALE, "avatar_scale", 2.3, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.MAX_HEALTH, "avatar_health", 40, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.ARMOR, "avatar_armor", 12, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.ATTACK_DAMAGE, "avatar_damage", 16, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.ATTACK_KNOCKBACK, "avatar_knockback", 2.5, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.KNOCKBACK_RESISTANCE, "avatar_steady", 1.0, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.MOVEMENT_SPEED, "avatar_stride", 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
            new Scaled(Attributes.JUMP_STRENGTH, "avatar_jump", 0.35, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.STEP_HEIGHT, "avatar_step", 1.5, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.SAFE_FALL_DISTANCE, "avatar_fall", 40, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.ENTITY_INTERACTION_RANGE, "avatar_reach", 4, AttributeModifier.Operation.ADD_VALUE),
            new Scaled(Attributes.BLOCK_INTERACTION_RANGE, "avatar_block_reach", 3, AttributeModifier.Operation.ADD_VALUE)
    );

    /** Giocatori in volo per lo "Schianto Abissale" (atterrando esplodono). */
    private static final Set<UUID> PENDING_SLAM = new HashSet<>();

    public static void start(ServerPlayer player, GodData data, int titleColor) {
        data.setAvatarTicks(DURATION);
        GodManager.refreshForm(player, data);
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 2f, 0.35f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.3f);
        for (int i = 0; i < 6; i++) GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.2 + i * 0.4, 0), 4.5 - i * 0.6, 48, -0.6);
        level.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 2, c.z, 200, 2, 2, 2, 0.2);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(player) < 80 * 80) GodManager.sendTitle(p, "title.thegods.avatar", "title.thegods.avatar.sub", titleColor);
        }
        GodManager.shakeArea(level, c, 48, 1.2f, 40);
        GodManager.syncAll(player);
    }

    public static void end(ServerPlayer player, GodData data) {
        if (!data.isAvatar()) return;
        data.setAvatarTicks(0);
        PENDING_SLAM.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.4f);
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 3, c.z, 500, 2, 3, 2, 0.6);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 5, c.z, 300, 2.5, 2, 2.5, 0);
        GodManager.syncAll(player);
    }

    public static void tick(ServerPlayer player, GodData data) {
        float before = data.avatarGrow();
        if (data.isAvatar()) {
            if (!data.isManifested()) {
                end(player, data);
            } else {
                data.setAvatarTicks(data.avatarTicks() - 1);
                data.setAvatarGrow(before + GROW_PER_TICK);
                if (!data.isAvatar()) end(player, data);
                else footsteps(player);
            }
        } else if (before > 0) {
            data.setAvatarGrow(before - SHRINK_PER_TICK);
        }
        float grow = data.avatarGrow();
        if (grow != before || (grow > 0 && player.tickCount % 20 == 0)) {
            apply(player, grow);
            if (grow != before) {
                GodManager.syncAll(player);
                if (grow == 0 || before == 0) GodManager.refreshForm(player, data);
            }
        }
    }

    /** Applica gli attributi proporzionati alla crescita (idempotente). */
    public static void apply(ServerPlayer player, float grow) {
        for (Scaled m : MODIFIERS) {
            AttributeInstance inst = player.getAttribute(m.attribute());
            if (inst == null) continue;
            var id = TheGods.id(m.id());
            AttributeModifier current = inst.getModifier(id);
            double amount = m.amount() * grow;
            if (grow <= 0) {
                if (current != null) inst.removeModifier(id);
            } else if (current == null || Math.abs(current.amount() - amount) > 1e-4) {
                inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, m.op()));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void footsteps(ServerPlayer player) {
        if (!player.onGround() || player.getDeltaMovement().horizontalDistanceSqr() < 0.003 || player.tickCount % 9 != 0) return;
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.1, c.z, 30, 1.0, 0.1, 1.0, 0.3);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6f, 0.5f);
        GodManager.shakeArea(level, c, 14, 0.25f, 6);
    }

    // =============================================================== combattimento

    /** Ogni pugno dell'Avatar genera un'onda d'urto a cono. */
    public static void onAvatarMelee(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 c = target.position().add(0, target.getBbHeight() / 2, 0);
        target.push(dir.x * 1.8, 0.6, dir.z * 1.8);
        target.hurtMarked = true;
        for (LivingEntity e : GodUtil.targetsIn(level, player, target.getBoundingBox().inflate(3.5))) {
            if (e == target) continue;
            GodUtil.hurt(e, player.damageSources().playerAttack(player), 8);
            e.push(dir.x * 1.2, 0.4, dir.z * 1.2);
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.SPLASH, c.x, c.y, c.z, 120, 0.8, 0.8, 0.8, 0.6);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 10, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.5f, 0.6f);
        GodManager.shakeArea(level, c, 16, 0.4f, 8);
    }

    public static void markSlam(ServerPlayer player) {
        PENDING_SLAM.add(player.getUUID());
    }

    /** Atterraggio dell'Avatar: onda d'urto proporzionale alla caduta (enorme dopo lo Schianto). */
    public static void onLanding(ServerPlayer player, float distance) {
        boolean slam = PENDING_SLAM.remove(player.getUUID());
        if (!slam && distance < 3.5f) return;
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        double radius = slam ? 10 : Math.min(9, 3 + distance * 0.45);
        float damage = slam ? 28 : Math.min(26, 6 + distance * 1.4f);
        for (LivingEntity e : GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(radius, 4, radius))) {
            double d = e.position().distanceTo(c);
            if (d > radius) continue;
            float k = (float) (1.1 - d / (radius + 1));
            GodUtil.hurtNow(e, player.damageSources().playerAttack(player), damage * k);
            Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize();
            e.push(push.x * 1.6 * k, 1.0 * k + 0.3, push.z * 1.6 * k);
            e.hurtMarked = true;
        }
        int rings = slam ? 6 : 3;
        for (int r = 1; r <= rings; r++) GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.2, 0), r * radius / rings, 30 + r * 10, 1.0);
        level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3, c.z, slam ? 80 : 30, radius * 0.4, 0.3, radius * 0.4, 0.15);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 6, c.z, 300, radius * 0.5, 2, radius * 0.5, 0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, slam ? 3f : 1.8f, 0.5f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.4f);
        GodManager.shakeArea(level, c, slam ? 48 : 28, slam ? 1.6f : 0.9f, slam ? 35 : 18);
    }

    private AvatarManager() {}
}
