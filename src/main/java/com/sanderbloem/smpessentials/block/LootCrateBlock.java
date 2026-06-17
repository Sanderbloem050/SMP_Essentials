package com.sanderbloem.smpessentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class LootCrateBlock extends Block {

    private final CrateType tier;

    public LootCrateBlock(Properties properties, CrateType tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(tier.key())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide() && level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
            if (!com.sanderbloem.smpessentials.config.ModConfig.get(sl.getServer()).cratesEnabled) {
                sp.sendSystemMessage(Component.literal("§7Loot crates zijn momenteel uitgeschakeld."));
                return InteractionResult.SUCCESS;
            }
            stack.shrink(1);
            List<ItemStack> rewards = tier.roll(sl.getRandom());

            StringBuilder sb = new StringBuilder(tier.color() + tierName() + " Crate geopend! §7Je kreeg: §f");
            for (int i = 0; i < rewards.size(); i++) {
                ItemStack r = rewards.get(i);
                sb.append(r.getCount()).append("× ").append(r.getHoverName().getString());
                if (i < rewards.size() - 1) sb.append("§7, §f");
            }

            for (ItemStack r : rewards) {
                if (!sp.getInventory().add(r)) sp.drop(r, false);
            }

            sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.7f, tierPitch());
            sl.sendParticles(tier.particles(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    20, 0.5, 0.5, 0.5, 0.1);
            sp.sendSystemMessage(Component.literal(sb.toString()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.literal(
                    "§eJe hebt een §6" + tierName() + " Key §enodig — rechtsklik met de sleutel."));
        }
        return InteractionResult.SUCCESS;
    }

    private String tierName() {
        return switch (tier) {
            case BRONZE -> "Bronze";
            case SILVER -> "Silver";
            case GOLD   -> "Gold";
        };
    }

    private float tierPitch() {
        return switch (tier) {
            case BRONZE -> 1.1f;
            case SILVER -> 1.3f;
            case GOLD   -> 1.6f;
        };
    }
}
