package com.thegods;

import com.mojang.logging.LogUtils;
import com.thegods.config.GodsConfig;
import com.thegods.god.Gods;
import com.thegods.network.ModNetwork;
import com.thegods.registry.ModAttachments;
import com.thegods.registry.ModEntities;
import com.thegods.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * The Gods — diventa una divinità.
 * Punto d'ingresso comune (client + server).
 */
@Mod(TheGods.MODID)
public class TheGods {
    public static final String MODID = "thegods";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheGods(IEventBus modBus, ModContainer container) {
        ModAttachments.ATTACHMENTS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        container.registerConfig(ModConfig.Type.COMMON, GodsConfig.SPEC);
        Gods.bootstrap();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
