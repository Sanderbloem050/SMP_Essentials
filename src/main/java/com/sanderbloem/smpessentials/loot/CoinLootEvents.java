package com.sanderbloem.smpessentials.loot;

import com.sanderbloem.smpessentials.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;

public class CoinLootEvents {

    private static final List<String> CHEST_TABLES = List.of(
            "minecraft:chests/simple_dungeon",
            "minecraft:chests/stronghold_corridor",
            "minecraft:chests/stronghold_crossing",
            "minecraft:chests/abandoned_mineshaft",
            "minecraft:chests/bastion_treasure",
            "minecraft:chests/end_city_treasure",
            "minecraft:chests/nether_bridge"
    );

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            String tableId = key.identifier().toString();
            if (!CHEST_TABLES.contains(tableId)) return;

            tableBuilder.withPool(LootPool.lootPool()
                    .add(LootItem.lootTableItem(ModItems.COPPER_COIN)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 8)))
                            .when(LootItemRandomChanceCondition.randomChance(0.6f)))
                    .add(LootItem.lootTableItem(ModItems.SILVER_COIN)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))
                            .when(LootItemRandomChanceCondition.randomChance(0.25f)))
                    .add(LootItem.lootTableItem(ModItems.GOLD_COIN)
                            .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1)))
                            .when(LootItemRandomChanceCondition.randomChance(0.15f))));
        });
    }
}
