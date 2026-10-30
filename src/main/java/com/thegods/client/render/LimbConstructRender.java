package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.god.Limb;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Geometria degli arti di acqua solida, nello spazio della ModelPart del giocatore
 * (origine sul perno della spalla/anca, Y verso il basso, fronte verso -Z).
 * Usata sia in terza persona (layer) sia in prima persona (braccio).
 */
public final class LimbConstructRender {
    private static final int DEEP = 0x1560C8;
    private static final int MID = 0x2FB8FF;
    private static final int FOAM = 0xC8F6FF;
    private static final int CORE = 0x8FF0FF;

    /**
     * @param form  0..1 animazione di formazione (cresce dalla spalla verso la mano)
     * @param slim  braccia sottili (skin Alex)
     */
    public static void render(PoseStack ps, MultiBufferSource buffers, Limb limb, float form, float t, boolean slim) {
        if (form <= 0.01f) return;
        float grow = 1 - (1 - form) * (1 - form);
        // la parte "esterna" dell'arto (lontano dal corpo): -X per i destri, +X per i sinistri
        int out = (limb == Limb.RIGHT_ARM || limb == Limb.RIGHT_LEG) ? -1 : 1;
        float xc = limb.isArm() ? out * (slim ? 0.03f : 0.0625f) : 0f;

        ps.pushPose();
        ps.translate(xc, 0, 0);
        ps.scale(grow, grow, grow);
        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        if (limb.isArm()) arm(ps, water, null, s, out, t);
        else leg(ps, water, null, s, out, t);
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        if (limb.isArm()) arm(ps, null, glow, s, out, t);
        else leg(ps, null, glow, s, out, t);
        if (form < 1) formingSpiral(ps, glow, form, t, limb.isArm() ? 0.75f : 0.78f);
        ps.popPose();
    }

    private static void arm(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, int out, float t) {
        float pulse = 0.5f + 0.5f * Mth.sin(t * 0.2f);
        // spallaccio
        if (water != null) {
            solid(water, ps, s, WaterRender.box(-0.17f, -0.17f, -0.16f, 0.17f, 0.08f, 0.16f), 0.82f, MID);
            for (int i = 0; i < 3; i++) {
                ps.pushPose();
                ps.translate(out * 0.15f, -0.12f + i * 0.08f, -0.05f + i * 0.05f);
                ps.mulPose(Axis.ZP.rotationDegrees(out < 0 ? 180 + 25 - i * 12 : -25 + i * 12));
                solid(water, ps, s, WaterRender.taper(0.22f - i * 0.04f, 0.07f, 0.05f, 0.005f, 0.005f), 0.85f, FOAM);
                ps.popPose();
            }
        }
        segment(ps, water, glow, s, -0.06f, 0.3f, 0.26f, 0.22f, 0.25f, 0.21f, DEEP, MID, pulse);
        // gomito
        if (water != null) solid(water, ps, s, WaterRender.box(-0.14f, 0.27f, -0.14f, 0.14f, 0.34f, 0.14f), 0.85f, FOAM);
        segment(ps, water, glow, s, 0.32f, 0.58f, 0.22f, 0.28f, 0.21f, 0.27f, MID, MID, pulse);
        // lama sull'avambraccio
        if (water != null) {
            ps.pushPose();
            ps.translate(out * 0.13f, 0.36f, 0.04f);
            ps.mulPose(Axis.ZP.rotationDegrees(90 + out * 18));
            solid(water, ps, s, WaterRender.taper(0.34f, 0.04f, 0.13f, 0.005f, 0.02f), 0.8f, FOAM);
            ps.popPose();
        }
        // pugno
        if (water != null) solid(water, ps, s, WaterRender.box(-0.16f, 0.56f, -0.16f, 0.16f, 0.76f, 0.16f), 0.86f, MID);
        if (glow != null) {
            Matrix4f m = ps.last().pose();
            WaterRender.glowSolid(glow, m, WaterRender.box(-0.05f, 0.6f, -0.17f, 0.05f, 0.72f, -0.165f), WaterRender.argb(0.6f + 0.3f * pulse, CORE));
            WaterRender.glowSolid(glow, m, WaterRender.box(-0.07f, -0.1f, -0.07f, 0.07f, 0.04f, 0.07f), WaterRender.argb(0.35f + 0.3f * pulse, CORE));
        }
    }

