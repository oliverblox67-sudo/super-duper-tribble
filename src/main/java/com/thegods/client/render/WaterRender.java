package com.thegods.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Primitive geometriche per disegnare acqua "viva": usa le texture animate
 * dell'acqua vanilla (water_still / water_flow) tinte e traslucide,
 * più un passaggio additivo luminoso per i nuclei di energia.
 */
public final class WaterRender {
    public static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    public static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    public static TextureAtlasSprite still() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_STILL);
    }

    public static TextureAtlasSprite flow() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WATER_FLOW);
    }

    /** Acqua traslucida, visibile da entrambi i lati. */
    public static RenderType water() {
        return RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
    }

    /** Energia luminosa additiva (stessa pipeline dei fulmini). */
    public static RenderType glow() {
        return RenderType.lightning();
    }

    public static int argb(float a, int rgb) {
        return FastColor.ARGB32.color(Mth.clamp((int) (a * 255), 0, 255), rgb);
    }

    public static int lerpColor(float t, int a, int b) {
        t = Mth.clamp(t, 0, 1);
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    // ------------------------------------------------------------ quad testurizzati

    public static void quad(VertexConsumer vc, PoseStack.Pose pose,
                            Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                            TextureAtlasSprite s, float u0, float v0, float u1, float v1,
                            int argb, int light) {
        Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(d).sub(a));
        if (n.lengthSquared() < 1e-12f) n.set(0, 1, 0);
        else n.normalize();
        vertex(vc, pose, a, s.getU(u0), s.getV(v0), argb, light, n);
        vertex(vc, pose, b, s.getU(u1), s.getV(v0), argb, light, n);
        vertex(vc, pose, c, s.getU(u1), s.getV(v1), argb, light, n);
        vertex(vc, pose, d, s.getU(u0), s.getV(v1), argb, light, n);
    }

    /** Come {@link #quad} ma con normale fissa verso l'alto: grandi superfici sempre luminose. */
    public static void quadLit(VertexConsumer vc, PoseStack.Pose pose,
                               Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                               TextureAtlasSprite s, float u0, float v0, float u1, float v1,
                               int argb, int light) {
        Vector3f n = new Vector3f(0, 1, 0);
        vertex(vc, pose, a, s.getU(u0), s.getV(v0), argb, light, n);
        vertex(vc, pose, b, s.getU(u1), s.getV(v0), argb, light, n);
        vertex(vc, pose, c, s.getU(u1), s.getV(v1), argb, light, n);
        vertex(vc, pose, d, s.getU(u0), s.getV(v1), argb, light, n);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vector3f p, float u, float v, int argb, int light, Vector3f n) {
        vc.addVertex(pose, p.x, p.y, p.z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, n.x, n.y, n.z);
    }

    // ------------------------------------------------------------ quad luminosi (POSITION_COLOR)

    public static void glowQuad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int argb) {
        vc.addVertex(m, a.x, a.y, a.z).setColor(argb);
        vc.addVertex(m, b.x, b.y, b.z).setColor(argb);
        vc.addVertex(m, c.x, c.y, c.z).setColor(argb);
        vc.addVertex(m, d.x, d.y, d.z).setColor(argb);
        // lato opposto (la pipeline dei fulmini scarta le facce posteriori)
        vc.addVertex(m, d.x, d.y, d.z).setColor(argb);
        vc.addVertex(m, c.x, c.y, c.z).setColor(argb);
        vc.addVertex(m, b.x, b.y, b.z).setColor(argb);
        vc.addVertex(m, a.x, a.y, a.z).setColor(argb);
    }

    // ------------------------------------------------------------ solidi

    /**
     * Prisma rastremato lungo +X: base (x=0) larga w0 e spessa t0, punta (x=len) larga w1 e spessa t1.
     * Larghezza sull'asse Y, spessore sull'asse Z.
     */
    public static Vector3f[] taper(float len, float w0, float t0, float w1, float t1) {
        return new Vector3f[]{
                new Vector3f(0, -w0 / 2, -t0 / 2), new Vector3f(len, -w1 / 2, -t1 / 2),
                new Vector3f(len, w1 / 2, -t1 / 2), new Vector3f(0, w0 / 2, -t0 / 2),
                new Vector3f(0, -w0 / 2, t0 / 2), new Vector3f(len, -w1 / 2, t1 / 2),
                new Vector3f(len, w1 / 2, t1 / 2), new Vector3f(0, w0 / 2, t0 / 2)
        };
    }

    public static Vector3f[] box(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new Vector3f[]{
                new Vector3f(x0, y0, z0), new Vector3f(x1, y0, z0), new Vector3f(x1, y1, z0), new Vector3f(x0, y1, z0),
                new Vector3f(x0, y0, z1), new Vector3f(x1, y0, z1), new Vector3f(x1, y1, z1), new Vector3f(x0, y1, z1)
        };
    }

    private static final int[][] FACES = {
            {0, 1, 2, 3}, {5, 4, 7, 6}, {4, 0, 3, 7}, {1, 5, 6, 2}, {4, 5, 1, 0}, {3, 2, 6, 7}
    };

    public static void solid(VertexConsumer vc, PoseStack.Pose pose, Vector3f[] c, TextureAtlasSprite s, int argb, int light) {
        for (int[] f : FACES) quad(vc, pose, c[f[0]], c[f[1]], c[f[2]], c[f[3]], s, 0, 0, 1, 1, argb, light);
    }

    public static void glowSolid(VertexConsumer vc, Matrix4f m, Vector3f[] c, int argb) {
        for (int[] f : FACES) glowQuad(vc, m, c[f[0]], c[f[1]], c[f[2]], c[f[3]], argb);
    }

    /** Cilindro lungo +Z da z0 a z1, diviso in tratti da ~1 blocco per non stirare la texture. */
    public static void tube(VertexConsumer vc, PoseStack.Pose pose, float z0, float z1, float radius, int sides,
                            float twist, TextureAtlasSprite s, int argb, int light) {
        float len = z1 - z0;
        int pieces = Math.max(1, Mth.ceil(len));
        for (int p = 0; p < pieces; p++) {
            float za = z0 + len * p / pieces, zb = z0 + len * (p + 1) / pieces;
            for (int i = 0; i < sides; i++) {
                float a0 = twist + Mth.TWO_PI * i / sides, a1 = twist + Mth.TWO_PI * (i + 1) / sides;
                Vector3f v0 = new Vector3f(Mth.cos(a0) * radius, Mth.sin(a0) * radius, za);
                Vector3f v1 = new Vector3f(Mth.cos(a1) * radius, Mth.sin(a1) * radius, za);
                Vector3f v2 = new Vector3f(Mth.cos(a1) * radius, Mth.sin(a1) * radius, zb);
                Vector3f v3 = new Vector3f(Mth.cos(a0) * radius, Mth.sin(a0) * radius, zb);
                quad(vc, pose, v0, v1, v2, v3, s, (float) i / sides, 0, (float) (i + 1) / sides, 1, argb, light);
            }
        }
    }

    /** Tronco di cono lungo +Z (raggio r0 a z=0, r1 a z=len). */
    public static void frustum(VertexConsumer vc, PoseStack.Pose pose, float len, float r0, float r1, int sides,
                               float twist, TextureAtlasSprite s, int argb, int light) {
        for (int i = 0; i < sides; i++) {
            float a0 = twist + Mth.TWO_PI * i / sides, a1 = twist + Mth.TWO_PI * (i + 1) / sides;
            quad(vc, pose,
                    new Vector3f(Mth.cos(a0) * r0, Mth.sin(a0) * r0, 0), new Vector3f(Mth.cos(a1) * r0, Mth.sin(a1) * r0, 0),
                    new Vector3f(Mth.cos(a1) * r1, Mth.sin(a1) * r1, len), new Vector3f(Mth.cos(a0) * r1, Mth.sin(a0) * r1, len),
                    s, (float) i / sides, 0, (float) (i + 1) / sides, 1, argb, light);
        }
    }

    /** Orienta lo spazio locale così che +Z punti lungo {@code dir} (normalizzato). */
    public static void orient(PoseStack ps, double dx, double dy, double dz) {
        ps.mulPose(com.mojang.math.Axis.YP.rotation((float) Math.atan2(dx, dz)));
        ps.mulPose(com.mojang.math.Axis.XP.rotation((float) -Math.asin(Mth.clamp(dy, -1, 1))));
    }

    public static void glowTube(VertexConsumer vc, Matrix4f m, float z0, float z1, float radius, int sides, int argb) {
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
            glowQuad(vc, m,
                    new Vector3f(Mth.cos(a0) * radius, Mth.sin(a0) * radius, z0),
                    new Vector3f(Mth.cos(a1) * radius, Mth.sin(a1) * radius, z0),
                    new Vector3f(Mth.cos(a1) * radius, Mth.sin(a1) * radius, z1),
                    new Vector3f(Mth.cos(a0) * radius, Mth.sin(a0) * radius, z1), argb);
        }
    }

    /** Sfera UV con ondulazione. */
    public static void sphere(VertexConsumer vc, PoseStack.Pose pose, float r, int stacks, int slices, float time, float wobble,
                              TextureAtlasSprite s, int argb, int light) {
        Vector3f[][] pts = new Vector3f[stacks + 1][slices + 1];
        for (int i = 0; i <= stacks; i++) {
            float phi = Mth.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                float th = Mth.TWO_PI * j / slices;
                float rr = r * (1 + wobble * Mth.sin(3 * th + time * 0.2f) * Mth.sin(2 * phi + time * 0.13f));
                pts[i][j] = new Vector3f(Mth.sin(phi) * Mth.cos(th) * rr, Mth.cos(phi) * rr, Mth.sin(phi) * Mth.sin(th) * rr);
            }
        }
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                quad(vc, pose, pts[i][j], pts[i][j + 1], pts[i + 1][j + 1], pts[i + 1][j], s,
                        (j % 4) / 4f, (i % 4) / 4f, (j % 4 + 1) / 4f, (i % 4 + 1) / 4f, argb, light);
            }
        }
    }

    private WaterRender() {}
}
