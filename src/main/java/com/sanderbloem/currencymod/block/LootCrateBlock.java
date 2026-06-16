package com.sanderbloem.currencymod.block;

import com.sanderbloem.currencymod.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

public class LootCrateBlock extends Block {

    public LootCrateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.CRATE_KEY)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND; // val door naar useWithoutItem (hint)
        }
        if (!level.isClientSide() && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (!com.sanderbloem.currencymod.config.ModConfig.get(sl.getServer()).cratesEnabled) {
                sp.sendSystemMessage(Component.literal("§7Loot crates zijn momenteel uitgeschakeld."));
                return InteractionResult.SUCCESS;
            }
            stack.shrink(1); // sleutel verbruiken
            List<ItemStack> rewards = roll(sl.getRandom());

            // Melding eerst opbouwen (add() leegt de stacks!)
            StringBuilder sb = new StringBuilder("§6Crate geopend! §7Je kreeg: §f");
            for (int i = 0; i < rewards.size(); i++) {
                ItemStack r = rewards.get(i);
                sb.append(r.getCount()).append("× ").append(r.getHoverName().getString());
                if (i < rewards.size() - 1) sb.append("§7, §f");
            }

            for (ItemStack r : rewards) {
                if (!sp.getInventory().add(r)) sp.drop(r, false);
            }
            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, 1.4f);
            sp.sendSystemMessage(Component.literal(sb.toString()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal("§eJe hebt een §6Crate Key §enodig — rechtsklik met de sleutel."));
        }
        return InteractionResult.SUCCESS;
    }

    /** Rolt 2 willekeurige beloningen uit een gewogen pool. */
    private static List<ItemStack> roll(RandomSource rng) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            int r = rng.nextInt(100);
            if (r < 35)      out.add(new ItemStack(ModItems.GOLD_COIN, 1 + rng.nextInt(2)));
            else if (r < 60) out.add(new ItemStack(ModItems.SILVER_COIN, 2 + rng.nextInt(4)));
            else if (r < 75) out.add(new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1 + rng.nextInt(3)));
            else if (r < 88) out.add(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 4 + rng.nextInt(9)));
            else if (r < 95) out.add(new ItemStack(net.minecraft.world.item.Items.EMERALD, 1 + rng.nextInt(3)));
            else if (r < 99) out.add(new ItemStack(ModItems.GOLD_COIN, 5 + rng.nextInt(5)));
            else             out.add(new ItemStack(net.minecraft.world.item.Items.NETHERITE_SCRAP, 1));
        }
        return out;
    }
}
