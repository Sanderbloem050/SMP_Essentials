package com.sanderbloem.currencymod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final Item COPPER_COIN = new Item(settings("copper_coin"));
    public static final Item SILVER_COIN = new Item(settings("silver_coin"));
    public static final Item GOLD_COIN   = new Item(settings("gold_coin"));
    public static final Item CRATE_KEY   = new Item(settings("crate_key"));

    private static Item.Properties settings(String name) {
        return new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, name)))
                .stacksTo(64);
    }

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "copper_coin"), COPPER_COIN);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "silver_coin"), SILVER_COIN);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "gold_coin"),   GOLD_COIN);
        Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "crate_key"),   CRATE_KEY);
    }
}
