package com.thegods.god.poseidon;

import com.thegods.config.GodsConfig;
import com.thegods.entity.HydroBeamEntity;
import com.thegods.entity.LeviathanEntity;
import com.thegods.entity.TidalDomeEntity;
import com.thegods.entity.WaterGolemEntity;
import com.thegods.entity.WaterSpearEntity;
import com.thegods.entity.MaelstromEntity;
import com.thegods.entity.TsunamiEntity;
import com.thegods.entity.WaterPrisonEntity;
import com.thegods.entity.WaterTridentEntity;
import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.registry.ModEntities;
import com.thegods.util.GodUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** I poteri di Poseidone (logica server). */
public final class PoseidonPowers {

    /** Tridente delle Maree: scaglia un gigantesco tridente d'acqua che esplode in un'onda d'urto. */
    public static boolean tidalTrident(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        WaterTridentEntity trident = new WaterTridentEntity(ModEntities.WATER_TRIDENT.get(), level);
        Vec3 start = player.getEyePosition().add(look.scale(1.5)).add(0, -0.2, 0);
        trident.setPos(start.x, start.y, start.z);
        trident.setOwner(player);
        trident.launch(look.scale(2.6));
        level.addFreshEntity(trident);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 2.0f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    /** Getto Abissale: un raggio d'acqua ad altissima pressione che segue lo sguardo. */
    public static boolean abyssalTorrent(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        HydroBeamEntity beam = new HydroBeamEntity(ModEntities.HYDRO_BEAM.get(), level);
        Vec3 eye = player.getEyePosition();
        beam.setPos(eye.x, eye.y, eye.z);
        beam.setOwner(player);
        level.addFreshEntity(beam);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 2.0f, 0.7f);
        return true;
    }

    /** Passo della Marea: scatto fulmineo dissolvendosi in acqua, travolge chi incontra. */
    public static boolean tideStep(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 from = player.position().add(0, 0.6, 0);
        Vec3 hit = GodUtil.raycastBlocks(level, player, from, look, 16);
        double dist = Math.max(0, hit.distanceTo(from) - 1.0);
        Vec3 dest = player.position();
        // Arretra finché la destinazione non è libera
        for (double d = dist; d > 0; d -= 0.5) {
            Vec3 candidate = player.position().add(look.scale(d));
            AABB box = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (level.noCollision(player, box)) {
                dest = candidate;
                break;
            }
        }
        Vec3 origin = player.position();
        if (dest.distanceToSqr(origin) < 1) return false;

        AABB sweep = new AABB(origin, dest).inflate(1.5);
        for (LivingEntity e : GodUtil.targetsIn(level, player, sweep)) {
            if (e.getBoundingBox().inflate(1.2).clip(origin.add(0, 1, 0), dest.add(0, 1, 0)).isEmpty()) continue;
            GodUtil.hurt(e, player.damageSources().playerAttack(player), 8);
            Vec3 side = e.position().subtract(origin).normalize();
            e.push(side.x * 0.8, 0.5, side.z * 0.8);
            e.hurtMarked = true;
        }
        GodUtil.particleLine(level, ParticleTypes.SPLASH, origin.add(0, 1, 0), dest.add(0, 1, 0), 0.4, 8, 0.4);
        GodUtil.particleLine(level, ParticleTypes.BUBBLE_POP, origin.add(0, 1, 0), dest.add(0, 1, 0), 0.6, 3, 0.3);
        GodUtil.particleLine(level, ParticleTypes.FALLING_WATER, origin.add(0, 1.5, 0), dest.add(0, 1.5, 0), 0.5, 4, 0.5);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.2f, 1.3f);

