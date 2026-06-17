package com.sanderbloem.smpessentials;

import com.sanderbloem.smpessentials.block.AtmBlock;
import com.sanderbloem.smpessentials.block.AuctionTerminalBlock;
import com.sanderbloem.smpessentials.block.BountyBoardBlock;
import com.sanderbloem.smpessentials.block.CrateType;
import com.sanderbloem.smpessentials.block.LootCrateBlock;
import com.sanderbloem.smpessentials.block.QuestBoardBlock;
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

    public static final Block ATM = new AtmBlock(props("atm", 3.0f, 6.0f));

    public static final Block BOUNTY_BOARD = new BountyBoardBlock(props("bounty_board", 2.5f, 4.0f));

    public static final Block LOOT_CRATE = new LootCrateBlock(props("loot_crate", 2.0f, 4.0f), CrateType.BRONZE);

    public static final Block BRONZE_CRATE = new LootCrateBlock(props("bronze_crate", 2.0f, 4.0f), CrateType.BRONZE);
    public static final Block SILVER_CRATE = new LootCrateBlock(props("silver_crate", 2.5f, 5.0f), CrateType.SILVER);
    public static final Block GOLD_CRATE   = new LootCrateBlock(props("gold_crate",   3.0f, 6.0f), CrateType.GOLD);

    public static final Block AUCTION_TERMINAL = new AuctionTerminalBlock(props("auction_terminal", 3.0f, 6.0f));

    public static final Block QUEST_BOARD = new QuestBoardBlock(props("quest_board", 2.5f, 4.0f));

    public static void register() {
        registerBlock("atm", ATM);
        registerBlock("bounty_board", BOUNTY_BOARD);
        registerBlock("loot_crate", LOOT_CRATE);
        registerBlock("bronze_crate", BRONZE_CRATE);
        registerBlock("silver_crate", SILVER_CRATE);
        registerBlock("gold_crate", GOLD_CRATE);
        registerBlock("auction_terminal", AUCTION_TERMINAL);
        registerBlock("quest_board", QUEST_BOARD);
    }

    private static BlockBehaviour.Properties props(String name, float hardness, float resistance) {
        return BlockBehaviour.Properties.of()
                .strength(hardness, resistance)
                .setId(ResourceKey.create(Registries.BLOCK,
                        Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, name)));
    }

    private static void registerBlock(String name, Block block) {
        Identifier id = Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, id))));
    }
}
