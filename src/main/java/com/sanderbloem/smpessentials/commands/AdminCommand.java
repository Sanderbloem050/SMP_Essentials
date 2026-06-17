package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.smpessentials.config.ModConfig;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.menu.ConfigMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class AdminCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("smpadmin")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))

                // /smpadmin — open config GUI
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.openMenu(new SimpleMenuProvider(
                            (id, inv, pl) -> new ConfigMenu(id, inv),
                            Component.literal("§6SMP Essentials — Instellingen")));
                    return 1;
                })

                // /smpadmin claims cost <bedrag>
                .then(Commands.literal("claims")
                        .then(Commands.literal("cost")
                                .then(Commands.argument("bedrag", LongArgumentType.longArg(0))
                                        .executes(ctx -> {
                                            long cost = LongArgumentType.getLong(ctx, "bedrag");
                                            ModConfig cfg = ModConfig.get(ctx.getSource().getServer());
                                            cfg.claimCostPerChunk = cost;
                                            cfg.setDirty();
                                            String msg = cost == 0
                                                    ? "§aClaim-kosten uitgeschakeld (gratis)."
                                                    : "§aClaim-kosten ingesteld op §6" + WalletData.formatBalance(cost) + "§a per chunk.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
                                            return 1;
                                        })))
                        // /smpadmin claims max <aantal>
                        .then(Commands.literal("max")
                                .then(Commands.argument("aantal", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            int max = IntegerArgumentType.getInteger(ctx, "aantal");
                                            ModConfig cfg = ModConfig.get(ctx.getSource().getServer());
                                            cfg.maxClaimsPerPlayer = max;
                                            cfg.setDirty();
                                            String msg = max == 0
                                                    ? "§aMaximum claims uitgeschakeld (onbeperkt)."
                                                    : "§aMaximum claims per speler ingesteld op §f" + max + "§a.";
                                            ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
                                            return 1;
                                        })))
                        // /smpadmin claims info
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    ModConfig cfg = ModConfig.get(ctx.getSource().getServer());
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§6Claim-instellingen: §7kosten=" + WalletData.formatBalance(cfg.claimCostPerChunk)
                                            + " · max=" + (cfg.maxClaimsPerPlayer == 0 ? "onbeperkt" : cfg.maxClaimsPerPlayer)), false);
                                    return 1;
                                })))
        );
    }
}
