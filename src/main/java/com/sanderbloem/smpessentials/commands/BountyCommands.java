package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.economy.BountyData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class BountyCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {

        d.register(Commands.literal("bounty")

                .then(Commands.literal("set")
                        .then(Commands.argument("speler", EntityArgument.player())
                                .then(Commands.argument("bedrag", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                                            if (!com.sanderbloem.smpessentials.config.ModConfig.get(server(p)).bountiesEnabled) {
                                                ctx.getSource().sendFailure(Component.literal("§7Bounties zijn momenteel uitgeschakeld."));
                                                return 0;
                                            }
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "speler");
                                            long amount = LongArgumentType.getLong(ctx, "bedrag");

                                            if (target.getUUID().equals(p.getUUID())) {
                                                ctx.getSource().sendFailure(Component.literal("§cJe kunt geen bounty op jezelf zetten."));
                                                return 0;
                                            }
                                            WalletData wallet = WalletData.get(server(p));
                                            if (!wallet.subtractBalance(p.getUUID(), amount)) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "§cOnvoldoende saldo. Je hebt " + WalletData.formatBalance(wallet.getBalance(p.getUUID()))));
                                                return 0;
                                            }
                                            BountyData.get(server(p)).add(target.getUUID(), amount);
                                            long total = BountyData.get(server(p)).get(target.getUUID());

                                            String msg = "§6" + p.getName().getString() + " §ezette een bounty van §6"
                                                    + WalletData.formatBalance(amount) + " §eop §c" + target.getName().getString()
                                                    + "§e! §7(totaal: " + WalletData.formatBalance(total) + ")";
                                            server(p).getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
                                            return 1;
                                        }))))

                .then(Commands.literal("list").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Map<java.util.UUID, Long> all = BountyData.get(server(p)).all();
                    if (all.isEmpty()) {
                        ctx.getSource().sendSuccess(() -> Component.literal("§7Er staan geen bounties open."), false);
                        return 1;
                    }
                    List<Map.Entry<java.util.UUID, Long>> sorted = all.entrySet().stream()
                            .sorted(Comparator.comparingLong((Map.Entry<java.util.UUID, Long> e) -> e.getValue()).reversed())
                            .toList();
                    ctx.getSource().sendSuccess(() -> Component.literal("§6=== Open bounties ==="), false);
                    for (var e : sorted) {
                        String name = nameOf(server(p), e.getKey());
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§c" + name + " §7— §6" + WalletData.formatBalance(e.getValue())), false);
                    }
                    return 1;
                }))
        );
    }

    private static String nameOf(MinecraftServer server, java.util.UUID id) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        return p != null ? p.getName().getString() : id.toString().substring(0, 8);
    }

    private static MinecraftServer server(ServerPlayer p) {
        return p.level().getServer();
    }
}
