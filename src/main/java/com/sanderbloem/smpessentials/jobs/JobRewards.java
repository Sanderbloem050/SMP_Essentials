package com.sanderbloem.smpessentials.jobs;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** Bepaalt hoeveel koper een gebroken blok oplevert (mining/farming jobs). */
public class JobRewards {

    private static final Map<Block, Long> ORE = new HashMap<>();
    private static final long CROP_REWARD = 1; // per rijp gewas

    static {
        put(2, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.NETHER_QUARTZ_ORE);
        put(3, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.NETHER_GOLD_ORE);
        put(5, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE);
        put(8, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        put(10, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        put(25, Blocks.ANCIENT_DEBRIS);
    }

    private static void put(long value, Block... blocks) {
        for (Block b : blocks) ORE.put(b, value);
    }

    public static long rewardFor(BlockState state) {
        Block b = state.getBlock();
        if (b instanceof CropBlock crop) {
            return crop.isMaxAge(state) ? CROP_REWARD : 0L;
        }
        return ORE.getOrDefault(b, 0L);
    }
}
