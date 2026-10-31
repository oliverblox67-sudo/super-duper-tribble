package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.client.mesh.GodMesh;
import com.thegods.client.mesh.MeshDrawer;
import com.thegods.client.mesh.MeshLibrary;
import com.thegods.client.mesh.Skeleton;
import com.thegods.entity.LeviathanEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Il Leviatano 2.0:
 * - corpo: spline di Catmull-Rom attraverso la scia della testa, anelli con profilo a cresta dorsale
 *   e ventre più chiaro, frame a rotazione minima (niente torsioni), scaglie disegnate dallo shader;
 * - testa scolpita (mesh "leviathan_head") con mandibola animata, denti, corna, creste e occhi;
 * - pinna dorsale continua + spuntoni, due paia di pinne pettorali che remano, pinna caudale;
 * - barbigli simulati come corde e soffio d'acqua dalle fauci.
 */
public class LeviathanRenderer extends SpellRenderer<LeviathanEntity> {
    private static final int DEEP = 0x072E7A, MID = 0x1F86D6, BELLY = 0xA8E6FF, CORE = 0x8FF0FF;
    private static final int SIDES = 14;
    private Skeleton headSkeleton;

    public LeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(LeviathanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        GodMesh head = MeshLibrary.get("leviathan_head");
        GodMesh fins = MeshLibrary.get("leviathan_fins");
        if (head == null || fins == null) return;
        Vec3 base = e.getPosition(pt);
        List<Vec3> ctrl = new ArrayList<>();
        ctrl.add(base);
        for (Vec3 p : e.trail) if (p.distanceToSqr(ctrl.get(ctrl.size() - 1)) > 0.01) ctrl.add(p);
        if (ctrl.size() < 4) return;
        float t = e.tickCount + pt;
        float fade = Mth.clamp((LeviathanEntity.LIFETIME - t) / 8f, 0, 1);

        // ---- spline e frame
        List<Vec3> c = catmullRom(ctrl, 2);
        int n = c.size();
        Vec3[] tan = new Vec3[n], nor = new Vec3[n], bin = new Vec3[n];
        for (int i = 0; i < n; i++) {
            Vec3 a = c.get(Math.max(i - 1, 0)), b = c.get(Math.min(i + 1, n - 1));
            Vec3 d = a.subtract(b); // verso la testa = "avanti"
            tan[i] = d.lengthSqr() < 1e-8 ? (i > 0 ? tan[i - 1] : new Vec3(0, 0, 1)) : d.normalize();
        }
        Vec3 up0 = Math.abs(tan[0].y) > 0.97 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        nor[0] = up0.subtract(tan[0].scale(up0.dot(tan[0]))).normalize();
        for (int i = 0; i < n - 1; i++) { // doppia riflessione (frame a rotazione minima)
            Vec3 v1 = c.get(i + 1).subtract(c.get(i));
            double c1 = v1.dot(v1);
            if (c1 < 1e-10) {
                nor[i + 1] = nor[i];
                continue;
            }
            Vec3 rL = nor[i].subtract(v1.scale(2 / c1 * v1.dot(nor[i])));
            Vec3 tL = tan[i].subtract(v1.scale(2 / c1 * v1.dot(tan[i])));
            Vec3 v2 = tan[i + 1].subtract(tL);
            double c2 = v2.dot(v2);
            nor[i + 1] = c2 < 1e-10 ? rL : rL.subtract(v2.scale(2 / c2 * v2.dot(rL)));
        }
        for (int i = 0; i < n; i++) bin[i] = tan[i].cross(nor[i]).normalize();
        float[] rad = new float[n];
        for (int i = 0; i < n; i++) {
            float u = i / (float) (n - 1);
            rad[i] = radius(u) * fade * (1 + 0.04f * Mth.sin(u * 30 - t * 0.4f));
        }

        // ---- corpo a scaglie
        VertexConsumer body = buffers.getBuffer(GodRenderTypes.water(false));
        PoseStack.Pose pose = ps.last();
        float vAcc = 0;
        Vector3f[] prevP = null, prevN = null;
        float[] prevS = null;
        float prevV = 0;
        for (int i = 0; i < n; i++) {
            if (i > 0) vAcc += (float) c.get(i).distanceTo(c.get(i - 1)) / 3f;
            Vector3f[] ringP = new Vector3f[SIDES + 1], ringN = new Vector3f[SIDES + 1];
            float[] ringS = new float[SIDES + 1];
            for (int k = 0; k <= SIDES; k++) {
                float th = Mth.TWO_PI * k / SIDES;
                float ct = Mth.cos(th), st = Mth.sin(th);
                float rr = rad[i] * (1 + 0.16f * (float) Math.pow(Math.max(0, ct), 6)) * Mth.lerp(Math.max(0, -ct), 1, 0.84f);
                Vec3 dir = nor[i].scale(ct).add(bin[i].scale(st));
                Vec3 p = c.get(i).add(dir.scale(rr)).subtract(base);
                ringP[k] = new Vector3f((float) p.x, (float) p.y, (float) p.z);
                ringN[k] = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
                ringS[k] = 0.08f + 0.85f * (float) Math.pow(Math.max(0, -ct), 1.5);
            }
            if (prevP != null) {
                for (int k = 0; k < SIDES; k++) {
                    int[] ks = {k, k + 1, k + 1, k};
                    boolean[] cur = {false, false, true, true};
                    for (int q = 0; q < 4; q++) {
                        Vector3f pp = cur[q] ? ringP[ks[q]] : prevP[ks[q]];
                        Vector3f nn = cur[q] ? ringN[ks[q]] : prevN[ks[q]];
                        float sh = cur[q] ? ringS[ks[q]] : prevS[ks[q]];
                        int argb = WaterRender.argb(0.93f, WaterRender.lerpColor(sh, DEEP, BELLY));
                        MeshDrawer.emit(body, pose, pp.x, pp.y, pp.z, argb, ks[q] / (float) SIDES, cur[q] ? vAcc : prevV,
                                nn.x, nn.y, nn.z, light, GodMesh.Material.SCALES);
                    }
                }
            }
            prevP = ringP;
            prevN = ringN;
            prevS = ringS;
            prevV = vAcc;
        }

        // ---- pinna dorsale continua (membrana)
        VertexConsumer fin = buffers.getBuffer(GodRenderTypes.water(false));
        int i0 = (int) (n * 0.05), i1 = (int) (n * 0.82);
        for (int i = i0; i < i1; i++) {
            for (int q = 0; q < 4; q++) {
                int idx = q == 0 || q == 3 ? i : i + 1;
                boolean tip = q >= 2;
                float h = rad[idx] * (0.85f + 0.3f * Mth.sin(idx * 0.9f - t * 0.2f));
                Vec3 b0 = c.get(idx).add(nor[idx].scale(rad[idx] * 1.08));
                Vec3 p = tip ? b0.add(nor[idx].scale(h)).add(tan[idx].scale(-h * 0.4)) : b0;
                p = p.subtract(base);
                MeshDrawer.emit(fin, pose, (float) p.x, (float) p.y, (float) p.z, WaterRender.argb(0.62f, tip ? BELLY : MID),
                        idx / (float) n * 6, tip ? 1 : 0, (float) bin[idx].x, (float) bin[idx].y, (float) bin[idx].z, light, GodMesh.Material.MEMBRANE);
            }
        }

        MeshDrawer.Tint tint = (s0, s1, y) -> WaterRender.argb(0.9f * fade, WaterRender.lerpColor(s0, DEEP, BELLY));
        // ---- spuntoni dorsali
        for (int i = i0; i < i1; i += 3) {
            place(ps, c.get(i).add(nor[i].scale(rad[i] * 1.02)).subtract(base), bin[i], nor[i], tan[i]);
            float sc = rad[i] * 0.85f;
            ps.scale(sc, sc, sc);
            MeshDrawer.draw(ps, buffers, fins, fins.part("dorsal_spike"), null, tint, light, false);
            ps.popPose();
        }
        // ---- pinne pettorali (due paia) che remano
        for (int pair = 0; pair < 2; pair++) {
            int i = Math.min(n - 1, (int) (n * (pair == 0 ? 0.07f : 0.42f)));
            float sc = (pair == 0 ? 1.15f : 0.7f) * fade;
            for (int side = -1; side <= 1; side += 2) {
                Vec3 at = c.get(i).add(bin[i].scale(side * rad[i] * 0.85)).add(nor[i].scale(-rad[i] * 0.25)).subtract(base);
                place(ps, at, bin[i], nor[i], tan[i]);
                if (side < 0) ps.scale(-1, 1, 1);
                ps.mulPose(Axis.ZP.rotationDegrees(-15 + Mth.sin(t * 0.25f + pair) * 22));
                ps.mulPose(Axis.YP.rotationDegrees(Mth.sin(t * 0.25f + pair + 1) * 10));
                ps.scale(sc, sc, sc);
                MeshDrawer.draw(ps, buffers, fins, fins.part("pectoral"), null, tint, light, false);
                MeshDrawer.draw(ps, buffers, fins, fins.part("pectoral_spines"), null, tint, light, false);
                ps.popPose();
            }
        }
        // ---- coda
        place(ps, c.get(n - 1).subtract(base), bin[n - 1], nor[n - 1], tan[n - 1]);
        ps.mulPose(Axis.YP.rotationDegrees(Mth.sin(t * 0.3f) * 25));
        ps.scale(fade, fade, fade);
        MeshDrawer.draw(ps, buffers, fins, fins.part("fluke"), null, tint, light, false);
        MeshDrawer.draw(ps, buffers, fins, fins.part("fluke_spines"), null, tint, light, false);
        ps.popPose();

        // ---- testa
        Vec3 fwd = e.getViewVector(pt);
        Vec3 upH = nor[0].subtract(fwd.scale(nor[0].dot(fwd)));
        upH = upH.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : upH.normalize();
        place(ps, Vec3.ZERO, upH.cross(fwd).normalize(), upH, fwd);
        float headScale = 1.35f * fade;
        ps.scale(headScale, headScale, headScale);
        if (headSkeleton == null || headSkeleton.mesh() != head) headSkeleton = new Skeleton(head);
        headSkeleton.reset();
        headSkeleton.rotate("jaw", Mth.lerp(pt, e.jawAnimPrev, e.jawAnim) * 0.8f, 0, 0);
        headSkeleton.compute();
        MeshDrawer.drawAll(ps, buffers, head, headSkeleton, (s0, s1, y) -> WaterRender.argb(0.95f * fade, WaterRender.lerpColor(s0, DEEP, MID)), light, false);
        float jaw = Mth.lerp(pt, e.jawAnimPrev, e.jawAnim);
        if (jaw > 0.3f) {
            VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
            ps.pushPose();
            ps.translate(0, -0.3f, 0.3f);
            ps.mulPose(Axis.YP.rotationDegrees(-90));
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(2.4f, 0.4f, 0.8f, 0.1f, 0.2f), WaterRender.argb(0.5f * jaw, CORE));
            ps.popPose();
        }
        ps.popPose();

