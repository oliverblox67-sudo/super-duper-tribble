package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.WaterTridentEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/** Un tridente d'acqua lungo 5 blocchi, con nucleo luminoso e anelli di corrente. */
public class WaterTridentRenderer extends SpellRenderer<WaterTridentEntity> {
    public WaterTridentRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WaterTridentEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float appear = Mth.clamp(t / 3f, 0, 1);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.yRotO, e.getYRot()) - 90f));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        ps.mulPose(Axis.XP.rotationDegrees(t * 20f));
        ps.scale(appear, appear, appear);

        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        int shaft = WaterRender.argb(0.8f, 0x1F7FE0);
        int prong = WaterRender.argb(0.85f, 0x5FD0FF);
        // asta
        ps.pushPose();
        ps.translate(-2.8f, 0, 0);
        WaterRender.solid(water, ps.last(), WaterRender.taper(3.5f, 0.1f, 0.1f, 0.2f, 0.2f), s, shaft, WaterRender.FULL_BRIGHT);
        ps.popPose();
        // traversa
        WaterRender.solid(water, ps.last(), WaterRender.box(0.6f, -0.7f, -0.12f, 0.85f, 0.7f, 0.12f), s, prong, WaterRender.FULL_BRIGHT);
        // punta centrale
        ps.pushPose();
        ps.translate(0.8f, 0, 0);
        WaterRender.solid(water, ps.last(), WaterRender.taper(1.9f, 0.26f, 0.2f, 0.01f, 0.01f), s, prong, WaterRender.FULL_BRIGHT);
        ps.popPose();
        // punte laterali con uncini
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(0.7f, side * 0.6f, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(side * 6f));
            WaterRender.solid(water, ps.last(), WaterRender.taper(1.4f, 0.2f, 0.16f, 0.01f, 0.01f), s, prong, WaterRender.FULL_BRIGHT);
            ps.translate(0.9f, 0, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(side * 150f));
            WaterRender.solid(water, ps.last(), WaterRender.taper(0.35f, 0.1f, 0.08f, 0.01f, 0.01f), s, prong, WaterRender.FULL_BRIGHT);
            ps.popPose();
        }
        // anelli di corrente attorno all'asta
        for (int r = 0; r < 3; r++) {
            float x = -2.2f + r * 0.9f + (t * 0.15f % 0.9f);
            ringX(water, ps, s, x, 0.32f + r * 0.03f, WaterRender.argb(0.5f, 0xA0EEFF));
        }

        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        int core = WaterRender.argb(0.7f, 0x9FF2FF);
        ps.pushPose();
        ps.translate(-2.8f, 0, 0);
        WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(3.6f, 0.04f, 0.04f, 0.07f, 0.07f), core);
        ps.popPose();
        ps.pushPose();
        ps.translate(0.8f, 0, 0);
        WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(1.8f, 0.1f, 0.08f, 0.0f, 0.0f), core);
        ps.popPose();
        ps.popPose();
    }

    private static void ringX(VertexConsumer vc, PoseStack ps, TextureAtlasSprite s, float x, float r, int argb) {
        int segs = 16;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            WaterRender.quad(vc, ps.last(),
                    new Vector3f(x, Mth.cos(a0) * r, Mth.sin(a0) * r), new Vector3f(x, Mth.cos(a1) * r, Mth.sin(a1) * r),
                    new Vector3f(x + 0.12f, Mth.cos(a1) * r * 0.8f, Mth.sin(a1) * r * 0.8f), new Vector3f(x + 0.12f, Mth.cos(a0) * r * 0.8f, Mth.sin(a0) * r * 0.8f),
                    s, 0, 0, 1, 1, argb, WaterRender.FULL_BRIGHT);
        }
    }
}
