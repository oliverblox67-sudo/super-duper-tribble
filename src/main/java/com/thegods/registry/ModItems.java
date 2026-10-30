package com.thegods.registry;

import com.thegods.TheGods;
import com.thegods.god.Gods;
import com.thegods.item.DivineRelicItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TheGods.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TheGods.MODID);

    /** Conchiglia di Poseidone: chi la suona diventa il Signore dei Mari. */
    public static final DeferredItem<DivineRelicItem> POSEIDON_CONCH = ITEMS.register("poseidon_conch",
            () -> new DivineRelicItem(() -> Gods.POSEIDON, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thegods"))
            .icon(() -> new ItemStack(POSEIDON_CONCH.get()))
            .displayItems((params, out) -> out.accept(POSEIDON_CONCH.get()))
            .build());

    private ModItems() {}
}
