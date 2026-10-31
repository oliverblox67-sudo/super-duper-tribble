package com.thegods.client.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thegods.client.render.GodRenderTypes;
import com.thegods.client.render.WaterRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Disegna le parti di una {@link GodMesh}: skinning a 2 ossa sulla CPU,
 * colore per vertice tramite {@link Tint}, materiale passato allo shader.
 */
public final class MeshDrawer {

    /** Colore ARGB di un vertice (tinta s0, finestra s1, altezza y nello spazio della mesh). */
    @FunctionalInterface
    public interface Tint {
        int argb(float s0, float s1, float y);
    }

    private static float[] px = new float[0], py = new float[0], pz = new float[0];
    private static float[] nx = new float[0], ny = new float[0], nz = new float[0];

    public static void draw(PoseStack ps, MultiBufferSource buffers, GodMesh mesh, GodMesh.Part part,
                            @Nullable Skeleton skeleton, Tint tint, int light, boolean cull) {
        if (part == null) return;
        boolean glow = part.material() == GodMesh.Material.GLOW;
        VertexConsumer vc = buffers.getBuffer(glow ? GodRenderTypes.glow() : GodRenderTypes.water(cull));
        boolean fancy = GodRenderTypes.fancy();
        if (glow && !fancy) {
            drawGlowFallback(ps, vc, part, skeleton, tint);
            return;
        }
        transform(part, skeleton);
        PoseStack.Pose pose = ps.last();
        int overlay = fancy ? OverlayTexture.pack(part.material().id, 0) : OverlayTexture.NO_OVERLAY;
        TextureAtlasSprite sprite = fancy ? null : WaterRender.still();
        float[] v = part.v();
        int[] t = part.tris();
        for (int i = 0; i < t.length; i += 3) {
            vertex(vc, pose, v, t[i], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 1], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 2], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 2], tint, light, overlay, sprite);
        }
        int[] q = part.quads();
        for (int i = 0; i < q.length; i += 4) {
            for (int k = 0; k < 4; k++) vertex(vc, pose, v, q[i + k], tint, light, overlay, sprite);
        }
    }

    /** Disegna solo i triangoli il cui osso dominante soddisfa il filtro (es. solo l'avambraccio). */
    public static void drawSubset(PoseStack ps, MultiBufferSource buffers, GodMesh.Part part, @Nullable Skeleton skeleton,
                                  Tint tint, int light, boolean cull, java.util.function.IntPredicate boneFilter) {
        if (part == null) return;
        if (!part.skinned()) {
            if (part.bone() >= 0 && boneFilter.test(part.bone())) draw(ps, buffers, null, part, skeleton, tint, light, cull);
            return;
        }
        VertexConsumer vc = buffers.getBuffer(GodRenderTypes.water(cull));
        boolean fancy = GodRenderTypes.fancy();
        transform(part, skeleton);
        PoseStack.Pose pose = ps.last();
        int overlay = fancy ? OverlayTexture.pack(part.material().id, 0) : OverlayTexture.NO_OVERLAY;
        TextureAtlasSprite sprite = fancy ? null : WaterRender.still();
        float[] v = part.v();
        int[] t = part.tris();
        for (int i = 0; i < t.length; i += 3) {
            int a = t[i];
            int bone = part.weights()[a] >= 0.5f ? part.joints()[a * 2] : part.joints()[a * 2 + 1];
            if (!boneFilter.test(bone)) continue;
            vertex(vc, pose, v, t[i], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 1], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 2], tint, light, overlay, sprite);
            vertex(vc, pose, v, t[i + 2], tint, light, overlay, sprite);
        }
    }

    public static void drawAll(PoseStack ps, MultiBufferSource buffers, GodMesh mesh, @Nullable Skeleton skeleton,
                               Tint tint, int light, boolean cullBody) {
        // prima l'acqua, poi i bagliori (ordine dei buffer)
        for (GodMesh.Part part : mesh.parts().values()) {
            if (part.material() != GodMesh.Material.GLOW) draw(ps, buffers, mesh, part, skeleton, tint, light, cullBody && part.skinned());
        }
        for (GodMesh.Part part : mesh.parts().values()) {
            if (part.material() == GodMesh.Material.GLOW) draw(ps, buffers, mesh, part, skeleton, tint, light, false);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float[] v, int idx, Tint tint, int light, int overlay,
                               @Nullable TextureAtlasSprite sprite) {
        int o = idx * GodMesh.STRIDE;
        float u = v[o + 3], vv = v[o + 4];
        if (sprite != null) {
            u = sprite.getU(Mth.frac(u * 2));
            vv = sprite.getV(Mth.frac(vv * 2));
        }
        vc.addVertex(pose, px[idx], py[idx], pz[idx])
                .setColor(tint.argb(v[o + 8], v[o + 9], v[o + 1]))
                .setUv(u, vv)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx[idx], ny[idx], nz[idx]);
    }

    /** Calcola posizioni e normali (con skinning se serve) nei buffer statici. */
    private static void transform(GodMesh.Part part, @Nullable Skeleton skeleton) {
        int n = part.vertexCount();
        if (px.length < n) {
            px = new float[n];
            py = new float[n];
            pz = new float[n];
            nx = new float[n];
            ny = new float[n];
            nz = new float[n];
        }
        float[] v = part.v();
        Vector3f a = new Vector3f(), b = new Vector3f(), na = new Vector3f(), nb = new Vector3f();
        for (int i = 0; i < n; i++) {
            int o = i * GodMesh.STRIDE;
            float x = v[o], y = v[o + 1], z = v[o + 2], vx = v[o + 5], vy = v[o + 6], vz = v[o + 7];
            if (skeleton != null && part.skinned()) {
                Matrix4f m0 = skeleton.matrices[part.joints()[i * 2]];
                Matrix4f m1 = skeleton.matrices[part.joints()[i * 2 + 1]];
                float w = part.weights()[i];
                m0.transformPosition(x, y, z, a);
                m1.transformPosition(x, y, z, b);
                m0.transformDirection(vx, vy, vz, na);
                m1.transformDirection(vx, vy, vz, nb);
                a.lerp(b, 1 - w);
                na.lerp(nb, 1 - w).normalize();
                px[i] = a.x;
                py[i] = a.y;
                pz[i] = a.z;
                nx[i] = na.x;
                ny[i] = na.y;
                nz[i] = na.z;
            } else if (skeleton != null && part.bone() >= 0) {
                Matrix4f m = skeleton.matrices[part.bone()];
                m.transformPosition(x, y, z, a);
                m.transformDirection(vx, vy, vz, na);
                px[i] = a.x;
                py[i] = a.y;
                pz[i] = a.z;
                nx[i] = na.x;
                ny[i] = na.y;
                nz[i] = na.z;
            } else {
                px[i] = x;
                py[i] = y;
                pz[i] = z;
                nx[i] = vx;
                ny[i] = vy;
                nz[i] = vz;
            }
        }
    }

    /** Senza shader: i bagliori usano la pipeline dei fulmini (solo posizione + colore). */
    private static void drawGlowFallback(PoseStack ps, VertexConsumer vc, GodMesh.Part part, @Nullable Skeleton skeleton, Tint tint) {
        transform(part, skeleton);
        Matrix4f m = ps.last().pose();
        float[] v = part.v();
        int[] t = part.tris();
        for (int i = 0; i < t.length; i += 3) {
            int[] ids = {t[i], t[i + 1], t[i + 2], t[i + 2]};
            for (int id : ids) vc.addVertex(m, px[id], py[id], pz[id]).setColor(tint.argb(v[id * GodMesh.STRIDE + 8], 0, v[id * GodMesh.STRIDE + 1]));
        }
        int[] q = part.quads();
        for (int i = 0; i < q.length; i++) {
            int id = q[i];
            vc.addVertex(m, px[id], py[id], pz[id]).setColor(tint.argb(v[id * GodMesh.STRIDE + 8], 0, v[id * GodMesh.STRIDE + 1]));
        }
    }

    /**
     * Emette un singolo vertice per geometria costruita al volo (es. il corpo del Leviatano),
     * gestendo shader personalizzato o fallback vanilla.
     */
    public static void emit(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, int argb,
                            float u, float v, float nxx, float nyy, float nzz, int light, GodMesh.Material material) {
        int overlay;
        if (GodRenderTypes.fancy()) {
            overlay = OverlayTexture.pack(material.id, 0);
        } else {
            TextureAtlasSprite s = WaterRender.still();
            u = s.getU(Mth.frac(u));
            v = s.getV(Mth.frac(v));
            overlay = OverlayTexture.NO_OVERLAY;
        }
        vc.addVertex(pose, x, y, z).setColor(argb).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nxx, nyy, nzz);
    }

    private MeshDrawer() {}
}