        barbels(e, pt, ps, buffers, base, light);
        if (e.phase() == LeviathanEntity.PHASE_BREATH && t >= LeviathanEntity.BREATH_START + 14) breath(e, pt, ps, buffers, base, fwd, t);
    }

    private static float radius(float u) {
        return u < 0.06f ? Mth.lerp(u / 0.06f, 0.95f, 1.45f) : Mth.lerp((float) Math.pow((u - 0.06f) / 0.94f, 0.85), 1.45f, 0.14f);
    }

    /** Push + posiziona un sistema locale (X = lato, Y = su, Z = avanti). */
    private static void place(PoseStack ps, Vec3 at, Vec3 x, Vec3 y, Vec3 z) {
        ps.pushPose();
        ps.translate(at.x, at.y, at.z);
        Matrix3f m = new Matrix3f(new Vector3f((float) x.x, (float) x.y, (float) x.z),
                new Vector3f((float) y.x, (float) y.y, (float) y.z), new Vector3f((float) z.x, (float) z.y, (float) z.z));
        ps.mulPose(m.getNormalizedRotation(new Quaternionf()));
    }

    private void barbels(LeviathanEntity e, float pt, PoseStack ps, MultiBufferSource buffers, Vec3 base, int light) {
        VertexConsumer vc = buffers.getBuffer(GodRenderTypes.water(false));
        PoseStack.Pose pose = ps.last();
        for (int b = 0; b < 2; b++) {
            Vec3[] cur = e.barbels[b], old = e.barbelsPrev[b];
            if (cur == null) continue;
            for (int i = 0; i < cur.length - 1; i++) {
                Vec3 a = old[i].lerp(cur[i], pt).subtract(base), c = old[i + 1].lerp(cur[i + 1], pt).subtract(base);
                Vec3 d = c.subtract(a);
                if (d.lengthSqr() < 1e-8) continue;
                Vec3 dn = d.normalize();
                Vec3 ref = Math.abs(dn.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
                Vec3 s1 = dn.cross(ref).normalize(), s2 = dn.cross(s1).normalize();
                float r0 = 0.08f * (1 - i / (float) cur.length) + 0.01f, r1 = 0.08f * (1 - (i + 1) / (float) cur.length) + 0.01f;
                for (int k = 0; k < 5; k++) {
                    double a0 = Mth.TWO_PI * k / 5, a1 = Mth.TWO_PI * (k + 1) / 5;
                    Vec3[] quad = {
                            a.add(s1.scale(Math.cos(a0) * r0)).add(s2.scale(Math.sin(a0) * r0)),
                            a.add(s1.scale(Math.cos(a1) * r0)).add(s2.scale(Math.sin(a1) * r0)),
                            c.add(s1.scale(Math.cos(a1) * r1)).add(s2.scale(Math.sin(a1) * r1)),
                            c.add(s1.scale(Math.cos(a0) * r1)).add(s2.scale(Math.sin(a0) * r1))};
                    for (int q = 0; q < 4; q++) {
                        double aa = q == 0 || q == 3 ? a0 : a1;
                        Vec3 nn = s1.scale(Math.cos(aa)).add(s2.scale(Math.sin(aa)));
                        MeshDrawer.emit(vc, pose, (float) quad[q].x, (float) quad[q].y, (float) quad[q].z, WaterRender.argb(0.9f, BELLY),
                                k / 5f, i / (float) cur.length, (float) nn.x, (float) nn.y, (float) nn.z, light, GodMesh.Material.CRYSTAL);
                    }
                }
            }
        }
    }

    /** Il torrente d'acqua sparato dalle fauci. */
    private void breath(LeviathanEntity e, float pt, PoseStack ps, MultiBufferSource buffers, Vec3 base, Vec3 dir, float t) {
        Vec3 mouth = e.getPosition(pt).add(dir.scale(2.8));
        HitResult hit = e.level().clip(new ClipContext(mouth, mouth.add(dir.scale(48)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? mouth.add(dir.scale(48)) : hit.getLocation();
        float len = (float) end.distanceTo(mouth);
        float k = Mth.clamp((t - LeviathanEntity.BREATH_START - 14) / 5f, 0, 1) * Mth.clamp((LeviathanEntity.DIVE_START - t) / 4f, 0, 1);
        ps.pushPose();
        Vec3 m = mouth.subtract(base);
        ps.translate(m.x, m.y, m.z);
        WaterRender.orient(ps, dir.x, dir.y, dir.z);
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        WaterRender.tube(water, ps.last(), 0, len, 0.75f * k, 14, t * 0.3f, WaterRender.flow(), WaterRender.argb(0.7f, 0x2A9BF0), WaterRender.FULL_BRIGHT);
        WaterRender.tube(water, ps.last(), 0, len, 0.45f * k, 10, -t * 0.4f, WaterRender.flow(), WaterRender.argb(0.6f, 0x9FE8FF), WaterRender.FULL_BRIGHT);
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        WaterRender.glowTube(glow, ps.last().pose(), 0, len, 0.22f * k, 8, WaterRender.argb(0.9f, 0xD8FBFF));
        ps.popPose();
    }

    /** Spline di Catmull-Rom (uniforme) con {@code sub} suddivisioni per segmento. */
    private static List<Vec3> catmullRom(List<Vec3> p, int sub) {
        List<Vec3> out = new ArrayList<>();
        int n = p.size();
        for (int i = 0; i < n - 1; i++) {
            Vec3 p0 = p.get(Math.max(i - 1, 0)), p1 = p.get(i), p2 = p.get(i + 1), p3 = p.get(Math.min(i + 2, n - 1));
            for (int s = 0; s < sub; s++) {
                double t = s / (double) sub, t2 = t * t, t3 = t2 * t;
                out.add(p0.scale(-0.5 * t3 + t2 - 0.5 * t)
                        .add(p1.scale(1.5 * t3 - 2.5 * t2 + 1))
                        .add(p2.scale(-1.5 * t3 + 2 * t2 + 0.5 * t))
                        .add(p3.scale(0.5 * t3 - 0.5 * t2)));
            }
        }
        out.add(p.get(n - 1));
        return out;
    }
}
