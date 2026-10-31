package com.thegods.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.thegods.TheGods;
import com.thegods.client.ClientConfig;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * RenderType con lo shader dell'acqua divina (assets/thegods/shaders/core/water_fx).
 * Se lo shader non si carica, o è disattivato nel config, si torna all'acqua vanilla.
 */
public final class GodRenderTypes {
    @Nullable
    private static ShaderInstance waterShader;

    private static final RenderStateShard.ShaderStateShard WATER_SHADER = new RenderStateShard.ShaderStateShard(() -> waterShader);

    /** Superfici sottili (penne, pinne, mantello): visibili da entrambi i lati. */
    public static final RenderType WATER_FX = RenderType.create("thegods_water_fx", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 262144, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(WATER_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true));

    /** Corpi chiusi (Titano, Leviatano): solo le facce esterne, così si vede dentro. */
    public static final RenderType WATER_FX_CULL = RenderType.create("thegods_water_fx_cull", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 262144, true, true,
            RenderType.CompositeState.builder()
                    .setShaderState(WATER_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
                    .createCompositeState(true));

    /** Bagliori additivi (occhi, nuclei di energia). */
    public static final RenderType GLOW_FX = RenderType.create("thegods_glow_fx", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 65536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(WATER_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    public static void registerShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(), TheGods.id("water_fx"), DefaultVertexFormat.NEW_ENTITY),
                    shader -> waterShader = shader);
        } catch (IOException e) {
            TheGods.LOGGER.error("Impossibile caricare lo shader dell'acqua divina, uso l'acqua vanilla", e);
            waterShader = null;
        }
    }

    /** True se usiamo lo shader personalizzato (altrimenti fallback vanilla). */
    public static boolean fancy() {
        return waterShader != null && ClientConfig.FANCY_WATER.get();
    }

    public static RenderType water(boolean cull) {
        if (!fancy()) return RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS);
        return cull ? WATER_FX_CULL : WATER_FX;
    }

    public static RenderType glow() {
        return fancy() ? GLOW_FX : RenderType.lightning();
    }

    private GodRenderTypes() {}
}
