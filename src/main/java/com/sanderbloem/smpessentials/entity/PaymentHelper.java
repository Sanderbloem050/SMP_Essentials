package com.sanderbloem.smpessentials.entity;

import com.sanderbloem.smpessentials.ModItems;
import com.sanderbloem.smpessentials.data.WalletData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class PaymentHelper {

    public static long countPhysicalCoins(Player player) {
        long total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.COPPER_COIN)) total += stack.getCount();
            else if (stack.is(ModItems.SILVER_COIN)) total += stack.getCount() * WalletData.SILVER_VALUE;
            else if (stack.is(ModItems.GOLD_COIN))   total += stack.getCount() * WalletData.GOLD_VALUE;
        }
        return total;
    }

    public static boolean processPayment(ServerPlayer player, long amount, WalletData wallet) {
        long physical = countPhysicalCoins(player);
        long walletBal = wallet.getBalance(player.getUUID());

        if (physical + walletBal < amount) return false;

        long remaining = amount;

        if (physical > 0 && remaining > 0) {
            remaining = deductPhysical(player, remaining);
        }

        if (remaining > 0) {
            if (!wallet.subtractBalance(player.getUUID(), remaining)) return false;
        }

        return true;
    }

    private static long deductPhysical(ServerPlayer player, long needed) {
        long[] coinValues = {WalletData.GOLD_VALUE, WalletData.SILVER_VALUE, 1L};
        net.minecraft.world.item.Item[] coinItems = {ModItems.GOLD_COIN, ModItems.SILVER_COIN, ModItems.COPPER_COIN};

        long remaining = needed;
        for (int ci = 0; ci < coinValues.length && remaining > 0; ci++) {
            long val = coinValues[ci];
            for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.is(coinItems[ci])) continue;
                long canTake = Math.min(stack.getCount(), (remaining + val - 1) / val);
                long taken = Math.min(canTake, stack.getCount());
                stack.shrink((int) taken);
                remaining -= taken * val;
            }
        }

        if (remaining < 0) {
            giveChange(player, -remaining);
        }

        return Math.max(0, remaining);
    }

    private static void giveChange(ServerPlayer player, long copper) {
        long goldCount   = copper / WalletData.GOLD_VALUE;
        copper %= WalletData.GOLD_VALUE;
        long silverCount = copper / WalletData.SILVER_VALUE;
        long copperCount = copper % WalletData.SILVER_VALUE;

        giveStack(player, ModItems.GOLD_COIN,   goldCount);
        giveStack(player, ModItems.SILVER_COIN, silverCount);
        giveStack(player, ModItems.COPPER_COIN, copperCount);
    }

    private static void giveStack(ServerPlayer player, net.minecraft.world.item.Item item, long count) {
        while (count > 0) {
            int batch = (int) Math.min(count, 64);
            ItemStack stack = new ItemStack(item, batch);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            count -= batch;
        }
    }
}
