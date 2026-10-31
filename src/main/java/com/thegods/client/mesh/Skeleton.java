package com.thegods.client.mesh;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Posa di uno scheletro: rotazioni locali (attorno al perno di ogni osso) e
 * spostamenti; calcola le matrici "modello" di ogni osso per lo skinning.
 * Le ossa nel JSON sono ordinate con i genitori prima dei figli.
 */
public final class Skeleton {
    private final GodMesh mesh;
    private final float[] rx, ry, rz, tx, ty, tz;
    public final Matrix4f[] matrices;

    public GodMesh mesh() {
        return mesh;
    }

    public Skeleton(GodMesh mesh) {
        this.mesh = mesh;
        int n = mesh.bones().size();
        rx = new float[n];
        ry = new float[n];
        rz = new float[n];
        tx = new float[n];
        ty = new float[n];
        tz = new float[n];
        matrices = new Matrix4f[n];
        for (int i = 0; i < n; i++) matrices[i] = new Matrix4f();
    }

    public void reset() {
        java.util.Arrays.fill(rx, 0);
        java.util.Arrays.fill(ry, 0);
        java.util.Arrays.fill(rz, 0);
        java.util.Arrays.fill(tx, 0);
        java.util.Arrays.fill(ty, 0);
        java.util.Arrays.fill(tz, 0);
    }

    /** Azzera e restituisce se stesso (comodo per le catene). */
    public Skeleton reset2() {
        reset();
        return this;
    }

    /** Rotazione (radianti) applicata in ordine Z, Y, X come le ModelPart vanilla. */
    public Skeleton rotate(String bone, float x, float y, float z) {
        int i = mesh.boneIndex(bone);
        if (i >= 0) {
            rx[i] += x;
            ry[i] += y;
            rz[i] += z;
        }
        return this;
    }

    public Skeleton translate(String bone, float x, float y, float z) {
        int i = mesh.boneIndex(bone);
        if (i >= 0) {
            tx[i] += x;
            ty[i] += y;
            tz[i] += z;
        }
        return this;
    }

    public Skeleton compute() {
        Quaternionf q = new Quaternionf();
        for (int i = 0; i < matrices.length; i++) {
            GodMesh.Bone b = mesh.bones().get(i);
            Matrix4f m = matrices[i];
            if (b.parent() >= 0) m.set(matrices[b.parent()]);
            else m.identity();
            m.translate(b.px() + tx[i], b.py() + ty[i], b.pz() + tz[i]);
            m.rotate(q.rotationZYX(rz[i], ry[i], rx[i]));
            m.translate(-b.px(), -b.py(), -b.pz());
        }
        return this;
    }
}
