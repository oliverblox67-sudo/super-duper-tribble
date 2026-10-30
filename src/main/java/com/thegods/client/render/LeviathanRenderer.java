package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.LeviathanEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Il Leviatano: corpo a tronchi di cono lungo la scia, pinne dorsali, testa con fauci che si aprono. */
public class LeviathanRenderer extends SpellRenderer<LeviathanEntity> {
    private static final int DEEP = 0x0E47A8, MID = 0x2FA8F0, FOAM = 0xC8F6FF, CORE = 0x8FF0FF;

    public LeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(LeviathanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        Vec3 base = e.getPosition(pt);
        List<Vec3> pts = new ArrayList<>();
        pts.add(base);
        for (Vec3 p : e.trail) {
            if (p.distanceToSqr(pts.get(pts.size() - 1)) > 0.04) pts.add(p);
        }
        if (pts.size() < 3) return;
        float t = e.tickCount + pt;
        float fade = Mth.clamp((LeviathanEntity.LIFETIME - t) / 10f, 0, 1);
        int n = pts.size();
        TextureAtlasSprite s = WaterRender.still();

        for (int pass = 0; pass < 2; pass++) {
            VertexConsumer vc = buffers.getBuffer(pass == 0 ? WaterRender.water() : WaterRender.glow());
            for (int i = 0; i < n - 1; i++) {
                Vec3 a = pts.get(i), b = pts.get(i + 1);
                Vec3 d = b.subtract(a);
                double len = d.length();
                if (len < 1e-3) continue;
                float r0 = radius(i / (float) (n - 1)) * fade, r1 = radius((i + 1) / (float) (n - 1)) * fade;
                ps.pushPose();
                ps.translate(a.x - base.x, a.y - base.y, a.z - base.z);
                WaterRender.orient(ps, d.x / len, d.y / len, d.z / len);
                if (pass == 0) {
                    int rgb = WaterRender.lerpColor(0.5f + 0.5f * Mth.sin(i * 0.6f - t * 0.4f), DEEP, MID);
                    WaterRender.frustum(vc, ps.last(), (float) len * 1.08f, r0, r1, 12, i * 0.3f, s, WaterRender.argb(0.78f, rgb), WaterRender.FULL_BRIGHT);
                    if (i % 3 == 1 && i < n - 4) {
                        // pinna dorsale
                        ps.pushPose();
                        ps.translate(0, r0 * 0.8f, 0);
                        ps.mulPose(Axis.ZP.rotationDegrees(90));
                        ps.mulPose(Axis.YP.rotationDegrees(35));
                        WaterRender.solid(vc, ps.last(), WaterRender.taper(r0 * 1.3f, r0 * 0.9f, 0.08f, 0.02f, 0.02f), s,
                                WaterRender.argb(0.85f, FOAM), WaterRender.FULL_BRIGHT);
                        ps.popPose();
                    }
                } else {
                    WaterRender.glowTube(vc, ps.last().pose(), 0, (float) len, Math.max(0.05f, r0 * 0.18f), 6,
                            WaterRender.argb(0.35f + 0.25f * Mth.sin(t * 0.3f - i * 0.5f), CORE));
                }
                ps.popPose();
            }
            head(ps, vc, s, pts.get(0).subtract(pts.get(1)).normalize(), t, fade, pass == 1);
        }
    }

    private static float radius(float u) {
        return u < 0.08f ? Mth.lerp(u / 0.08f, 0.85f, 1.15f) : Mth.lerp((float) Math.pow((u - 0.08f) / 0.92f, 0.8), 1.15f, 0.12f);
    }

    private void head(PoseStack ps, VertexConsumer vc, TextureAtlasSprite s, Vec3 dir, float t, float k, boolean glow) {
        ps.pushPose();
        WaterRender.orient(ps, dir.x, dir.y, dir.z);
        ps.scale(k, k, k);
        ps.mulPose(Axis.YP.rotationDegrees(-90)); // da qui: +X = avanti, Y = su, Z = lato
        float jaw = 18 + 14 * Mth.sin(t * 0.35f);
        if (!glow) {
            // cranio
            ps.pushPose();
            ps.translate(-0.4f, 0.15f, 0);
            WaterRender.solid(vc, ps.last(), WaterRender.taper(2.6f, 1.25f, 1.6f, 0.35f, 0.55f), s, WaterRender.argb(0.85f, MID), WaterRender.FULL_BRIGHT);
            ps.popPose();
            // mandibola
            ps.pushPose();
            ps.translate(-0.3f, -0.35f, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-jaw));
            WaterRender.solid(vc, ps.last(), WaterRender.taper(2.3f, 0.45f, 1.35f, 0.15f, 0.4f), s, WaterRender.argb(0.85f, DEEP), WaterRender.FULL_BRIGHT);
            ps.popPose();
            // corna all'indietro
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(0.2f, 0.6f, side * 0.55f);
                ps.mulPose(Axis.YP.rotationDegrees(side * -25));
                ps.mulPose(Axis.ZP.rotationDegrees(160));
                WaterRender.solid(vc, ps.last(), WaterRender.taper(1.9f, 0.32f, 0.28f, 0.02f, 0.02f), s, WaterRender.argb(0.9f, FOAM), WaterRender.FULL_BRIGHT);
                ps.popPose();
            }
            // barbigli
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(1.6f, -0.1f, side * 0.35f);
                ps.mulPose(Axis.YP.rotationDegrees(side * -40 + Mth.sin(t * 0.2f + side) * 10));
                ps.mulPose(Axis.ZP.rotationDegrees(170));
                WaterRender.solid(vc, ps.last(), WaterRender.taper(1.4f, 0.08f, 0.08f, 0.01f, 0.01f), s, WaterRender.argb(0.8f, FOAM), WaterRender.FULL_BRIGHT);
                ps.popPose();
            }
        } else {
            for (int side = -1; side <= 1; side += 2) {
                WaterRender.glowSolid(vc, ps.last().pose(), WaterRender.box(0.5f, 0.35f, side * 0.62f - 0.14f, 0.85f, 0.55f, side * 0.62f + 0.14f),
                        WaterRender.argb(0.95f, 0xE6FFFF));
            }
            // bagliore nelle fauci
            ps.pushPose();
            ps.translate(0.2f, -0.3f, 0);
            WaterRender.glowSolid(vc, ps.last().pose(), WaterRender.taper(1.6f, 0.3f, 0.6f, 0.05f, 0.1f), WaterRender.argb(0.45f, CORE));
            ps.popPose();
        }
        ps.popPose();
    }
}
