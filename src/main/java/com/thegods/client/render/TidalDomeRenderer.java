package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thegods.entity.TidalDomeEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** La Cupola delle Maree: sfera d'acqua corrente con anelli luminosi che salgono. */
public class TidalDomeRenderer extends SpellRenderer<TidalDomeEntity> {
    public TidalDomeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TidalDomeEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float k = Mth.clamp(t / 8f, 0, 1) * Mth.clamp((TidalDomeEntity.LIFETIME - t) / 8f, 0, 1);
        if (k <= 0.01f) return;
        float r = e.getSize() * (1 - (1 - k) * (1 - k));
        Entity owner = e.getOwner();
        Vec3 base = e.getPosition(pt);
        Vec3 c = owner != null ? owner.getPosition(pt) : base;
        ps.pushPose();
        ps.translate(c.x - base.x, c.y - base.y + 1, c.z - base.z);

        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        ps.pushPose();
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(t * 1.5f));
        WaterRender.sphere(water, ps.last(), r, 16, 28, t, 0.02f, WaterRender.flow(), WaterRender.argb(0.26f, 0x3FB8F5), WaterRender.FULL_BRIGHT);
        ps.popPose();

        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < 4; i++) {
            float h = ((t * 0.04f + i / 4f) % 1f) * 2 - 1; // da -1 a 1
            float y = h * r, rr = r * Mth.sqrt(Math.max(0, 1 - h * h)) * 1.01f;
            float a = 0.5f * (1 - Math.abs(h)) * k;
            ring(glow, m, y, rr, 0.08f, WaterRender.argb(a, 0x9FF0FF));
        }
        ring(glow, m, -0.95f, r * 0.98f, 0.15f, WaterRender.argb(0.5f * k, 0xBFF6FF));
        ps.popPose();
    }

    private static void ring(VertexConsumer vc, Matrix4f m, float y, float r, float w, int argb) {
        int segs = 48;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            WaterRender.glowQuad(vc, m,
                    new Vector3f(Mth.cos(a0) * r, y, Mth.sin(a0) * r), new Vector3f(Mth.cos(a1) * r, y, Mth.sin(a1) * r),
                    new Vector3f(Mth.cos(a1) * r, y + w, Mth.sin(a1) * r), new Vector3f(Mth.cos(a0) * r, y + w, Mth.sin(a0) * r), argb);
        }
    }
}
