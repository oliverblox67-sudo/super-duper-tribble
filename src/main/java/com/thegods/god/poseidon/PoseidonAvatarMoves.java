package com.thegods.god.poseidon;

import com.thegods.god.AvatarManager;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.util.GodUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Le mosse dell'Avatar degli Abissi (il Titano guidato da dentro). */
public final class PoseidonAvatarMoves {

    /** Avvia la trasformazione (potere normale). */
    public static boolean become(ServerPlayer player, GodData data) {
        if (data.isAvatar()) {
            AvatarManager.end(player, data);
            return false;
        }
        AvatarManager.start(player, data, PoseidonGod.COLOR);
        return true;
    }

    /** Pugno di Marea: un colpo che scaglia un'onda d'urto a cono lunga 11 blocchi. */
    public static boolean tidalPunch(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        player.swing(InteractionHand.MAIN_HAND, true);
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 flat = look.multiply(1, 0, 1).normalize();
        for (LivingEntity e : GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(12))) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double d = to.length();
            if (d > 12 || to.normalize().dot(look) < 0.8) continue;
            GodUtil.hurtNow(e, player.damageSources().playerAttack(player), 18 * (float) (1.2 - d / 15));
            e.push(flat.x * 2.6, 0.7, flat.z * 2.6);
            e.hurtMarked = true;
        }
        Vec3 from = eye.add(look.scale(2));
        for (int i = 0; i < 10; i++) {
            Vec3 p = from.add(look.scale(i));
            double r = 0.4 + i * 0.18;
            level.sendParticles(ParticleTypes.SPLASH, p.x, p.y, p.z, 20, r, r, r, 0.4);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 3, r * 0.6, r * 0.6, r * 0.6, 0.05);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 2f, 0.4f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2.5f, 0.5f);
        GodManager.shakeArea(level, player.position(), 24, 0.6f, 12);
        return true;
    }

    /** Schianto Abissale: un balzo enorme, poi un'esplosione d'acqua all'atterraggio. */
    public static boolean abyssalSlam(ServerPlayer player, GodData data) {
        if (!player.onGround() && !player.isInWater()) return false;
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        player.setDeltaMovement(look.x * 0.9, 1.65, look.z * 0.9);
        player.hurtMarked = true;
        AvatarManager.markSlam(player);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 0.2, player.getZ(), 120, 1.5, 0.2, 1.5, 0.5);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2f, 0.5f);
        return true;
    }

    /** Onda del Titano: una spazzata del braccio solleva uno tsunami. */
    public static boolean titanWave(ServerPlayer player, GodData data) {
        player.swing(InteractionHand.OFF_HAND, true);
        PoseidonPowers.spawnTsunami(player.serverLevel(), player, player.getYRot(), 4, 22f, 8f);
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.35f);
        GodManager.shakeArea(level, player.position(), 30, 0.7f, 20);
        return true;
    }

    /** Ruggito degli Abissi: un'onda sonora respinge, rallenta e indebolisce tutti attorno. */
    public static boolean roar(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 c = player.position();
        for (LivingEntity e : GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(18))) {
            double d = e.position().distanceTo(c);
            if (d > 18) continue;
            float k = (float) (1.1 - d / 20);
            GodUtil.hurtNow(e, player.damageSources().playerAttack(player), 6 * k);
            Vec3 push = e.position().subtract(c).multiply(1, 0, 1).normalize();
            e.push(push.x * 2.2 * k, 0.5 * k, push.z * 2.2 * k);
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
        for (int r = 1; r <= 5; r++) {
            GodUtil.particleRing(level, ParticleTypes.CLOUD, c.add(0, 1 + r * 0.6, 0), r * 3.2, 24 + r * 10, 0.6);
            GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.3, 0), r * 3.2, 24 + r * 10, 0.8);
        }
        level.playSound(null, c.x, c.y, c.z, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 3f, 0.45f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 2f, 0.4f);
        GodManager.shakeArea(level, c, 40, 1.3f, 30);
        return true;
    }

    public static boolean leave(ServerPlayer player, GodData data) {
        AvatarManager.end(player, data);
        return true;
    }

    private PoseidonAvatarMoves() {}
}
