package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.HydroBeamEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Getto Abissale: tubo d'acqua corrente, nucleo bianco-azzurro e spirale. */
public class HydroBeamRenderer extends SpellRenderer<HydroBeamEntity> {
    public HydroBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(HydroBeamEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        Entity owner = e.getOwner();
        if (!(owner instanceof LivingEntity caster)) return;
        float t = e.tickCount + pt;
        float k = Mth.clamp(t / 5f, 0, 1) * Mth.clamp((HydroBeamEntity.LIFETIME - t) / 6f, 0, 1);
        if (k <= 0) return;

        Vec3 eye = caster.getEyePosition(pt);
        Vec3 dir = caster.getViewVector(pt);
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 start = eye.add(right.scale(0.35)).add(0, -0.35, 0).add(dir.scale(0.6));
        HitResult hit = e.level().clip(new ClipContext(eye, eye.add(dir.scale(HydroBeamEntity.RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(dir.scale(HydroBeamEntity.RANGE)) : hit.getLocation();
        Vec3 seg = end.subtract(start);
        float len = (float) seg.length();
        if (len < 0.1f) return;
        Vec3 n = seg.scale(1 / len);

        Vec3 base = e.getPosition(pt);
        ps.pushPose();
        ps.translate(start.x - base.x, start.y - base.y, start.z - base.z);
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(n.x, n.z)));
        ps.mulPose(Axis.XP.rotation((float) -Math.asin(Mth.clamp(n.y, -1, 1))));

        TextureAtlasSprite flow = WaterRender.flow();
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        WaterRender.tube(water, ps.last(), 0, len, 0.42f * k, 14, t * 0.3f, flow, WaterRender.argb(0.7f, 0x2A9BF0), WaterRender.FULL_BRIGHT);
        WaterRender.tube(water, ps.last(), 0, len, 0.26f * k, 10, -t * 0.4f, flow, WaterRender.argb(0.6f, 0x8FE6FF), WaterRender.FULL_BRIGHT);
        // spirale
        Vector3f pa = null, pb = null;
        int steps = Math.max(8, (int) (len * 4));
        for (int i = 0; i <= steps; i++) {
            float z = len * i / steps;
            float a = z * 1.6f - t * 0.6f;
            float r = 0.62f * k;
            Vector3f c = new Vector3f(Mth.cos(a) * r, Mth.sin(a) * r, z);
            Vector3f a1 = new Vector3f(c).mul(0.86f, 0.86f, 1), b1 = new Vector3f(c).mul(1.14f, 1.14f, 1);
            if (pa != null) WaterRender.quad(water, ps.last(), pa, a1, b1, pb, flow, 0, 0, 1, 1, WaterRender.argb(0.55f, 0xBDF4FF), WaterRender.FULL_BRIGHT);
            pa = a1;
            pb = b1;
        }

        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        Matrix4f m = ps.last().pose();
        WaterRender.glowTube(glow, m, 0, len, 0.13f * k, 8, WaterRender.argb(0.9f, 0xD8FBFF));
        WaterRender.glowTube(glow, m, 0, len, 0.22f * k, 8, WaterRender.argb(0.25f, 0x5FD0FF));
        // bagliore all'impatto
        ps.translate(0, 0, len);
        for (int i = 0; i < 4; i++) {
            ps.mulPose(Axis.ZP.rotationDegrees(45f + t * 3f));
            float s = (1.2f + 0.3f * Mth.sin(t * 0.8f + i)) * k;
            Matrix4f mm = ps.last().pose();
            WaterRender.glowQuad(glow, mm, new Vector3f(-s, -0.08f, 0), new Vector3f(s, -0.08f, 0),
                    new Vector3f(s, 0.08f, 0), new Vector3f(-s, 0.08f, 0), WaterRender.argb(0.5f, 0xAFF0FF));
        }
        ps.popPose();
    }
}
