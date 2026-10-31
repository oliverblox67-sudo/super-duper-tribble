package com.thegods.client.mesh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thegods.TheGods;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Carica (e ricarica con F3+T) tutte le mesh in assets/thegods/meshes/. */
public final class MeshLibrary extends SimplePreparableReloadListener<Map<String, GodMesh>> {
    public static final MeshLibrary INSTANCE = new MeshLibrary();
    private static volatile Map<String, GodMesh> meshes = Map.of();

    @Nullable
    public static GodMesh get(String name) {
        return meshes.get(name);
    }

    @Override
    protected Map<String, GodMesh> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, GodMesh> out = new HashMap<>();
        Map<ResourceLocation, Resource> found = manager.listResources("meshes",
                rl -> rl.getNamespace().equals(TheGods.MODID) && rl.getPath().endsWith(".json"));
        for (var entry : found.entrySet()) {
            String path = entry.getKey().getPath();
            String name = path.substring("meshes/".length(), path.length() - ".json".length());
            try (Reader reader = entry.getValue().openAsReader()) {
                out.put(name, parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                TheGods.LOGGER.error("Mesh non valida: {}", entry.getKey(), e);
            }
        }
        TheGods.LOGGER.info("The Gods: caricate {} mesh 3D", out.size());
        return out;
    }

    @Override
    protected void apply(Map<String, GodMesh> prepared, ResourceManager manager, ProfilerFiller profiler) {
        meshes = Map.copyOf(prepared);
    }

    private static GodMesh parse(JsonObject root) {
        List<GodMesh.Bone> bones = new ArrayList<>();
        if (root.has("bones")) {
            for (JsonElement el : root.getAsJsonArray("bones")) {
                JsonObject b = el.getAsJsonObject();
                JsonArray pv = b.getAsJsonArray("pivot");
                bones.add(new GodMesh.Bone(b.get("name").getAsString(), b.get("parent").getAsInt(),
                        pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat()));
            }
        }
        Map<String, GodMesh.Part> parts = new LinkedHashMap<>();
        for (var e : root.getAsJsonObject("parts").entrySet()) {
            JsonObject p = e.getValue().getAsJsonObject();
            int bone = -1;
            if (p.has("bone")) {
                String bn = p.get("bone").getAsString();
                for (int i = 0; i < bones.size(); i++) if (bones.get(i).name().equals(bn)) bone = i;
            }
            parts.put(e.getKey(), new GodMesh.Part(e.getKey(), GodMesh.Material.of(p.get("material").getAsString()),
                    floats(p.getAsJsonArray("v")), p.has("t") ? ints(p.getAsJsonArray("t")) : new int[0],
                    p.has("q") ? ints(p.getAsJsonArray("q")) : new int[0],
                    p.has("j") ? ints(p.getAsJsonArray("j")) : null,
                    p.has("w") ? floats(p.getAsJsonArray("w")) : null, bone));
        }
        return new GodMesh(List.copyOf(bones), parts);
    }

    private static float[] floats(JsonArray a) {
        float[] out = new float[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    private static int[] ints(JsonArray a) {
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
        return out;
    }

    private MeshLibrary() {}
}
