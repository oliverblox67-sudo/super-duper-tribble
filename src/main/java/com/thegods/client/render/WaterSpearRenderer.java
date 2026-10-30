package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.WaterSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/** Lancia di cristallo d'acqua: asta, guardia e punta a diamante con nucleo luminoso. */
public class WaterSpearRenderer extends SpellRenderer<WaterSpearEntity> {
    public WaterSpearRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WaterSpearEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float form = Mth.clamp(t / 8f, 0, 1);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.rotLerp(pt, e.yRotO, e.getYRot()) - 90f));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        ps.mulPose(Axis.XP.rotationDegrees(t * 15f));
        ps.scale(form, form, form);
        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        ps.pushPose();
        ps.translate(-1.5f, 0, 0);
        WaterRender.solid(water, ps.last(), WaterRender.taper(1.9f, 0.08f, 0.08f, 0.13f, 0.13f), s, WaterRender.argb(0.8f, 0x2A8FE0), WaterRender.FULL_BRIGHT);
        ps.popPose();
        WaterRender.solid(water, ps.last(), WaterRender.box(0.35f, -0.22f, -0.06f, 0.45f, 0.22f, 0.06f), s, WaterRender.argb(0.85f, 0x9FE8FF), WaterRender.FULL_BRIGHT);
        ps.pushPose();
        ps.translate(0.45f, 0, 0);
        WaterRender.solid(water, ps.last(), WaterRender.taper(1.1f, 0.3f, 0.12f, 0.0f, 0.0f), s, WaterRender.argb(0.88f, 0x6FD8FF), WaterRender.FULL_BRIGHT);
        ps.popPose();

        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        float pulse = e.isLaunched() ? 0.8f : 0.45f + 0.35f * Mth.sin(t * 0.6f);
        ps.pushPose();
        ps.translate(-1.5f, 0, 0);
        WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(3.0f, 0.03f, 0.03f, 0.05f, 0.05f), WaterRender.argb(pulse, 0xBFF6FF));
        ps.popPose();
        ps.popPose();
    }
}
