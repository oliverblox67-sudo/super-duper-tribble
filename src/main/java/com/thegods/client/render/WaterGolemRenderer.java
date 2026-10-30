package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.entity.WaterGolemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Il Titano degli Abissi, interamente procedurale.
 * Animazioni: emersione dall'acqua, camminata pesante, respiro, pugno a martello,
 * schianto a due braccia con inclinazione del busto, scioglimento alla morte.
 * Spazio locale: Y su, +Z avanti.
 */
public class WaterGolemRenderer extends EntityRenderer<WaterGolemEntity> {
    private static final int DEEP = 0x0F4FAE, MID = 0x2FA8F0, FOAM = 0xBFF3FF, CORE = 0x8FF0FF;

    public WaterGolemRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.6f;
    }

    @Override
    public ResourceLocation getTextureLocation(WaterGolemEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private record Pose(float t, float walkPos, float walkSpeed, float punch, float slam, float alpha, int tint) {}

    @Override
    public void render(WaterGolemEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        float t = e.tickCount + pt;
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        float punch = e.attackAnim > 0 ? 1 - (e.attackAnim - pt) / 10f : -1;
        float slam = e.slamAnim > 0 ? 1 - (e.slamAnim - pt) / 20f : -1;
        float rise = e.emerge > 0 ? 1 - Mth.clamp((e.emerge - pt) / 30f, 0, 1) : 1;
        rise = 1 - (1 - rise) * (1 - rise);
        float death = e.deathTime > 0 ? Mth.clamp((e.deathTime + pt) / 20f, 0, 1) : 0;
        float alpha = 0.8f * (1 - death);
        int tint = e.hurtTime > 0 ? FOAM : MID;
        Pose pose = new Pose(t, e.walkAnimation.position(pt), Math.min(1, e.walkAnimation.speed(pt)), punch, slam, alpha, tint);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.translate(0, -(1 - rise) * 6.5f, 0);
        ps.scale(1 + death * 0.4f, 1 - death * 0.85f, 1 + death * 0.4f);

        TextureAtlasSprite s = WaterRender.still();
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        draw(ps, water, null, s, pose);
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        draw(ps, null, glow, s, pose);
        ps.popPose();
        super.render(e, yaw, pt, ps, buffers, light);
    }

    private void draw(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, Pose p) {
        float swing = Mth.sin(p.walkPos * 0.55f) * 0.75f * p.walkSpeed;
        float breathe = 1 + 0.025f * Mth.sin(p.t * 0.09f);
        float lean = 0;
        if (p.slam >= 0) lean = p.slam < 0.55f ? -8 * (p.slam / 0.55f) : Mth.lerp((p.slam - 0.55f) / 0.45f, -8, 28) * (p.slam > 0.85f ? (1 - p.slam) / 0.15f : 1);

        // ---- gambe
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 0.78f, 3.0f, 0);
            float legSwing = side * swing;
            ps.mulPose(Axis.XP.rotation(legSwing));
            limb(ps, water, glow, s, 1.6f, 0.95f, 0.8f, 0.9f, p, DEEP);
            ps.translate(0, -1.6f, 0);
            orb(ps, water, glow, s, 0.5f, p);
            ps.mulPose(Axis.XP.rotation(Math.max(0, -legSwing) * 0.9f));
            limb(ps, water, glow, s, 1.45f, 0.78f, 0.7f, 0.75f, p, MID);
            ps.translate(0, -1.45f, 0);
            box(ps, water, s, -0.45f, -0.05f, -0.5f, 0.45f, 0.3f, 0.8f, p, MID); // piede (punta in avanti)
            ps.popPose();
        }

        // ---- busto (ruota sul bacino)
        ps.pushPose();
        ps.translate(0, 3.0f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(lean));
        ps.mulPose(Axis.YP.rotation(-swing * 0.15f));
        // bacino e vita
        box(ps, water, s, -0.95f, -0.2f, -0.6f, 0.95f, 0.35f, 0.6f, p, DEEP);
        trap(ps, water, s, 0.0f, 0.9f, 0.75f, 0.55f, 0.85f, 0.65f, p, DEEP);
        // torace a V
        ps.pushPose();
        ps.scale(breathe, 1, breathe);
        trap(ps, water, s, 0.9f, 2.6f, 0.95f, 0.72f, 1.5f, 0.88f, p, p.tint);
        ps.popPose();
        // cuore luminoso
        float beat = 0.5f + 0.5f * Mth.sin(p.t * 0.25f);
        if (glow != null) {
            float r = 0.28f + 0.06f * beat;
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.box(-r, 1.85f - r, 0.55f, r, 1.85f + r, 0.95f), WaterRender.argb(0.6f + 0.3f * beat, CORE));
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.box(-0.08f, 0.2f, -0.1f, 0.08f, 2.5f, 0.1f), WaterRender.argb(0.35f, CORE));
        }
        // testa
        ps.pushPose();
        ps.translate(0, 2.6f, 0.15f);
        box(ps, water, s, -0.45f, 0.05f, -0.45f, 0.45f, 0.95f, 0.45f, p, p.tint);
        box(ps, water, s, -0.35f, -0.05f, 0.25f, 0.35f, 0.25f, 0.55f, p, DEEP); // mascella
        if (glow != null) {
            for (int side = -1; side <= 1; side += 2) {
                WaterRender.glowSolid(glow, ps.last().pose(),
                        WaterRender.box(side * 0.12f - 0.12f, 0.52f, 0.455f, side * 0.12f + 0.12f, 0.6f, 0.47f), WaterRender.argb(0.95f, 0xE0FFFF));
            }
        }
        for (int i = -2; i <= 2; i++) { // cresta
            ps.pushPose();
            ps.translate(i * 0.18f, 0.9f, -0.05f - Math.abs(i) * 0.05f);
            ps.mulPose(Axis.ZP.rotationDegrees(90 - i * 12));
            ps.mulPose(Axis.YP.rotationDegrees(20));
            spike(ps, water, glow, s, 0.75f - Math.abs(i) * 0.15f, 0.16f, p);
            ps.popPose();
        }
        ps.popPose();

        // ---- braccia
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            ps.translate(side * 1.75f, 2.35f, 0);
            // spallaccio con spuntoni
            box(ps, water, s, -0.6f, -0.35f, -0.6f, 0.6f, 0.45f, 0.6f, p, p.tint);
            for (int k = 0; k < 3; k++) {
                ps.pushPose();
                ps.translate(side * 0.3f, 0.35f, -0.3f + k * 0.3f);
                ps.mulPose(Axis.ZP.rotationDegrees(side > 0 ? 55 : 125));
                spike(ps, water, glow, s, 0.7f - k * 0.1f, 0.18f, p);
                ps.popPose();
            }
            float armX = -side * swing * 0.8f; // camminata: opposte alle gambe
            boolean right = side < 0;
            if (p.slam >= 0) {
                armX = p.slam < 0.55f ? Mth.lerp(p.slam / 0.55f, 0, -3.0f) : Mth.lerp((p.slam - 0.55f) / 0.45f, -3.0f, -0.6f);
            } else if (right && p.punch >= 0) {
                armX = p.punch < 0.35f ? Mth.lerp(p.punch / 0.35f, 0, -2.7f) : Mth.lerp(Math.min(1, (p.punch - 0.35f) / 0.3f), -2.7f, -1.3f);
            }
            ps.mulPose(Axis.XP.rotation(armX));
            ps.mulPose(Axis.ZP.rotationDegrees(side * 8));
            limb(ps, water, glow, s, 1.7f, 0.75f, 0.65f, 0.75f, p, DEEP);
            ps.translate(0, -1.7f, 0);
            orb(ps, water, glow, s, 0.42f, p);
            ps.mulPose(Axis.XP.rotation(-0.25f));
            limb(ps, water, glow, s, 1.5f, 0.65f, 0.9f, 0.7f, p, MID); // avambraccio a guanto
            ps.translate(0, -1.5f, 0);
            box(ps, water, s, -0.5f, -0.85f, -0.5f, 0.5f, 0.05f, 0.5f, p, p.tint); // pugno
            if (glow != null) {
                WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.box(-0.2f, -0.6f, 0.49f, 0.2f, -0.3f, 0.52f), WaterRender.argb(0.7f, CORE));
            }
            ps.popPose();
        }
        ps.popPose();
    }

    // ------------------------------------------------------------ primitive

    /** Segmento che pende verso il basso (-Y) dal punto corrente. */
    private void limb(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s,
                      float len, float w0, float w1, float depth, Pose p, int rgb) {
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(-90));
        if (water != null) {
            WaterRender.solid(water, ps.last(), WaterRender.taper(len * 1.03f, w0, depth, w1, depth * 0.95f), s,
                    WaterRender.argb(p.alpha, WaterRender.lerpColor(0.4f, rgb, p.tint)), WaterRender.FULL_BRIGHT);
        }
        if (glow != null) {
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(len, w0 * 0.18f, 0.12f, w1 * 0.18f, 0.12f),
                    WaterRender.argb(0.3f + 0.2f * Mth.sin(p.t * 0.2f), CORE));
        }
        ps.popPose();
    }

    private void orb(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, float r, Pose p) {
        if (water != null) box(ps, water, s, -r, -r, -r, r, r, r, p, FOAM);
    }

    private void spike(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, float len, float w, Pose p) {
        if (water != null) {
            WaterRender.solid(water, ps.last(), WaterRender.taper(len, w, w, 0.01f, 0.01f), s,
                    WaterRender.argb(p.alpha, FOAM), WaterRender.FULL_BRIGHT);
        }
        if (glow != null) {
            WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(len * 0.9f, w * 0.3f, w * 0.3f, 0, 0), WaterRender.argb(0.4f, CORE));
        }
    }

    private void box(PoseStack ps, VertexConsumer water, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, Pose p, int rgb) {
        if (water == null) return;
        WaterRender.solid(water, ps.last(), WaterRender.box(x0, y0, z0, x1, y1, z1), s, WaterRender.argb(p.alpha, rgb), WaterRender.FULL_BRIGHT);
    }

    /** Tronco a sezione rettangolare da y0 (mezze misure w0,d0) a y1 (w1,d1). */
    private void trap(PoseStack ps, VertexConsumer water, TextureAtlasSprite s, float y0, float y1,
                      float w0, float d0, float w1, float d1, Pose p, int rgb) {
        if (water == null) return;
        Vector3f[] c = {
                new Vector3f(-w0, y0, -d0), new Vector3f(w0, y0, -d0), new Vector3f(w1, y1, -d1), new Vector3f(-w1, y1, -d1),
                new Vector3f(-w0, y0, d0), new Vector3f(w0, y0, d0), new Vector3f(w1, y1, d1), new Vector3f(-w1, y1, d1)
        };
        WaterRender.solid(water, ps.last(), c, s, WaterRender.argb(p.alpha, rgb), WaterRender.FULL_BRIGHT);
    }
}
