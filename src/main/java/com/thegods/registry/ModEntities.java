package com.thegods.registry;

import com.thegods.TheGods;
import com.thegods.entity.HydroBeamEntity;
import com.thegods.entity.LeviathanEntity;
import com.thegods.entity.TidalDomeEntity;
import com.thegods.entity.WaterGolemEntity;
import com.thegods.entity.WaterSpearEntity;
import com.thegods.entity.MaelstromEntity;
import com.thegods.entity.TsunamiEntity;
import com.thegods.entity.WaterPrisonEntity;
import com.thegods.entity.WaterTridentEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TheGods.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WaterTridentEntity>> WATER_TRIDENT =
            spell("water_trident", WaterTridentEntity::new, 1.0f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<HydroBeamEntity>> HYDRO_BEAM =
            spell("hydro_beam", HydroBeamEntity::new, 0.5f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<TsunamiEntity>> TSUNAMI =
            spell("tsunami", TsunamiEntity::new, 2.0f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<MaelstromEntity>> MAELSTROM =
            spell("maelstrom", MaelstromEntity::new, 2.0f, 2.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<WaterPrisonEntity>> WATER_PRISON =
            spell("water_prison", WaterPrisonEntity::new, 1.0f, 1.0f);

    public static final DeferredHolder<EntityType<?>, EntityType<WaterSpearEntity>> WATER_SPEAR =
            spell("water_spear", WaterSpearEntity::new, 0.5f, 0.5f);
    public static final DeferredHolder<EntityType<?>, EntityType<TidalDomeEntity>> TIDAL_DOME =
            spell("tidal_dome", TidalDomeEntity::new, 1.0f, 1.0f);
    public static final DeferredHolder<EntityType<?>, EntityType<LeviathanEntity>> LEVIATHAN =
            spell("leviathan", LeviathanEntity::new, 2.0f, 2.0f);

    /** Il Titano degli Abissi: una creatura vera (IA, vita, cavalcabile). */
    public static final DeferredHolder<EntityType<?>, EntityType<WaterGolemEntity>> WATER_GOLEM = ENTITIES.register("water_golem",
            () -> EntityType.Builder.of(WaterGolemEntity::new, MobCategory.MISC)
                    .sized(2.6f, 6.8f)
                    .fireImmune()
                    .noSave()
                    .clientTrackingRange(10)
                    .build(TheGods.MODID + ":water_golem"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WATER_GOLEM.get(), WaterGolemEntity.createAttributes().build());
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> spell(
            String name, EntityType.EntityFactory<T> factory, float w, float h) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .sized(w, h)
                .noSave()
                .noSummon()
                .fireImmune()
                .clientTrackingRange(16)
                .updateInterval(1)
                .build(TheGods.MODID + ":" + name));
    }

    private ModEntities() {}
}
