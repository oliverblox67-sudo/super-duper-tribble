package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thegods.entity.MaelstromEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Il maelstrom: un disco a spirale che ruota più veloce al centro e una tromba d'acqua attorcigliata. */
public class MaelstromRenderer extends SpellRenderer<MaelstromEntity> {
    public MaelstromRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MaelstromEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float k = e.intensity(pt) * Mth.clamp((MaelstromEntity.LIFETIME - t) / 10f, 0, 1);
        if (k <= 0.01f) return;
        float radius = e.getSize() * k;
        TextureAtlasSprite s = WaterRender.still();
        TextureAtlasSprite flow = WaterRender.flow();
        VertexConsumer vc = buffers.getBuffer(WaterRender.water());

        // --- Disco a spirale
        int rings = Math.max(6, (int) (radius * 1.5f));
        int segs = 48;
        for (int r = 0; r < rings; r++) {
            float r0 = radius * r / rings, r1 = radius * (r + 1) / rings;
            float spin = t * 0.5f / (1 + r0 * 0.35f);
            float y0 = 0.06f + 0.01f * r, y1 = 0.06f + 0.01f * (r + 1);
            for (int i = 0; i < segs; i++) {
                float a0 = Mth.TWO_PI * i / segs + spin, a1 = Mth.TWO_PI * (i + 1) / segs + spin;
                float arm = 0.5f + 0.5f * Mth.sin(3 * a0 - r0 * 0.9f);
                float alpha = (0.35f + 0.5f * arm) * (1 - 0.4f * r0 / Math.max(radius, 0.01f));
                int rgb = WaterRender.lerpColor(arm, 0x0A3A80, 0x5FD0FF);
                WaterRender.quadLit(vc, ps.last(),
                        new Vector3f(Mth.cos(a0) * r0, y0, Mth.sin(a0) * r0), new Vector3f(Mth.cos(a1) * r0, y0, Mth.sin(a1) * r0),
                        new Vector3f(Mth.cos(a1) * r1, y1, Mth.sin(a1) * r1), new Vector3f(Mth.cos(a0) * r1, y1, Mth.sin(a0) * r1),
                        s, 0, 0, 1, 1, WaterRender.argb(alpha * k, rgb), WaterRender.FULL_BRIGHT);
            }
        }

        // --- Tromba d'acqua
        float height = MaelstromEntity.COLUMN_HEIGHT * k;
        int levels = 28, sides = 18;
        Vector3f[][] col = new Vector3f[levels + 1][sides + 1];
        for (int l = 0; l <= levels; l++) {
            float y = height * l / levels;
            float rr = (0.9f + y * 0.17f + Mth.sin(y * 0.8f - t * 0.5f) * 0.18f) * k;
            float twist = y * 0.35f - t * 0.45f;
            float sway = Mth.sin(t * 0.05f + y * 0.15f) * y * 0.04f;
            for (int i = 0; i <= sides; i++) {
                float a = Mth.TWO_PI * i / sides + twist;
                col[l][i] = new Vector3f(Mth.cos(a) * rr + sway, y, Mth.sin(a) * rr);
            }
        }
        for (int l = 0; l < levels; l++) {
            float f = (float) l / levels;
            int argb = WaterRender.argb(0.62f, WaterRender.lerpColor(f, 0x1560C8, 0xCFF6FF));
            for (int i = 0; i < sides; i++) {
                WaterRender.quadLit(vc, ps.last(), col[l][i], col[l][i + 1], col[l + 1][i + 1], col[l + 1][i], flow,
                        0, 0, 1, 1, argb, WaterRender.FULL_BRIGHT);
            }
        }

        // --- Linee di energia luminosa attorcigliate nella colonna
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        Matrix4f m = ps.last().pose();
        for (int strand = 0; strand < 3; strand++) {
            for (int l = 0; l < levels; l++) {
                float ya = height * l / levels, yb = height * (l + 1) / levels;
                float aa = ya * 0.6f - t * 0.7f + strand * Mth.TWO_PI / 3, ab = yb * 0.6f - t * 0.7f + strand * Mth.TWO_PI / 3;
                float ra = (0.5f + ya * 0.1f) * k, rb = (0.5f + yb * 0.1f) * k;
                float w = 0.06f;
                WaterRender.glowQuad(glow, m,
                        new Vector3f(Mth.cos(aa) * ra - w, ya, Mth.sin(aa) * ra), new Vector3f(Mth.cos(aa) * ra + w, ya, Mth.sin(aa) * ra),
                        new Vector3f(Mth.cos(ab) * rb + w, yb, Mth.sin(ab) * rb), new Vector3f(Mth.cos(ab) * rb - w, yb, Mth.sin(ab) * rb),
                        WaterRender.argb(0.45f, 0x8FEFFF));
            }
        }
    }
}
