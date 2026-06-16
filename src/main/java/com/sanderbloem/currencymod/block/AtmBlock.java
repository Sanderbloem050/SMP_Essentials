package com.sanderbloem.currencymod.block;

import com.sanderbloem.currencymod.menu.AtmMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AtmBlock extends Block {

    public AtmBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new AtmMenu(id, inv),
                    Component.literal("§6Bank")));
        }
        return InteractionResult.SUCCESS;
    }
}
