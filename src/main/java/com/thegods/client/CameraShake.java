package com.thegods.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Scossa della telecamera, rispetta l'opzione vanilla "Effetti di distorsione". */
public final class CameraShake {
    private static float intensity;
    private static int ticks, duration;

    public static void add(float strength, int length) {
        if (ticks <= 0 || strength >= current(0)) {
            intensity = strength;
            ticks = length;
            duration = Math.max(1, length);
        }
    }

    public static void tick() {
        if (ticks > 0) ticks--;
    }

    public static float current(float partialTick) {
        if (ticks <= 0) return 0;
        float scale = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        return intensity * Mth.clamp((ticks - partialTick) / duration, 0, 1) * scale;
    }

    private CameraShake() {}
}
