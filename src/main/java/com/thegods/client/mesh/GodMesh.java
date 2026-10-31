package com.thegods.client.mesh;

import java.util.List;
import java.util.Map;

/**
 * Una mesh caricata da assets/thegods/meshes/*.json (generata da tools/meshgen).
 * Vertici con stride 10: x,y,z, u,v, nx,ny,nz, s0 (tinta), s1 (finestra).
 */
public record GodMesh(List<Bone> bones, Map<String, Part> parts) {
    public static final int STRIDE = 10;

    public record Bone(String name, int parent, float px, float py, float pz) {}

    public enum Material {
        WATER(0), CRYSTAL(1), SCALES(2), MEMBRANE(3), GLOW(4);

        public final int id;

        Material(int id) {
            this.id = id;
        }

        public static Material of(String s) {
            return switch (s) {
                case "crystal" -> CRYSTAL;
                case "scales" -> SCALES;
                case "membrane" -> MEMBRANE;
                case "glow" -> GLOW;
                default -> WATER;
            };
        }
    }

    /**
     * @param tris  indici dei triangoli
     * @param quads indici dei quad
     * @param joints 2 ossa per vertice (null se rigida)
     * @param weights peso del primo osso per vertice
     * @param bone osso della parte rigida (-1 se nessuno)
     */
    public record Part(String name, Material material, float[] v, int[] tris, int[] quads,
                       int[] joints, float[] weights, int bone) {
        public int vertexCount() {
            return v.length / STRIDE;
        }

        public boolean skinned() {
            return joints != null;
        }
    }

    public int boneIndex(String name) {
        for (int i = 0; i < bones.size(); i++) if (bones.get(i).name().equals(name)) return i;
        return -1;
    }

    public Part part(String name) {
        return parts.get(name);
    }
}
