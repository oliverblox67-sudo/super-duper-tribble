package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.WaterPrisonEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Una bolla d'acqua tremolante attorno al prigioniero, con anelli di corrente. */
public class WaterPrisonRenderer extends SpellRenderer<WaterPrisonEntity> {
    public WaterPrisonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WaterPrisonEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float k = Mth.clamp(t / 6f, 0, 1) * Mth.clamp((WaterPrisonEntity.LIFETIME - t) / 6f, 0, 1);
        if (k <= 0.01f) return;
        LivingEntity target = e.getTarget();
        Vec3 base = e.getPosition(pt);
        Vec3 c = target != null ? target.getPosition(pt).add(0, target.getBbHeight() / 2, 0) : base;

        ps.pushPose();
        ps.translate(c.x - base.x, c.y - base.y, c.z - base.z);
        float r = e.getSize() * (0.6f + 0.4f * k);
        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer vc = buffers.getBuffer(WaterRender.water());
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(t * 2f));
        WaterRender.sphere(vc, ps.last(), r, 14, 22, t, 0.05f, s, WaterRender.argb(0.5f, 0x2A8FE8), WaterRender.FULL_BRIGHT);
        WaterRender.sphere(vc, ps.last(), r * 0.93f, 10, 16, -t, 0.04f, s, WaterRender.argb(0.25f, 0x9FE8FF), WaterRender.FULL_BRIGHT);
        ps.popPose();

        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(t * (3 + i) + i * 60));
            ps.mulPose(Axis.XP.rotationDegrees(60 * i + 20));
            Matrix4f m = ps.last().pose();
            float r0 = r * 1.04f, r1 = r * 1.09f;
            int segs = 36;
            for (int j = 0; j < segs; j++) {
                float a0 = Mth.TWO_PI * j / segs, a1 = Mth.TWO_PI * (j + 1) / segs;
                float a = 0.25f + 0.35f * Mth.sin(a0 * 2 + t * 0.3f);
                WaterRender.glowQuad(glow, m,
                        new Vector3f(Mth.cos(a0) * r0, 0, Mth.sin(a0) * r0), new Vector3f(Mth.cos(a1) * r0, 0, Mth.sin(a1) * r0),
                        new Vector3f(Mth.cos(a1) * r1, 0, Mth.sin(a1) * r1), new Vector3f(Mth.cos(a0) * r1, 0, Mth.sin(a0) * r1),
                        WaterRender.argb(a * k, 0x8FEFFF));
            }
            ps.popPose();
        }
        ps.popPose();
    }
}