    private static void leg(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, int out, float t) {
        float pulse = 0.5f + 0.5f * Mth.sin(t * 0.2f + 1.3f);
        segment(ps, water, glow, s, -0.02f, 0.36f, 0.29f, 0.24f, 0.29f, 0.24f, DEEP, MID, pulse);
        // ginocchio con sperone
        if (water != null) {
            solid(water, ps, s, WaterRender.box(-0.15f, 0.33f, -0.15f, 0.15f, 0.41f, 0.15f), 0.85f, FOAM);
            ps.pushPose();
            ps.translate(0, 0.37f, -0.14f);
            ps.mulPose(Axis.YP.rotationDegrees(90));
            ps.mulPose(Axis.ZP.rotationDegrees(-10));
            solid(water, ps, s, WaterRender.taper(0.18f, 0.08f, 0.06f, 0.005f, 0.005f), 0.85f, FOAM);
            ps.popPose();
        }
        segment(ps, water, glow, s, 0.39f, 0.66f, 0.24f, 0.2f, 0.24f, 0.2f, MID, MID, pulse);
        if (water != null) {
            // piede allungato con pinne alla caviglia
            solid(water, ps, s, WaterRender.box(-0.14f, 0.64f, -0.24f, 0.14f, 0.76f, 0.14f), 0.86f, MID);
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * 0.13f, 0.62f, 0.08f);
                ps.mulPose(Axis.YP.rotationDegrees(-90 + side * 30));
                ps.mulPose(Axis.ZP.rotationDegrees(15));
                solid(water, ps, s, WaterRender.taper(0.24f, 0.12f, 0.03f, 0.01f, 0.01f), 0.75f, FOAM);
                ps.popPose();
            }
        }
    }

    /** Un segmento verticale rastremato (da y0 a y1) con vena luminosa. */
    private static void segment(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s,
                                float y0, float y1, float w0, float w1, float d0, float d1, int c0, int c1, float pulse) {
        ps.pushPose();
        ps.translate(0, y0, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(90)); // +X locale -> +Y (giù lungo l'arto)
        float len = y1 - y0;
        if (water != null) {
            solid(water, ps, s, WaterRender.taper(len, w0, d0, w1, d1), 0.78f, WaterRender.lerpColor(0.5f, c0, c1));
        }
        if (glow != null) {
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(len, w0 * 0.22f, d0 * 0.22f, w1 * 0.22f, d1 * 0.22f),
                    WaterRender.argb(0.35f + 0.35f * pulse, CORE));
        }
        ps.popPose();
    }

    private static void solid(VertexConsumer vc, PoseStack ps, TextureAtlasSprite s, Vector3f[] corners, float alpha, int rgb) {
        WaterRender.solid(vc, ps.last(), corners, s, WaterRender.argb(alpha, rgb), WaterRender.FULL_BRIGHT);
    }

    /** Spirale luminosa che "disegna" l'arto mentre si forma. */
    private static void formingSpiral(PoseStack ps, VertexConsumer glow, float form, float t, float length) {
        Matrix4f m = ps.last().pose();
        int steps = 40;
        float head = form * length;
        for (int i = 0; i < steps; i++) {
            float y0 = length * i / steps, y1 = length * (i + 1) / steps;
            if (y1 > head + 0.15f) break;
            float a0 = y0 * 30 - t * 0.6f, a1 = y1 * 30 - t * 0.6f, r = 0.24f;
            float alpha = (1 - form) * 0.9f;
            WaterRender.glowQuad(glow, m,
                    new Vector3f(Mth.cos(a0) * r, y0, Mth.sin(a0) * r), new Vector3f(Mth.cos(a1) * r, y1, Mth.sin(a1) * r),
                    new Vector3f(Mth.cos(a1) * r, y1 + 0.03f, Mth.sin(a1) * r), new Vector3f(Mth.cos(a0) * r, y0 + 0.03f, Mth.sin(a0) * r),
                    WaterRender.argb(alpha, FOAM));
        }
    }

    private LimbConstructRender() {}
}
