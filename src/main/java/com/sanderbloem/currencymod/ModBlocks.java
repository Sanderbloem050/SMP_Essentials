package com.sanderbloem.currencymod;

import com.sanderbloem.currencymod.block.AtmBlock;
import com.sanderbloem.currencymod.block.LootCrateBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final Block ATM = new AtmBlock(BlockBehaviour.Properties.of()
            .strength(3.0f, 6.0f)
            .setId(ResourceKey.create(Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "atm"))));

    public static final Block LOOT_CRATE = new LootCrateBlock(BlockBehaviour.Properties.of()
            .strength(2.0f, 4.0f)
            .setId(ResourceKey.create(Registries.BLOCK,
                    Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "loot_crate"))));

    public static void register() {
        registerBlock("atm", ATM);
        registerBlock("loot_crate", LOOT_CRATE);
    }

    private static void registerBlock(String name, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
