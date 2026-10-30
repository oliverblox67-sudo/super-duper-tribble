package com.thegods.client;

import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.god.Gods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;


/**
 * Solo per sviluppo: con -Dthegods.showcase=true entra nel mondo, diventa Poseidone,
 * lancia tutti i poteri e salva screenshot in run/screenshots/. Poi chiude il gioco.
 * Disattivato in gioco normale.
 */
public final class ShowcaseDirector {
    public static final boolean ENABLED = Boolean.getBoolean("thegods.showcase");
    private static int ticks, bootTicks;
    private static boolean worldRequested;

    static void tick(Minecraft mc) {
        if (mc.level == null) {
            // Crea un mondo piatto nuovo appena il gioco è pronto
            if (!worldRequested && ++bootTicks > 40) {
                worldRequested = true;
                String name = "showcase-" + System.currentTimeMillis();
                mc.createWorldOpenFlows().createFreshLevel(name,
                        new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(20261030L, false, false),
                        ra -> ra.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                        new TitleScreen());
            }
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        ticks++;
        switch (ticks) {
            case 40 -> server(mc, (level, p) -> {
                level.setDayTime(6000);
                level.setWeatherParameters(6000, 0, false, false);
                GodManager.grant(p, Gods.POSEIDON);
            });
            case 41 -> look(mc, 0, 10);
            case 60 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                server(mc, (level, p) -> GodManager.toggleManifest(p));
            }
            case 100 -> shot(mc, "01_poseidon_back");
            case 102 -> look(mc, 0, -5);
            case 104 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 120 -> shot(mc, "02_poseidon_front");
            case 124 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                cast(mc, 5); // tsunami
            }
            case 132 -> turn(mc, 55, 5);
            case 140 -> shot(mc, "03_tsunami");
            case 150 -> shot(mc, "04_tsunami_later");
            case 196 -> look(mc, 0, 35);
            case 200 -> cast(mc, 4); // maelstrom
            case 236 -> turn(mc, 60, 15);
            case 245 -> shot(mc, "05_maelstrom");
            case 296 -> look(mc, 0, 0);
            case 300 -> cast(mc, 0); // tridente
            case 302 -> turn(mc, 25, 0);
            case 304 -> shot(mc, "06_trident");
            case 320 -> cast(mc, 1); // getto
            case 326 -> turn(mc, 30, 0);
            case 332 -> shot(mc, "07_torrent");
            case 380 -> server(mc, (level, p) -> {
                Mob z = EntityType.ZOMBIE.create(level);
                if (z != null) {
                    Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
                    z.moveTo(at.x, p.getY(), at.z, 180, 0);
                    z.setNoAi(true);
                    level.addFreshEntity(z);
                }
            });
            case 376 -> look(mc, 0, 8);
            case 384 -> cast(mc, 3); // prigione
            case 394 -> turn(mc, 40, 10);
            case 400 -> shot(mc, "08_prison");
            case 426 -> look(mc, 0, 0);
            case 430 -> cast(mc, 6); // ira
            case 446 -> shot(mc, "09_wrath");
            case 452 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 470 -> shot(mc, "10_wrath_front");
            case 520 -> mc.stop();
            default -> {}
        }
    }

    private static void look(Minecraft mc, float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    /** Gira il giocatore di {@code yaw} gradi (solo client: la telecamera segue). */
    private static void turn(Minecraft mc, float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYRot(mc.player.getYRot() + yaw);
        mc.player.setXRot(pitch);
    }

    private static void cast(Minecraft mc, int index) {
        server(mc, (level, p) -> {
            GodData d = GodManager.data(p);
            d.clearCooldowns();
            d.setEnergy(100);
            GodManager.useAbility(p, index);
        });
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
    }

    private static void server(Minecraft mc, java.util.function.BiConsumer<ServerLevel, ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) return;
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) action.accept(p.serverLevel(), p);
        });
    }

    private ShowcaseDirector() {}
}
