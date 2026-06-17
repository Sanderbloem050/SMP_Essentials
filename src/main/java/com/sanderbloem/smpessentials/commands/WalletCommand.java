package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.smpessentials.ModItems;
import com.sanderbloem.smpessentials.data.WalletData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class WalletCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("balance")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WalletData data = WalletData.get(player.level().getServer());
                    long balance = data.getBalance(player.getUUID());
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Wallet: " + WalletData.formatBalance(balance)), false);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer sender = ctx.getSource().getPlayerOrException();
                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
                                    long amount = LongArgumentType.getLong(ctx, "amount");

                                    if (sender.getUUID().equals(target.getUUID())) {
                                        ctx.getSource().sendFailure(Component.literal("Je kunt jezelf niet betalen."));
                                        return 0;
                                    }

                                    WalletData data = WalletData.get(sender.level().getServer());
                                    if (!data.subtractBalance(sender.getUUID(), amount)) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "Onvoldoende saldo. Je hebt " + WalletData.formatBalance(data.getBalance(sender.getUUID()))));
                                        return 0;
                                    }

                                    data.addBalance(target.getUUID(), amount);
                                    String fmt = WalletData.formatBalance(amount);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Verstuurd: " + fmt + " naar " + target.getName().getString()), false);
                                    target.sendSystemMessage(Component.literal(sender.getName().getString() + " heeft je " + fmt + " gestuurd."));
                                    return 1;
                                })
                        ))
        );

        dispatcher.register(Commands.literal("deposit")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WalletData data = WalletData.get(player.level().getServer());
                    long total = collectCoins(player);
                    if (total == 0) {
                        ctx.getSource().sendFailure(Component.literal("Geen munten in je inventory."));
                        return 0;
                    }
                    data.addBalance(player.getUUID(), total);
                    ctx.getSource().sendSuccess(
                            () -> Component.literal("Gestort: " + WalletData.formatBalance(total)), false);
                    return 1;
                })
        );

        dispatcher.register(Commands.literal("withdraw")
                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            long amount = LongArgumentType.getLong(ctx, "amount");
                            WalletData data = WalletData.get(player.level().getServer());
                            if (!data.subtractBalance(player.getUUID(), amount)) {
                                ctx.getSource().sendFailure(Component.literal(
                                        "Onvoldoende saldo. Je hebt " + WalletData.formatBalance(data.getBalance(player.getUUID()))));
                                return 0;
                            }
                            giveCoins(player, amount);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Opgenomen: " + WalletData.formatBalance(amount)), false);
                            return 1;
                        })
                )
        );
    }

    private static long collectCoins(ServerPlayer player) {
        long total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.COPPER_COIN)) {
                total += stack.getCount();
                stack.setCount(0);
            } else if (stack.is(ModItems.SILVER_COIN)) {
                total += (long) stack.getCount() * WalletData.SILVER_VALUE;
                stack.setCount(0);
            } else if (stack.is(ModItems.GOLD_COIN)) {
                total += (long) stack.getCount() * WalletData.GOLD_VALUE;
                stack.setCount(0);
            }
        }
        return total;
    }

    private static void giveCoins(ServerPlayer player, long copper) {
        long gold   = copper / WalletData.GOLD_VALUE;   copper %= WalletData.GOLD_VALUE;
        long silver = copper / WalletData.SILVER_VALUE; copper %= WalletData.SILVER_VALUE;
        giveStacks(player, ModItems.GOLD_COIN,   gold);
        giveStacks(player, ModItems.SILVER_COIN, silver);
        giveStacks(player, ModItems.COPPER_COIN, copper);
    }

    private static void giveStacks(ServerPlayer player, net.minecraft.world.item.Item item, long count) {
        while (count > 0) {
            int batch = (int) Math.min(count, 64);
            ItemStack stack = new ItemStack(item, batch);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
            count -= batch;
        }
    }
}
