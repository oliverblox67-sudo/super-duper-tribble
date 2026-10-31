package com.thegods.client;

import com.thegods.god.GodData;
import com.thegods.god.GodManager;
import com.thegods.client.gui.BodyForgeScreen;
import com.thegods.entity.WaterGolemEntity;
import com.thegods.god.Gods;
import com.thegods.god.Limb;
import com.thegods.god.LimbManager;
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
    /** Scena da girare: "all" (tutto) oppure una scena breve: titan, avatar, leviathan, wings. */
    private static final String SCENE = System.getProperty("thegods.showcase.scene", "all");
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
        if (mc.screen != null && !(mc.screen instanceof BodyForgeScreen)) mc.setScreen(null);
        ticks++;
        if (!SCENE.equals("all")) {
            ShowcaseScenes.tick(mc, SCENE, ticks);
            return;
        }
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
            // --- Forgia del Corpo: braccio destro e gamba sinistra d'acqua
            case 122 -> server(mc, (level, p) -> {
                LimbManager.perform(p, Limb.RIGHT_ARM, LimbManager.Action.FORGE);
                LimbManager.perform(p, Limb.LEFT_LEG, LimbManager.Action.FORGE);
            });
            case 160 -> shot(mc, "03_water_limbs");
            case 162 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 168 -> shot(mc, "04_first_person_arm");
            case 170 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                BodyForgeScreen screen = new BodyForgeScreen();
                mc.setScreen(screen);
                screen.select(Limb.RIGHT_ARM);
            }
            case 180 -> shot(mc, "05_body_forge");
            case 182 -> mc.setScreen(null);
            // --- Pioggia di Lance
            case 184 -> {
                look(mc, 0, 0);
                zombies(mc, 3, 14);
            }
            case 188 -> cast(mc, 2);
            case 196 -> turn(mc, 25, 0);
            case 200 -> shot(mc, "06_spears_forming");
            case 216 -> shot(mc, "07_spears_flying");
            // --- Leviatano
            case 270 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                look(mc, 0, -10);
                zombies(mc, 3, 18);
            }
            case 276 -> cast(mc, 8);
            case 280 -> server(mc, (level, p) -> GodManager.toggleManifest(p)); // ali chiuse per vedere il Leviatano
            case 300 -> turn(mc, 20, -15);
            case 304 -> shot(mc, "09_leviathan");
            case 322 -> shot(mc, "10_leviathan_late");
            case 330 -> server(mc, (level, p) -> GodManager.toggleManifest(p));
            // --- Titano
            case 400 -> {
                look(mc, 0, 0);
                zombies(mc, 2, 12);
            }
            case 404 -> cast(mc, 9);
            case 440 -> turn(mc, 40, 8);
            case 446 -> shot(mc, "11_titan");
            case 450 -> server(mc, (level, p) -> {
                for (WaterGolemEntity g : level.getEntitiesOfClass(WaterGolemEntity.class, p.getBoundingBox().inflate(30))) {
                    if (g.isOwnedBy(p)) p.startRiding(g, true);
                }
            });
            case 460 -> look(mc, 0, 15);
            case 470 -> server(mc, (level, p) -> GodManager.useAbility(p, 0)); // in groppa: comando del Titano
            case 478 -> shot(mc, "12_titan_ride_slam");
            case 500 -> server(mc, (level, p) -> p.stopRiding());
            // --- Tsunami e Ira
            case 520 -> look(mc, 0, -5);
            case 524 -> cast(mc, 7);
            case 532 -> turn(mc, 55, 5);
            case 540 -> shot(mc, "13_tsunami");
            case 580 -> look(mc, 0, 0);
            case 584 -> cast(mc, 10);
            case 600 -> shot(mc, "14_wrath");
            case 630 -> cast(mc, 5);
            case 634 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 646 -> shot(mc, "15_dome");
            case 680 -> mc.stop();
            default -> {}
        }
    }

    static void zombies(Minecraft mc, int count, double distance) {
        server(mc, (level, p) -> {
            Vec3 f = p.getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-f.z, 0, f.x);
            for (int i = 0; i < count; i++) {
                Mob z = EntityType.ZOMBIE.create(level);
                if (z == null) continue;
                Vec3 at = p.position().add(f.scale(distance)).add(side.scale((i - (count - 1) / 2.0) * 3));
                z.moveTo(at.x, p.getY(), at.z, 180, 0);
                z.setNoAi(true);
                level.addFreshEntity(z);
            }
        });
    }

    static void look(Minecraft mc, float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    /** Gira il giocatore di {@code yaw} gradi (solo client: la telecamera segue). */
    static void turn(Minecraft mc, float yaw, float pitch) {
        if (mc.player == null) return;
        mc.player.setYRot(mc.player.getYRot() + yaw);
        mc.player.setXRot(pitch);
    }

    static void cast(Minecraft mc, int index) {
        server(mc, (level, p) -> {
            GodData d = GodManager.data(p);
            d.clearCooldowns();
            d.setEnergy(100);
            GodManager.useAbility(p, index);
        });
    }

    static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
    }

    static void server(Minecraft mc, java.util.function.BiConsumer<ServerLevel, ServerPlayer> action) {
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
