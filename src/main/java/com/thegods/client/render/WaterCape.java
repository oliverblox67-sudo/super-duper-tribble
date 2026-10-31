package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thegods.client.mesh.GodMesh;
import com.thegods.client.mesh.MeshDrawer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mantello d'acqua con simulazione della stoffa (Verlet):
 * griglia di punti appesa alle spalle, gravità, vento del movimento, vincoli di distanza
 * e collisione con il corpo (capsula). Disegnato con lo shader dell'acqua.
 */
public final class WaterCape {
    private static final int W = 7, H = 12;
    private static final int TOP = 0x0F55B8, BOTTOM = 0x8FDFFF;
    private static final Map<UUID, Cloth> CLOTHS = new HashMap<>();

    private static final class Cloth {
        final Vec3[] pos = new Vec3[W * H], old = new Vec3[W * H], render = new Vec3[W * H], renderPrev = new Vec3[W * H];
        int lastSeen;
        boolean init;
    }

    private static Vec3 anchor(AbstractClientPlayer p, int c, float scale) {
        float b = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(b), 0, Mth.cos(b));
        Vec3 right = new Vec3(-Mth.cos(b), 0, -Mth.sin(b));
        float x = Mth.lerp(c / (float) (W - 1), -0.3f, 0.3f) * scale;
        return p.position().add(right.scale(x)).add(0, 1.4f * scale, 0).add(fwd.scale(-0.2f * scale));
    }

    public static void tick(AbstractClientPlayer p) {
        Cloth cl = CLOTHS.computeIfAbsent(p.getUUID(), k -> new Cloth());
        cl.lastSeen = p.tickCount;
        float scale = p.getScale();
        float vRest = 1.3f * scale / (H - 1);
        if (!cl.init || cl.pos[0].distanceToSqr(anchor(p, 0, scale)) > 64) {
            for (int r = 0; r < H; r++) for (int c = 0; c < W; c++) {
                Vec3 a = anchor(p, c, scale).add(0, -r * vRest, 0);
                cl.pos[r * W + c] = cl.old[r * W + c] = cl.render[r * W + c] = cl.renderPrev[r * W + c] = a;
            }
            cl.init = true;
        }
        float t = p.tickCount;
        float b = p.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(b), 0, -Mth.sin(b));
        // integrazione
        for (int r = 1; r < H; r++) for (int c = 0; c < W; c++) {
            int i = r * W + c;
            Vec3 vel = cl.pos[i].subtract(cl.old[i]).scale(0.95);
            cl.old[i] = cl.pos[i];
            float ripple = Mth.sin(t * 0.25f + r * 0.6f + c * 0.4f) * 0.006f;
            cl.pos[i] = cl.pos[i].add(vel).add(0, -0.032 * scale, 0).add(right.scale(ripple));
        }
        for (int c = 0; c < W; c++) {
            int i = c;
            cl.old[i] = cl.pos[i];
            cl.pos[i] = anchor(p, c, scale);
        }
        // vincoli + collisioni
        Vec3 base = p.position();
        float radius = 0.34f * scale;
        for (int iter = 0; iter < 6; iter++) {
            for (int r = 0; r < H; r++) for (int c = 0; c < W; c++) {
                int i = r * W + c;
                float hRest = 0.6f * scale / (W - 1) * (1 + 0.4f * r / (H - 1));
                if (c + 1 < W) satisfy(cl, i, i + 1, hRest, r == 0);
                if (r + 1 < H) satisfy(cl, i, i + W, vRest, r == 0);
                if (r + 1 < H && c + 1 < W) satisfy(cl, i, i + W + 1, (float) Math.sqrt(hRest * hRest + vRest * vRest), r == 0);
            }
            for (int i = W; i < W * H; i++) {
                Vec3 q = cl.pos[i];
                double dy = q.y - base.y;
                if (dy > 0 && dy < 1.5 * scale) {
                    double dx = q.x - base.x, dz = q.z - base.z, d = Math.sqrt(dx * dx + dz * dz);
                    if (d < radius && d > 1e-5) cl.pos[i] = new Vec3(base.x + dx / d * radius, q.y, base.z + dz / d * radius);
                }
                if (q.y < base.y + 0.03) cl.pos[i] = new Vec3(cl.pos[i].x, base.y + 0.03, cl.pos[i].z);
            }
        }
        for (int i = 0; i < W * H; i++) {
            cl.renderPrev[i] = cl.render[i];
            cl.render[i] = cl.pos[i];
        }
    }

    private static void satisfy(Cloth cl, int a, int b, float rest, boolean aPinned) {
        Vec3 d = cl.pos[b].subtract(cl.pos[a]);
        double len = d.length();
        if (len < 1e-6) return;
        Vec3 corr = d.scale((len - rest) / len);
        if (aPinned) {
            cl.pos[b] = cl.pos[b].subtract(corr);
        } else {
            cl.pos[a] = cl.pos[a].add(corr.scale(0.5));
            cl.pos[b] = cl.pos[b].subtract(corr.scale(0.5));
        }
    }

    /** Disegna il mantello: {@code ps} è posizionato sull'entità (RenderPlayerEvent.Post). */
    public static void render(AbstractClientPlayer p, PoseStack ps, MultiBufferSource buffers, float pt, float alpha) {
        Cloth cl = CLOTHS.get(p.getUUID());
        if (cl == null || !cl.init || alpha <= 0.01f) return;
        Vec3 origin = p.getPosition(pt);
        Vec3[] pts = new Vec3[W * H];
        for (int i = 0; i < W * H; i++) pts[i] = cl.renderPrev[i].lerp(cl.render[i], pt).subtract(origin);
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (p == mc.player && mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_BACK) alpha *= 0.45f;
        VertexConsumer vc = buffers.getBuffer(GodRenderTypes.water(false));
        PoseStack.Pose pose = ps.last();
        for (int r = 0; r < H - 1; r++) for (int c = 0; c < W - 1; c++) {
            int[] idx = {r * W + c, r * W + c + 1, (r + 1) * W + c + 1, (r + 1) * W + c};
            for (int i : idx) {
                int rr = i / W, cc = i % W;
                Vec3 du = pts[rr * W + Math.min(cc + 1, W - 1)].subtract(pts[rr * W + Math.max(cc - 1, 0)]);
                Vec3 dv = pts[Math.min(rr + 1, H - 1) * W + cc].subtract(pts[Math.max(rr - 1, 0) * W + cc]);
                Vec3 n = du.cross(dv);
                n = n.lengthSqr() < 1e-10 ? new Vec3(0, 0, 1) : n.normalize();
                float v = rr / (float) (H - 1);
                int argb = WaterRender.argb(alpha * Mth.lerp(v, 0.88f, 0.5f), WaterRender.lerpColor(v, TOP, BOTTOM));
                MeshDrawer.emit(vc, pose, (float) pts[i].x, (float) pts[i].y, (float) pts[i].z, argb,
                        cc / (float) (W - 1), v * 1.6f, (float) n.x, (float) n.y, (float) n.z, WaterRender.FULL_BRIGHT, GodMesh.Material.WATER);
            }
        }
    }

    public static void cleanup(int now) {
        CLOTHS.values().removeIf(c -> Math.abs(now - c.lastSeen) > 200);
    }

    private WaterCape() {}
}
