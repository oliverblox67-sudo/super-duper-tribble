package com.thegods.client;

import com.thegods.TheGods;
import com.thegods.client.gui.GodHud;
import com.thegods.client.gui.TitleCardOverlay;
import com.thegods.client.mesh.MeshLibrary;
import com.thegods.client.render.GodRenderTypes;
import com.thegods.client.render.HydroBeamRenderer;
import com.thegods.client.render.LeviathanRenderer;
import com.thegods.client.render.TidalDomeRenderer;
import com.thegods.client.render.WaterGolemRenderer;
import com.thegods.client.render.WaterSpearRenderer;
import com.thegods.client.render.LimbConstructLayer;
import com.thegods.client.render.MaelstromRenderer;
import com.thegods.client.render.PoseidonFormLayer;
import com.thegods.client.render.TsunamiRenderer;
import com.thegods.client.render.WaterPrisonRenderer;
import com.thegods.client.render.WaterTridentRenderer;
import com.thegods.registry.ModEntities;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Punto d'ingresso solo client: renderer, tasti, HUD. */
@Mod(value = TheGods.MODID, dist = Dist.CLIENT)
public class TheGodsClient {
    public TheGodsClient(IEventBus modBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, ClientConfig.SPEC);
        modBus.addListener(GodRenderTypes::registerShaders);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent e) -> e.registerReloadListener(MeshLibrary.INSTANCE));
        modBus.addListener(TheGodsClient::registerRenderers);
        modBus.addListener(TheGodsClient::addLayers);
        modBus.addListener(TheGodsClient::registerKeys);
        modBus.addListener(TheGodsClient::registerGui);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WATER_TRIDENT.get(), WaterTridentRenderer::new);
        event.registerEntityRenderer(ModEntities.HYDRO_BEAM.get(), HydroBeamRenderer::new);
        event.registerEntityRenderer(ModEntities.TSUNAMI.get(), TsunamiRenderer::new);
        event.registerEntityRenderer(ModEntities.MAELSTROM.get(), MaelstromRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_PRISON.get(), WaterPrisonRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_SPEAR.get(), WaterSpearRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDAL_DOME.get(), TidalDomeRenderer::new);
        event.registerEntityRenderer(ModEntities.LEVIATHAN.get(), LeviathanRenderer::new);
        event.registerEntityRenderer(ModEntities.WATER_GOLEM.get(), WaterGolemRenderer::new);
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) {
                renderer.addLayer(new PoseidonFormLayer(renderer));
                renderer.addLayer(new LimbConstructLayer(renderer));
            }
        }
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.MANIFEST);
        event.register(ModKeys.USE_POWER);
        event.register(ModKeys.NEXT_POWER);
        event.register(ModKeys.PREV_POWER);
        event.register(ModKeys.BODY_FORGE);
    }

    private static void registerGui(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, TheGods.id("god_hud"), GodHud::render);
        event.registerAboveAll(TheGods.id("title_card"), TitleCardOverlay::render);
    }
}
