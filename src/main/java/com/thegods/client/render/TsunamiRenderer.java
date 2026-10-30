package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.TsunamiEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Lo tsunami: una superficie d'acqua con il profilo di un'onda che si arriccia,
 * con ondulazioni lungo la cresta e schiuma bianca sul labbro.
 */
public class TsunamiRenderer extends SpellRenderer<TsunamiEntity> {
    /** Profilo dell'onda (z avanti, y su), normalizzato sull'altezza. Dal dorso al ricciolo. */
    private static final float[][] PROFILE = {
            {-2.4f, 0.00f}, {-1.9f, 0.18f}, {-1.4f, 0.40f}, {-0.9f, 0.64f}, {-0.45f, 0.84f},
            {0.0f, 0.97f}, {0.45f, 1.04f}, {0.85f, 1.00f}, {1.15f, 0.88f}, {1.30f, 0.72f},
            {1.25f, 0.58f}, {1.05f, 0.50f}, {0.80f, 0.50f}
    };

    public TsunamiRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TsunamiEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float h = e.currentHeight(pt);
        if (h < 0.05f) return;
        float width = e.getSize();
        float t = e.tickCount + pt;
        int lit = LightTexture.pack(Math.max(LightTexture.block(light), 12), Math.max(LightTexture.sky(light), 12));

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));

        int nx = Math.max(16, (int) (width * 1.6f));
        int np = PROFILE.length;
        Vector3f[][] pts = new Vector3f[nx + 1][np];
        float depth = Math.min(h * 0.55f, 5f);
        for (int i = 0; i <= nx; i++) {
            float u = (float) i / nx;
            float x = (u - 0.5f) * width;
            float edge = (float) Math.pow(Mth.sin(Mth.PI * u), 0.55);
            for (int j = 0; j < np; j++) {
                float pz = PROFILE[j][0], py = PROFILE[j][1];
                float curl = j > 6 ? edge : 1;
                float y = py * h * edge + Mth.sin(x * 0.5f + t * 0.25f + j * 0.3f) * 0.25f * edge;
                float z = pz * depth * curl + Mth.sin(x * 0.33f - t * 0.2f) * 0.45f * edge;
                pts[i][j] = new Vector3f(x, Math.max(0, y), z);
            }
        }

        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer vc = buffers.getBuffer(WaterRender.water());
        for (int j = 0; j < np - 1; j++) {
            float rowT = (float) j / (np - 2);
            int rgb = rowT < 0.55f ? WaterRender.lerpColor(rowT / 0.55f, 0x1C63C4, 0x3DB4F5)
                    : WaterRender.lerpColor((rowT - 0.55f) / 0.45f, 0x3DB4F5, 0xF0FCFF);
            float alpha = rowT > 0.75f ? 0.9f : 0.8f;
            int argb = WaterRender.argb(alpha, rgb);
            for (int i = 0; i < nx; i++) {
                float u0 = (i % 4) / 4f, u1 = (i % 4 + 1) / 4f;
                float v0 = (j % 3) / 3f, v1 = (j % 3 + 1) / 3f;
                WaterRender.quadLit(vc, ps.last(), pts[i][j], pts[i + 1][j], pts[i + 1][j + 1], pts[i][j + 1], s, u0, v0, u1, v1, argb, lit);
            }
        }
        ps.popPose();
    }
}
