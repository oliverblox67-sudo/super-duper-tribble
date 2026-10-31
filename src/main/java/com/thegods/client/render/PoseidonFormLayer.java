package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.thegods.god.GodData;
import com.thegods.god.Gods;
import com.thegods.client.mesh.GodMesh;
import com.thegods.client.mesh.MeshDrawer;
import com.thegods.client.mesh.MeshLibrary;
import com.thegods.registry.ModAttachments;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * La forma divina di Poseidone, disegnata sopra il giocatore:
 * - due ali gigantesche fatte d'acqua viva (9 remiganti + 7 copritrici per ala,
 *   ognuna in 5 segmenti che ondeggiano come correnti marine)
 * - correnti d'acqua che orbitano attorno al corpo
 * - corona di spuntoni d'acqua e aureola luminosa
 * - occhi che brillano come il mare profondo
 */
public class PoseidonFormLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final int DEEP = 0x0E4FB8;
    private static final int MID = 0x2FB8FF;
    private static final int FOAM = 0xC8F6FF;
    private static final int CORE = 0x7FE8FF;

    private float alphaMul = 1f;

    public PoseidonFormLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        GodData data = player.getData(ModAttachments.GOD_DATA);
        if (!data.is(Gods.POSEIDON) || player.isInvisible()) return;
        if (Mth.lerp(partialTick, data.clientAvatarPrev, data.clientAvatar) > 0.15f) return; // le ali si fondono nel titano
        float form = Mth.lerp(partialTick, data.clientFormPrev, data.clientForm);
        if (form < 0.01f) return;
        float ult = Mth.lerp(partialTick, data.clientUltimatePrev, data.clientUltimate);
        boolean airborne = player.getAbilities().flying || (!player.onGround() && !player.isInWater());
        float t = ageInTicks;

        // In terza persona da dietro le tue stesse ali diventano più trasparenti per non coprirti la visuale
        var mc = net.minecraft.client.Minecraft.getInstance();
        alphaMul = player == mc.player && mc.options.getCameraType() == net.minecraft.client.CameraType.THIRD_PERSON_BACK ? 0.5f : 1f;

        TextureAtlasSprite still = WaterRender.still();

        // ---------- passaggio 1: acqua traslucida
        ps.pushPose();
        getParentModel().body.translateAndRotate(ps);
        for (int side = -1; side <= 1; side += 2) wing(ps, buffers, null, player, side, form, ult, t, partialTick, airborne);
        VertexConsumer water = buffers.getBuffer(WaterRender.water());
        currents(ps, water, still, form, ult, t);
        ps.popPose();

        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        crown(ps, water, null, still, form, ult, t);
        ps.popPose();

        // ---------- passaggio 2: energia luminosa
        VertexConsumer glow = buffers.getBuffer(WaterRender.glow());

        ps.pushPose();
        getParentModel().head.translateAndRotate(ps);
        crown(ps, null, glow, still, form, ult, t);
        eyes(ps, glow, form, ult, t);
        halo(ps, glow, form, ult, t);
        ps.popPose();
    }

    // =============================================================== ALI

    /**
     * Un'ala costruita come quella di un uccello: un "braccio" d'acqua in 3 segmenti
     * (omero, avambraccio, mano) da cui pendono penne vere (mesh "feather": vessillo curvo
     * + rachide di cristallo), animate dal battito, dalle onde e dalle molle di {@link WingPhysics}.
     */
    private void wing(PoseStack ps, MultiBufferSource buffers, VertexConsumer glow, AbstractClientPlayer player,
                      int side, float form, float ult, float t, float pt, boolean airborne) {
        GodMesh feather = MeshLibrary.get("feather");
        float open = ease(form);
        float flap = Mth.sin(t * (airborne ? 0.2f : 0.075f));
        float beat = airborne ? 1f : 0.35f;
        int wingIdx = side < 0 ? 0 : 1;

        ps.pushPose();
        ps.translate(side * 0.14f, 0.1f, 0.15f);
        ps.scale(side, 1, 1); // specchia: l'ala è disegnata per il lato +X
        float sweep = Mth.lerp(open, 80f, 34f) + flap * 14f * beat - ult * 10f;
        ps.mulPose(Axis.YP.rotationDegrees(-sweep));
        ps.mulPose(Axis.ZP.rotationDegrees(flap * 10f * beat - ult * 8f));
        float size = (0.25f + 0.75f * open) * (1f + ult * 0.4f);
        ps.scale(size, size, size);

        // --- braccio (angoli sopra l'orizzontale, la punta segue di più il battito)
        float[] angles = {
                Mth.lerp(open, -70f, 40f + flap * 8f * beat),
                Mth.lerp(open, -80f, 16f + flap * 14f * beat),
                Mth.lerp(open, -88f, -10f + flap * 20f * beat)
        };
        float[] lens = {0.95f, 1.2f, 1.05f};
        float[][] pts = new float[4][2];
        for (int i = 0; i < 3; i++) {
            float a = angles[i] * Mth.DEG_TO_RAD;
            pts[i + 1][0] = pts[i][0] + Mth.cos(a) * lens[i];
            pts[i + 1][1] = pts[i][1] - Mth.sin(a) * lens[i];
        }
        float total = lens[0] + lens[1] + lens[2];

        VertexConsumer arm = buffers.getBuffer(GodRenderTypes.water(false));
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.translate(pts[i][0], pts[i][1], 0);
            ps.mulPose(Axis.ZP.rotationDegrees(-angles[i]));
            float r0 = Mth.lerp(i / 3f, 0.12f, 0.05f), r1 = Mth.lerp((i + 1) / 3f, 0.12f, 0.05f);
            tube(arm, ps.last(), lens[i] * 1.06f, r0, r1, WaterRender.argb(0.85f * alphaMul, WaterRender.lerpColor(ult * 0.4f, MID, FOAM)));
            if (glow != null) {
                WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(lens[i], r0 * 0.6f, r0 * 0.6f, r1 * 0.6f, r1 * 0.6f),
                        WaterRender.argb((0.45f + 0.3f * ult) * alphaMul, CORE));
            }
            ps.popPose();
        }
        if (feather == null) {
            ps.popPose();
            return;
        }

        // --- remiganti: pendono dal braccio, più lunghe verso la punta
        int primaries = 14;
        for (int i = 0; i < primaries; i++) {
            float f = (i + 0.5f) / primaries;
            float[] at = along(pts, lens, total, 0.06f + f * 0.94f);
            float dir = Mth.lerp(open, -92f, Mth.lerp(f, -98f, -22f) + flap * 6f * beat * f);
            float len = (1.3f + 1.8f * (float) Math.pow(f, 1.25)) * (f > 0.93f ? 0.9f : 1f) * (0.45f + 0.55f * open);
            float lag = WingPhysics.lag(player, wingIdx, i, pt);
            float wave = Mth.sin(t * 0.15f + i * 0.45f) * 5f;
            int base = WaterRender.lerpColor(ult * 0.35f, DEEP, MID);
            featherMesh(ps, buffers, feather, at, dir + lag, len, 2.3f, 0.015f, wave, base, WaterRender.lerpColor(ult * 0.35f, MID, FOAM), 0.78f);
        }
        // --- copritrici: due file, più corte e chiare
        for (int row = 0; row < 2; row++) {
            int coverts = row == 0 ? 11 : 8;
            for (int i = 0; i < coverts; i++) {
                float f = (i + 0.5f) / coverts;
                float[] at = along(pts, lens, total, 0.04f + f * (row == 0 ? 0.86f : 0.7f));
                float dir = Mth.lerp(open, -90f, Mth.lerp(f, -92f, -38f));
                float len = (row == 0 ? 0.75f + 0.6f * f : 0.45f + 0.3f * f) * (0.5f + 0.5f * open);
                float lag = WingPhysics.lag(player, wingIdx, primaries + i / 2, pt) * 0.6f;
                featherMesh(ps, buffers, feather, at, dir + lag, len, row == 0 ? 2.6f : 2.9f, -0.03f - row * 0.03f,
                        Mth.sin(t * 0.12f + i) * 3f, MID, FOAM, 0.85f);
            }
        }
        ps.popPose();
    }

    /** Disegna una penna-mesh: vessillo membranoso + rachide di cristallo. */
    private void featherMesh(PoseStack ps, MultiBufferSource buffers, GodMesh feather, float[] at, float dirDeg, float len,
                             float width, float zOffset, float waveDeg, int colorBase, int colorTip, float alpha) {
        ps.pushPose();
        ps.translate(at[0], at[1], zOffset);
        ps.mulPose(Axis.ZP.rotationDegrees(-dirDeg));
        ps.mulPose(Axis.YP.rotationDegrees(waveDeg));
        ps.scale(len, width, len * 0.8f);
        float a = alpha * alphaMul;
        MeshDrawer.Tint tint = (s0, s1, y) -> WaterRender.argb(a * (1 - 0.35f * s1), WaterRender.lerpColor(s0, colorBase, colorTip));
        MeshDrawer.draw(ps, buffers, feather, feather.part("vane"), null, tint, WaterRender.FULL_BRIGHT, false);
        MeshDrawer.draw(ps, buffers, feather, feather.part("rachis"), null, tint, WaterRender.FULL_BRIGHT, false);
        ps.popPose();
    }

    /** Tubo liscio lungo +X (sezione circolare a 10 lati) con lo shader dell'acqua. */
    private static void tube(VertexConsumer vc, PoseStack.Pose pose, float len, float r0, float r1, int argb) {
        int sides = 10;
        for (int k = 0; k < sides; k++) {
            float a0 = Mth.TWO_PI * k / sides, a1 = Mth.TWO_PI * (k + 1) / sides;
            float[][] q = {{0, r0, a0}, {0, r0, a1}, {len, r1, a1}, {len, r1, a0}};
            for (float[] v : q) {
                float cy = Mth.cos(v[2]), cz = Mth.sin(v[2]);
                MeshDrawer.emit(vc, pose, v[0], cy * v[1], cz * v[1], argb, v[2] / Mth.TWO_PI, v[0], 0, cy, cz,
                        WaterRender.FULL_BRIGHT, GodMesh.Material.WATER);
            }
        }
    }

    /** Punto del braccio alla frazione {@code s} della sua lunghezza. */
    private static float[] along(float[][] pts, float[] lens, float total, float s) {
        float d = s * total;
        for (int i = 0; i < lens.length; i++) {
            if (d <= lens[i] || i == lens.length - 1) {
                float k = Mth.clamp(d / lens[i], 0, 1);
                return new float[]{Mth.lerp(k, pts[i][0], pts[i + 1][0]), Mth.lerp(k, pts[i][1], pts[i + 1][1])};
            }
            d -= lens[i];
        }
        return pts[pts.length - 1];
    }

    // =============================================================== CORRENTI ATTORNO AL CORPO

    private void currents(PoseStack ps, VertexConsumer water, TextureAtlasSprite s, float form, float ult, float t) {
        int streams = ult > 0.05f ? 4 : 2;
        float radius = (0.65f + ult * 0.5f) * ease(form);
        for (int st = 0; st < streams; st++) {
            float phase = st * Mth.TWO_PI / streams;
            int segs = 28;
            Vector3f prevA = null, prevB = null;
            for (int i = 0; i <= segs; i++) {
                float u = (float) i / segs;
                float a = t * 0.11f + u * Mth.TWO_PI * 0.8f + phase;
                float y = 1.35f - u * 1.7f + Mth.sin(t * 0.07f + phase) * 0.15f;
                float r = radius * (0.85f + 0.15f * Mth.sin(u * 9 + t * 0.2f));
                float w = 0.07f * Mth.sin(u * Mth.PI) + 0.01f;
                Vector3f c = new Vector3f(Mth.cos(a) * r, y, Mth.sin(a) * r);
                Vector3f top = new Vector3f(c).add(0, -w, 0);
                Vector3f bot = new Vector3f(c).add(0, w, 0);
                if (prevA != null) {
                    WaterRender.quad(water, ps.last(), prevA, top, bot, prevB, s, 0, 0, 1, 1,
                            WaterRender.argb(0.7f * form, WaterRender.lerpColor(u, MID, FOAM)), WaterRender.FULL_BRIGHT);
                }
                prevA = top;
                prevB = bot;
            }
        }
    }

    // =============================================================== CORONA, OCCHI, AUREOLA

    private void crown(PoseStack ps, VertexConsumer water, VertexConsumer glow, TextureAtlasSprite s, float form, float ult, float t) {
        float grow = ease(Mth.clamp(form * 1.4f - 0.4f, 0, 1));
        if (grow <= 0) return;
        int spikes = 9;
        for (int i = 0; i < spikes; i++) {
            float a = -Mth.HALF_PI + (i - (spikes - 1) / 2f) * (Mth.TWO_PI / spikes);
            float front = 1 - Math.abs(i - (spikes - 1) / 2f) / ((spikes - 1) / 2f);
            float h = (0.22f + 0.38f * front * front) * grow * (1 + ult * 0.4f)
                    * (1 + 0.06f * Mth.sin(t * 0.3f + i));
            ps.pushPose();
            ps.translate(Mth.cos(a) * 0.29f, -0.47f, Mth.sin(a) * 0.29f);
            ps.mulPose(Axis.YP.rotation(-a));
            ps.mulPose(Axis.ZP.rotationDegrees(-90f + 14f));
            if (water != null) {
                WaterRender.solid(water, ps.last(), WaterRender.taper(h, 0.13f, 0.06f, 0.01f, 0.01f), s,
                        WaterRender.argb(0.85f, WaterRender.lerpColor(front, MID, FOAM)), WaterRender.FULL_BRIGHT);
            }
            if (glow != null) {
                WaterRender.glowSolid(glow, ps.last().pose(), WaterRender.taper(h * 0.9f, 0.04f, 0.03f, 0.005f, 0.005f),
                        WaterRender.argb(0.5f + 0.3f * ult, CORE));
            }
            ps.popPose();
        }
    }

    private void eyes(PoseStack ps, VertexConsumer glow, float form, float ult, float t) {
        float a = Mth.clamp(form * 2 - 1, 0, 1) * (0.85f + 0.15f * Mth.sin(t * 0.3f));
        if (a <= 0) return;
        Matrix4f m = ps.last().pose();
        float z = -0.2535f;
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side < 0 ? -0.1875f : 0.0625f, x1 = x0 + 0.125f;
            WaterRender.glowQuad(glow, m, new Vector3f(x0, -0.25f, z), new Vector3f(x1, -0.25f, z),
                    new Vector3f(x1, -0.1875f, z), new Vector3f(x0, -0.1875f, z), WaterRender.argb(a, 0xB8FFFF));
            float g = 0.04f + ult * 0.04f;
            WaterRender.glowQuad(glow, m, new Vector3f(x0 - g, -0.25f - g, z - 0.001f), new Vector3f(x1 + g, -0.25f - g, z - 0.001f),
                    new Vector3f(x1 + g, -0.1875f + g, z - 0.001f), new Vector3f(x0 - g, -0.1875f + g, z - 0.001f),
                    WaterRender.argb(a * 0.35f, MID));
        }
    }

    private void halo(PoseStack ps, VertexConsumer glow, float form, float ult, float t) {
        float grow = ease(Mth.clamp(form * 1.5f - 0.5f, 0, 1));
        if (grow <= 0) return;
        ps.pushPose();
        ps.translate(0, -0.42f + Mth.sin(t * 0.08f) * 0.03f, 0.34f);
        ps.mulPose(Axis.XP.rotationDegrees(-12f));
        ps.mulPose(Axis.ZP.rotationDegrees(t * 1.5f));
        Matrix4f m = ps.last().pose();
        float r0 = 0.5f * grow * (1 + ult * 0.3f), r1 = r0 + 0.05f;
        int segs = 40;
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float pulse = 0.45f + 0.35f * Mth.sin(a0 * 3 + t * 0.2f);
            WaterRender.glowQuad(glow, m,
                    new Vector3f(Mth.cos(a0) * r0, Mth.sin(a0) * r0, 0), new Vector3f(Mth.cos(a1) * r0, Mth.sin(a1) * r0, 0),
                    new Vector3f(Mth.cos(a1) * r1, Mth.sin(a1) * r1, 0), new Vector3f(Mth.cos(a0) * r1, Mth.sin(a0) * r1, 0),
                    WaterRender.argb(pulse * (1 + ult * 0.5f), CORE));
        }
        // Tre "gocce" che orbitano sull'aureola
        for (int i = 0; i < 3; i++) {
            float a = i * Mth.TWO_PI / 3 - t * 0.05f;
            float cx = Mth.cos(a) * (r0 + 0.02f), cy = Mth.sin(a) * (r0 + 0.02f), d = 0.05f;
            WaterRender.glowQuad(glow, m, new Vector3f(cx - d, cy - d, 0.01f), new Vector3f(cx + d, cy - d, 0.01f),
                    new Vector3f(cx + d, cy + d, 0.01f), new Vector3f(cx - d, cy + d, 0.01f), WaterRender.argb(0.9f, FOAM));
        }
        ps.popPose();
    }

    private static float ease(float x) {
        x = Mth.clamp(x, 0, 1);
        return 1 - (1 - x) * (1 - x) * (1 - x);
    }
}
