package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.client.mesh.GodMesh;
import com.thegods.client.mesh.MeshDrawer;
import com.thegods.client.mesh.MeshLibrary;
import com.thegods.client.mesh.Skeleton;
import com.thegods.god.GodData;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * L'Avatar degli Abissi: il titano d'acqua avvolge il giocatore, che resta visibile
 * (a grandezza normale) dentro il petto, sospeso in un nucleo di luce.
 * Lo scheletro del titano copia la posa del giocatore frame per frame.
 */
public final class AvatarRenderer {
    private static final int DEEP = 0x0C46A6, MID = 0x35B0F4, FOAM = 0xC8F6FF, CORE = 0x8FF0FF;
    private static final float TITAN_HEIGHT = 7.0f;
    private static final float CHEST_Y = 3.35f;
    private static boolean rendering;
    private static Skeleton skeleton;

    public static float grow(AbstractClientPlayer player, float pt) {
        GodData d = player.getData(ModAttachments.GOD_DATA);
        return Mth.lerp(pt, d.clientAvatarPrev, d.clientAvatar);
    }

    /** @return true se l'avatar è stato disegnato (l'evento vanilla va annullato). */
    public static boolean render(AbstractClientPlayer player, PlayerRenderer renderer, PoseStack ps,
                                 MultiBufferSource buffers, int light, float pt) {
        if (rendering) return false;
        float grow = grow(player, pt);
        if (grow <= 0.01f) return false;
        GodMesh mesh = MeshLibrary.get("titan");
        if (mesh == null) return false;
        if (skeleton == null || skeleton.mesh() != mesh) skeleton = new Skeleton(mesh);

        float ease = grow * grow * (3 - 2 * grow);
        float scale = player.getScale();
        float meshScale = player.getBbHeight() / TITAN_HEIGHT;
        float bodyYaw = Mth.rotLerp(pt, player.yBodyRotO, player.yBodyRot);
        float t = player.tickCount + pt;

        // 1) il pilota, a grandezza (quasi) normale, nel petto del titano
        rendering = true;
        try {
            ps.pushPose();
            ps.translate(0, CHEST_Y * meshScale * ease, 0);
            float pilot = Mth.lerp(ease, 1.0f, 0.86f) / scale;
            ps.scale(pilot, pilot, pilot);
            renderer.render(player, Mth.rotLerp(pt, player.yRotO, player.getYRot()), pt, ps, buffers, light);
            ps.popPose();
        } finally {
            rendering = false;
        }

        // 2) il titano copia la posa appena calcolata per il pilota
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();
        skeleton.reset();
        TitanPose.fromPlayer(skeleton, model, t);
        skeleton.compute();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        ps.scale(meshScale, meshScale, meshScale);
        float fill = ease * (TITAN_HEIGHT + 1.6f) - 0.6f; // l'acqua sale dai piedi alla testa
        boolean hurt = player.hurtTime > 0;
        MeshDrawer.drawAll(ps, buffers, mesh, skeleton, (s0, s1, y) -> {
            float a = Mth.clamp((fill - y) / 1.2f, 0, 1) * 0.82f * (1 - 0.62f * s1);
            return WaterRender.argb(a, WaterRender.lerpColor(s0, DEEP, hurt ? FOAM : MID));
        }, light, true);
        core(ps, buffers, ease, t);
        ps.popPose();
        return true;
    }

    /** Nucleo del pilota: anelli di luce che ruotano come un giroscopio attorno al giocatore. */
    private static void core(PoseStack ps, MultiBufferSource buffers, float k, float t) {
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());
        ps.pushPose();
        ps.translate(0, 4.25f, 0.05f);
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(t * (2.5f + i) + i * 60));
            ps.mulPose(Axis.XP.rotationDegrees(55 * i + 25 + Mth.sin(t * 0.03f + i) * 15));
            Matrix4f m = ps.last().pose();
            float r0 = 1.18f, r1 = 1.25f;
            int segs = 48;
            for (int j = 0; j < segs; j++) {
                float a0 = Mth.TWO_PI * j / segs, a1 = Mth.TWO_PI * (j + 1) / segs;
                float a = (0.25f + 0.3f * Mth.sin(a0 * 3 + t * 0.2f)) * k;
                WaterRender.glowQuad(glow, m,
                        new Vector3f(Mth.cos(a0) * r0, 0, Mth.sin(a0) * r0), new Vector3f(Mth.cos(a1) * r0, 0, Mth.sin(a1) * r0),
                        new Vector3f(Mth.cos(a1) * r1, 0, Mth.sin(a1) * r1), new Vector3f(Mth.cos(a0) * r1, 0, Mth.sin(a0) * r1),
                        WaterRender.argb(a, CORE));
            }
            ps.popPose();
        }
        ps.popPose();
    }

    /** Prima persona: due enormi pugni d'acqua nella visuale. */
    public static void renderHands(AbstractClientPlayer player, PoseStack ps, MultiBufferSource buffers, int light, float pt) {
        GodMesh mesh = MeshLibrary.get("titan");
        if (mesh == null) return;
        if (skeleton == null || skeleton.mesh() != mesh) skeleton = new Skeleton(mesh);
        skeleton.reset();
        skeleton.compute();
        float swing = Mth.sin(player.getAttackAnim(pt) * Mth.PI);
        float bob = Mth.sin((player.tickCount + pt) * 0.08f) * 0.02f;
        int fr = mesh.boneIndex("forearm_r"), fl = mesh.boneIndex("forearm_l");
        MeshDrawer.Tint tint = (s0, s1, y) -> WaterRender.argb(0.78f, WaterRender.lerpColor(s0, DEEP, MID));
        for (int side = -1; side <= 1; side += 2) {
            boolean right = side > 0; // lato destro dello schermo = braccio destro del titano
            float sw = right ? swing : 0;
            ps.pushPose();
            ps.translate(side * 0.78f, -0.5f + bob + sw * 0.25f, -1.35f - sw * 0.6f);
            ps.mulPose(Axis.YP.rotationDegrees(180 + side * 10));
            ps.mulPose(Axis.XP.rotationDegrees(-58 - sw * 30));
            ps.scale(0.4f, 0.4f, 0.4f);
            ps.translate(right ? 2.02f : -2.02f, -3.6f, -0.05f);
            int bone = right ? fr : fl;
            MeshDrawer.drawSubset(ps, buffers, mesh.part("body"), skeleton, tint, light, true, b -> b == bone);
            MeshDrawer.draw(ps, buffers, mesh, mesh.part(right ? "blade_r" : "blade_l"), skeleton, tint, light, false);
            ps.popPose();
        }
    }

    private AvatarRenderer() {}
}
