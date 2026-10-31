package com.thegods.client;

import com.thegods.entity.WaterGolemEntity;
import com.thegods.god.GodManager;
import com.thegods.god.Gods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Scene brevi della demo automatica, per controllare un singolo modello alla volta. */
final class ShowcaseScenes {

    static void tick(Minecraft mc, String scene, int t) {
        if (t == 40) {
            ShowcaseDirector.server(mc, (level, p) -> {
                level.setDayTime(6000);
                level.setWeatherParameters(6000, 0, false, false);
                GodManager.grant(p, Gods.POSEIDON);
            });
            ShowcaseDirector.look(mc, 0, 5);
        }
        if (t == 50) ShowcaseDirector.server(mc, (level, p) -> GodManager.toggleManifest(p));
        switch (scene) {
            case "titan" -> titan(mc, t);
            case "avatar" -> avatar(mc, t);
            case "leviathan" -> leviathan(mc, t);
            case "wings" -> wings(mc, t);
            default -> {}
        }
    }

    private static void titan(Minecraft mc, int t) {
        switch (t) {
            case 70 -> ShowcaseDirector.cast(mc, indexOf("poseidon_titan"));
            case 110 -> faceTitan(mc, 9, 0);
            case 114 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 120 -> ShowcaseDirector.shot(mc, "titan_front");
            case 122 -> faceTitan(mc, 9, 70);
            case 130 -> ShowcaseDirector.shot(mc, "titan_side");
            case 132 -> ShowcaseDirector.zombies(mc, 1, 3);
            case 150 -> ShowcaseDirector.shot(mc, "titan_punch");
            case 200 -> mc.stop();
            default -> {}
        }
    }

    private static void avatar(Minecraft mc, int t) {
        switch (t) {
            case 70 -> ShowcaseDirector.cast(mc, indexOf("poseidon_avatar"));
            case 72 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 84 -> ShowcaseDirector.shot(mc, "avatar_forming");
            case 120 -> ShowcaseDirector.shot(mc, "avatar_front");
            case 122 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 130 -> ShowcaseDirector.shot(mc, "avatar_back");
            case 132 -> mc.options.setCameraType(CameraType.FIRST_PERSON);
            case 140 -> ShowcaseDirector.shot(mc, "avatar_first_person");
            case 142 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                ShowcaseDirector.zombies(mc, 2, 6);
            }
            case 150 -> ShowcaseDirector.server(mc, (level, p) -> GodManager.useAbility(p, 0)); // pugno di marea
            case 154 -> ShowcaseDirector.shot(mc, "avatar_punch");
            case 190 -> mc.stop();
            default -> {}
        }
    }

    private static void leviathan(Minecraft mc, int t) {
        switch (t) {
            case 60 -> ShowcaseDirector.server(mc, (level, p) -> p.setYRot(-90)); // guarda verso +X
            case 62 -> ShowcaseDirector.look(mc, -90, 0);
            case 66 -> ShowcaseDirector.cast(mc, indexOf("poseidon_leviathan"));
            case 67 -> ShowcaseDirector.server(mc, (level, p) -> {
                for (var lev : level.getEntitiesOfClass(com.thegods.entity.LeviathanEntity.class, p.getBoundingBox().inflate(20))) {
                    lev.showcasePath = true;
                }
            });
            case 70 -> ShowcaseDirector.server(mc, (level, p) -> GodManager.toggleManifest(p));
            case 72 -> ShowcaseDirector.server(mc, (level, p) -> {
                // la telecamera di lato al percorso del serpente
                for (var lev : level.getEntitiesOfClass(com.thegods.entity.LeviathanEntity.class, p.getBoundingBox().inflate(20))) {
                    p.teleportTo(level, lev.getX(), lev.getY() + 6, lev.getZ() - 16, 0, 0);
                }
            });
            case 74 -> ShowcaseDirector.look(mc, 0, 8);
            case 100 -> ShowcaseDirector.shot(mc, "leviathan_rise");
            case 116 -> ShowcaseDirector.shot(mc, "leviathan_head");
            case 130 -> ShowcaseDirector.shot(mc, "leviathan_body");
            case 150 -> ShowcaseDirector.shot(mc, "leviathan_late");
            case 200 -> ShowcaseDirector.shot(mc, "leviathan_turn");
            case 246 -> ShowcaseDirector.shot(mc, "leviathan_breath");
            case 262 -> ShowcaseDirector.shot(mc, "leviathan_breath2");
            case 300 -> mc.stop();
            default -> {}
        }
    }

    private static void wings(Minecraft mc, int t) {
        switch (t) {
            case 90 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 100 -> ShowcaseDirector.shot(mc, "wings_front");
            case 102 -> ShowcaseDirector.turn(mc, 90, 0);
            case 110 -> ShowcaseDirector.shot(mc, "wings_side");
            case 112 -> {
                ShowcaseDirector.turn(mc, 90, 0);
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 120 -> ShowcaseDirector.shot(mc, "wings_back");
            case 122 -> ShowcaseDirector.server(mc, (level, p) -> {
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.setDeltaMovement(0, 0.6, 0);
                p.hurtMarked = true;
            });
            case 124 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 140 -> ShowcaseDirector.shot(mc, "wings_flying");
            case 170 -> mc.stop();
            default -> {}
        }
    }

    /** Mette il giocatore a {@code dist} blocchi dal Titano, a un angolo dato, a guardarlo. */
    private static void faceTitan(Minecraft mc, double dist, float angleDeg) {
        ShowcaseDirector.server(mc, (level, p) -> {
            for (WaterGolemEntity g : level.getEntitiesOfClass(WaterGolemEntity.class, p.getBoundingBox().inflate(40))) {
                float yaw = g.getYRot() + angleDeg;
                Vec3 dir = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
                Vec3 at = g.position().add(dir.scale(dist));
                p.teleportTo(level, at.x, g.getY() + 2, at.z, yaw + 180, -12);
                return;
            }
        });
    }

    static int indexOf(String id) {
        var list = Gods.POSEIDON.abilities();
        for (int i = 0; i < list.size(); i++) if (list.get(i).id().equals(id)) return i;
        return 0;
    }

    private ShowcaseScenes() {}
}