        player.teleportTo(dest.x, dest.y, dest.z);
        player.setDeltaMovement(look.scale(0.4));
        player.hurtMarked = true;
        player.fallDistance = 0;
        level.playSound(null, dest.x, dest.y, dest.z, SoundEvents.DOLPHIN_SPLASH, SoundSource.PLAYERS, 1.5f, 0.8f);
        level.sendParticles(ParticleTypes.SPLASH, dest.x, dest.y + 1, dest.z, 60, 0.6, 0.8, 0.6, 0.3);
        return true;
    }

    /** Prigione d'Acqua: imprigiona il bersaglio in una sfera d'acqua dove annega. */
    public static boolean waterPrison(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = GodUtil.raycastBlocks(level, player, eye, look, 40);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end,
                new AABB(eye, end).inflate(1.5), e -> GodUtil.isValidTarget(player, e), 0.6f);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            player.displayClientMessage(Component.translatable("message.thegods.no_target")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        WaterPrisonEntity prison = new WaterPrisonEntity(ModEntities.WATER_PRISON.get(), level);
        prison.setOwner(player);
        prison.trap(target);
        level.addFreshEntity(prison);
        GodUtil.particleLine(level, ParticleTypes.SPLASH, eye, target.getEyePosition(), 0.5, 2, 0.1);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 2.0f, 0.8f);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.5f, 0.7f);
        return true;
    }

    /** Vortice degli Abissi: un maelstrom risucchia tutto, poi esplode in un geyser. */
    public static boolean maelstrom(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 at = GodUtil.raycastBlocks(level, player, eye, player.getLookAngle(), 48);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(at.x), Mth.floor(at.z));
        double y = (ground <= at.y + 1 && ground > at.y - 24) ? ground : at.y;
        MaelstromEntity m = new MaelstromEntity(ModEntities.MAELSTROM.get(), level);
        m.setPos(at.x, y, at.z);
        m.setOwner(player);
        m.setSize(10f);
        level.addFreshEntity(m);
        level.playSound(null, at.x, y, at.z, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundSource.PLAYERS, 3.0f, 0.5f);
        level.playSound(null, at.x, y, at.z, SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.PLAYERS, 2.0f, 0.5f);
        return true;
    }

    /** Tsunami: un muro d'oceano alto come un palazzo travolge tutto davanti a te. */
    public static boolean tsunami(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        spawnTsunami(level, player, player.getYRot(), 3, 26f, 9f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3.0f, 0.4f);
        GodManager.shakeArea(level, player.position(), 40, 0.8f, 40);
        return true;
    }

    public static TsunamiEntity spawnTsunami(ServerLevel level, ServerPlayer owner, float yRot, double distance, float width, float height) {
        Vec3 dir = GodUtil.horizontalDir(yRot);
        Vec3 at = owner.position().add(dir.scale(distance));
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        double y = Math.abs(ground - at.y) < 8 ? ground : at.y;
        TsunamiEntity wave = new TsunamiEntity(ModEntities.TSUNAMI.get(), level);
        wave.moveTo(at.x, y, at.z, yRot, 0);
        wave.setOwner(owner);
        wave.setSize(width);
        wave.setMaxHeight(height);
        level.addFreshEntity(wave);
        return wave;
    }

    // ================================================================ COSTRUTTI D'ACQUA

    /** Pioggia di Lance: dieci lance d'acqua solida si formano alle tue spalle e partono una dopo l'altra. */
    public static boolean waterSpears(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition(), look = player.getLookAngle();
        List<LivingEntity> targets = GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(40)).stream()
                .filter(e -> !(e instanceof WaterGolemEntity))
                .filter(e -> e.getEyePosition().subtract(eye).normalize().dot(look) > 0.35)
                .sorted(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .toList();
        int count = 10;
        for (int i = 0; i < count; i++) {
            double a = Math.toRadians(-80 + 160.0 * i / (count - 1));
            Vec3 offset = new Vec3(Math.sin(a) * 2.6, 1.9 + Math.cos(a) * 1.5, -0.9);
            WaterSpearEntity spear = new WaterSpearEntity(ModEntities.WATER_SPEAR.get(), level);
            Vec3 pos = player.position().add(offset.yRot(-player.getYRot() * Mth.DEG_TO_RAD));
            spear.setPos(pos.x, pos.y, pos.z);
            spear.setOwner(player);
            spear.setup(offset, 12 + i * 3, targets.isEmpty() ? null : targets.get(i % targets.size()));
            level.addFreshEntity(spear);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 2f, 0.6f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1f, 1.8f);
        return true;
    }

    /** Cupola delle Maree: una sfera d'acqua che riflette i proiettili e respinge i nemici. */
    public static boolean tidalDome(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        TidalDomeEntity dome = new TidalDomeEntity(ModEntities.TIDAL_DOME.get(), level);
        dome.setPos(player.getX(), player.getY(), player.getZ());
        dome.setOwner(player);
        dome.setSize(5f);
        level.addFreshEntity(dome);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 2f, 0.7f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 2f, 0.6f);
        GodUtil.particleRing(level, ParticleTypes.SPLASH, player.position().add(0, 0.2, 0), 5, 64, 0.4);
        return true;
    }

    /** Il Leviatano: un serpente marino d'acqua solida emerge dal terreno e caccia i nemici. */
    public static boolean leviathan(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = player.position().add(dir.scale(4));
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        LeviathanEntity lev = new LeviathanEntity(ModEntities.LEVIATHAN.get(), level);
        lev.setPos(at.x, Math.abs(ground - at.y) < 8 ? ground - 1 : at.y - 1, at.z);
        lev.setOwner(player);
        lev.launch(dir);
        level.addFreshEntity(lev);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 2f, 0.4f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.4f);
        GodManager.shakeArea(level, at, 30, 0.7f, 20);
        return true;
    }

    /** Il Titano degli Abissi: un golem d'acqua gigantesco che combatte per te (e che puoi cavalcare). */
    public static boolean titan(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        // un solo titano per evocatore
        for (WaterGolemEntity old : level.getEntitiesOfClass(WaterGolemEntity.class, player.getBoundingBox().inflate(256), g -> g.isOwnedBy(player))) {
            old.kill();
        }
        Vec3 dir = player.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 at = player.position().add(dir.scale(4));
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(at.x), Mth.floor(at.z));
        WaterGolemEntity golem = ModEntities.WATER_GOLEM.get().create(level);
        if (golem == null) return false;
        golem.moveTo(at.x, Math.abs(ground - at.y) < 8 ? ground : at.y, at.z, player.getYRot(), 0);
        golem.setYHeadRot(player.getYRot());
        golem.setYBodyRot(player.getYRot());
        golem.setOwner(player);
        level.addFreshEntity(golem);
        level.sendParticles(ParticleTypes.SPLASH, at.x, golem.getY() + 0.5, at.z, 400, 2, 1, 2, 0.6);
        for (int r = 1; r <= 3; r++) GodUtil.particleRing(level, ParticleTypes.SPLASH, golem.position().add(0, 0.3, 0), r * 1.5, 32, 0.6);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 2f, 0.3f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 2f, 0.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 3f, 0.3f);
        GodManager.sendTitle(player, "title.thegods.poseidon_titan", "title.thegods.poseidon_titan.sub", PoseidonGod.COLOR);
        GodManager.shakeArea(level, at, 40, 1.0f, 30);
        return true;
    }

    /** ULTIMATE — L'Ira di Poseidone: tempesta, otto tsunami e fulmini sugli empi. */
    public static boolean wrathOfTheSeas(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        data.setUltimateTicks(240);
        if (GodsConfig.ULTIMATE_CHANGES_WEATHER.get()) {
            level.setWeatherParameters(0, 2400, true, true);
        }
        for (int i = 0; i < 8; i++) {
            spawnTsunami(level, player, player.getYRot() + i * 45f, 4, 18f, 11f);
        }
        List<LivingEntity> targets = GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(40));
        int strikes = 0;
        for (LivingEntity t : targets) {
            if (strikes++ >= 12) break;
            strikeLightning(level, player, t.position());
        }
        player.setDeltaMovement(0, 1.4, 0);
        player.hurtMarked = true;
        if (GodsConfig.DIVINE_FLIGHT.get()) {
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }

        Vec3 c = player.position();
        level.playSound(null, c.x, c.y, c.z, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 3.0f, 0.4f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0f, 0.6f);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 3.0f, 0.5f);
        for (int i = 0; i < 6; i++) GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.2 + i * 0.5, 0), 2 + i * 1.5, 64, 1.0);
        level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 8, c.z, 600, 12, 2, 12, 0);

        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(player) < 96 * 96) {
                GodManager.sendTitle(p, "title.thegods.poseidon_wrath", "title.thegods.poseidon_wrath.sub", PoseidonGod.COLOR);
            }
        }
        GodManager.shakeArea(level, c, 96, 1.6f, 70);
        return true;
    }

    static void tickWrath(ServerPlayer player, GodData data) {
        ServerLevel level = player.serverLevel();
        int t = data.ultimateTicks();
        if (t % 12 == 0) {
            List<LivingEntity> targets = GodUtil.targetsIn(level, player, player.getBoundingBox().inflate(36));
            if (!targets.isEmpty()) {
                LivingEntity target = targets.get(level.random.nextInt(targets.size()));
                strikeLightning(level, player, target.position());
            } else {
                double a = level.random.nextDouble() * Math.PI * 2;
                double r = 10 + level.random.nextDouble() * 20;
                Vec3 p = player.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(p.x), Mth.floor(p.z));
                strikeLightning(level, player, new Vec3(p.x, y, p.z), true);
            }
        }
        if (t % 4 == 0) {
            Vec3 c = player.position();
            level.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 3, c.z, 30, 4, 1, 4, 0);
            GodUtil.particleRing(level, ParticleTypes.SPLASH, c.add(0, 0.5, 0), 3, 24, 0.4);
        }
    }

    private static void strikeLightning(ServerLevel level, ServerPlayer owner, Vec3 at) {
        strikeLightning(level, owner, at, false);
    }

    private static void strikeLightning(ServerLevel level, ServerPlayer owner, Vec3 at, boolean visualOnly) {
        GodUtil.divineLightning(level, owner, at, visualOnly ? 0 : 14);
    }

    private PoseidonPowers() {}
}
