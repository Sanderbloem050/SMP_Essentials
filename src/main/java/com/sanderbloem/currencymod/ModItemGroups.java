package com.sanderbloem.currencymod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> CURRENCY_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "currency"));

    public static void register() {
        CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.literal("SMP Currency"))
                .icon(() -> new ItemStack(ModItems.GOLD_COIN))
                .displayItems((params, output) -> {
                    output.accept(ModItems.COPPER_COIN);
                    output.accept(ModItems.SILVER_COIN);
                    output.accept(ModItems.GOLD_COIN);
                    output.accept(ModItems.CRATE_KEY);
                    output.accept(ModItems.BRONZE_KEY);
                    output.accept(ModItems.SILVER_KEY);
                    output.accept(ModItems.GOLD_KEY);
                    output.accept(ModBlocks.ATM);
                    output.accept(ModBlocks.BOUNTY_BOARD);
                    output.accept(ModBlocks.LOOT_CRATE);
                    output.accept(ModBlocks.BRONZE_CRATE);
                    output.accept(ModBlocks.SILVER_CRATE);
                    output.accept(ModBlocks.GOLD_CRATE);
                    output.accept(ModBlocks.AUCTION_TERMINAL);
                    output.accept(ModBlocks.QUEST_BOARD);
                })
                .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "currency"), tab);
    }
}
