package com.thegods.client.render;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Movimento secondario delle penne: ogni penna "insegue" con una molla smorzata
 * la spinta del battito d'ala e il vento dato dalla velocità del giocatore.
 * Le punte reagiscono più lentamente della base: sembra stoffa/acqua, non cartone.
 */
public final class WingPhysics {
    public static final int FEATHERS = 24; // per ala (remiganti + copritrici)
    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        final float[] lag = new float[FEATHERS * 2], prev = new float[FEATHERS * 2], vel = new float[FEATHERS * 2];
        float flapPrev;
        int lastSeen;
    }

    public static void tick(AbstractClientPlayer p, float t, boolean airborne) {
        State s = STATES.computeIfAbsent(p.getUUID(), k -> new State());
        s.lastSeen = p.tickCount;
        float flap = Mth.sin(t * (airborne ? 0.2f : 0.075f));
        float flapVel = flap - s.flapPrev;
        s.flapPrev = flap;
        Vec3 m = p.getDeltaMovement();
        float speed = (float) m.horizontalDistance();
        for (int i = 0; i < FEATHERS * 2; i++) {
            float f = (i % FEATHERS) / (float) FEATHERS;
            float target = -flapVel * 55f * (0.35f + f) + speed * 30f * (0.3f + f) - (float) m.y * 18f * f;
            // molla smorzata: le punte (f alto) sono più morbide
            float k = 0.16f - 0.08f * f, damp = 0.78f;
            s.prev[i] = s.lag[i];
            s.vel[i] = s.vel[i] * damp + (target - s.lag[i]) * k;
            s.lag[i] = Mth.clamp(s.lag[i] + s.vel[i], -35, 35);
        }
    }

    /** Angolo extra (gradi) della penna {@code i} dell'ala {@code side} (0 = destra, 1 = sinistra). */
    public static float lag(AbstractClientPlayer p, int side, int i, float pt) {
        State s = STATES.get(p.getUUID());
        if (s == null) return 0;
        int idx = side * FEATHERS + Math.min(i, FEATHERS - 1);
        return Mth.lerp(pt, s.prev[idx], s.lag[idx]);
    }

    public static void cleanup(int now) {
        STATES.values().removeIf(s -> Math.abs(now - s.lastSeen) > 200);
    }

    private WingPhysics() {}
}
