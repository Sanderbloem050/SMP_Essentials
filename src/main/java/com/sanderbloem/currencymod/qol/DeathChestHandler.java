package com.sanderbloem.currencymod.qol;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public class DeathChestHandler {

    /** True = sterven mag doorgaan (vanilla drops gebeuren niet, wij regelen het zelf). */
    public static boolean onAllowDeath(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof ServerPlayer player)) return true;
        if (player.level().isClientSide()) return true;
        if (!com.sanderbloem.currencymod.config.ModConfig.get(player.level().getServer()).deathChestEnabled) return true;

        ServerLevel level = player.level();
        NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
        boolean hasItems = items.stream().anyMatch(s -> !s.isEmpty());

        if (hasItems) {
            BlockPos pos = findFreeSpot(level, player.blockPosition());
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ChestBlockEntity chest) {
                int slot = 0;
                for (ItemStack stack : items) {
                    if (stack.isEmpty()) continue;
                    if (slot >= chest.getContainerSize()) break;
                    chest.setItem(slot++, stack.copy());
                }
            }
            player.getInventory().clearContent();
            BackManager.setDeath(player.getUUID(), Location.of(player));
            player.sendSystemMessage(Component.literal(
                    "§cJe bent gestorven! §7Je spullen liggen veilig in een kist. Gebruik §f/back §7om terug te gaan."));
        }
        return true;
    }

    private static BlockPos findFreeSpot(ServerLevel level, BlockPos origin) {
        if (level.getBlockState(origin).isAir()) return origin;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos p = origin.above(dy);
            if (level.getBlockState(p).isAir()) return p;
        }
        return origin;
    }
}
