package com.sanderbloem.currencymod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sanderbloem.currencymod.claims.ClaimsData;
import com.sanderbloem.currencymod.config.ModConfig;
import com.sanderbloem.currencymod.data.WalletData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class ClaimCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {

        d.register(Commands.literal("claim")
            .then(Commands.literal("show").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                boolean on = com.sanderbloem.currencymod.claims.ClaimBorders.toggle(p.getUUID());
                ctx.getSource().sendSuccess(() -> Component.literal(on
                        ? "§aClaim-randen §2AAN §7(groen = van jou, rood = anderen)"
                        : "§7Claim-randen uit."), false);
                return 1;
            }))
            .executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            String key = ClaimsData.key(p.level(), p.blockPosition());
            ClaimsData data = ClaimsData.get(server(p));
            UUID owner = data.ownerOf(key);
            if (owner != null) {
                ctx.getSource().sendFailure(Component.literal(
                        owner.equals(p.getUUID()) ? "§cDeze chunk is al van jou." : "§cDeze chunk is al geclaimd."));
                return 0;
            }
            ModConfig cfg = ModConfig.get(server(p));
            int max = cfg.maxClaimsPerPlayer;
            if (max > 0 && data.countClaims(p.getUUID()) >= max) {
                ctx.getSource().sendFailure(Component.literal(
                        "§cJe hebt het maximum aantal claims bereikt (§f" + max + "§c)."));
                return 0;
            }
            long cost = cfg.claimCostPerChunk;
            if (cost > 0) {
                WalletData wallet = WalletData.get(server(p));
                if (!wallet.subtractBalance(p.getUUID(), cost)) {
                    ctx.getSource().sendFailure(Component.literal(
                            "§cOnvoldoende saldo. Een chunk claimen kost §6"
                            + WalletData.formatBalance(cost)
                            + "§c. Jij hebt §6" + WalletData.formatBalance(wallet.getBalance(p.getUUID())) + "§c."));
                    return 0;
                }
            }
            data.claim(key, p.getUUID());
            String costMsg = cost > 0 ? " §7(kosten: §6" + WalletData.formatBalance(cost) + "§7)" : "";
            ctx.getSource().sendSuccess(() -> Component.literal("§aChunk geclaimd!" + costMsg), false);
            return 1;
        }));

        d.register(Commands.literal("unclaim").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            String key = ClaimsData.key(p.level(), p.blockPosition());
            boolean ok = ClaimsData.get(server(p)).unclaim(key, p.getUUID());
            if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§aClaim verwijderd."), false);
            else ctx.getSource().sendFailure(Component.literal("§cDeze chunk is niet van jou."));
            return ok ? 1 : 0;
        }));

        d.register(Commands.literal("claims").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            long n = ClaimsData.get(server(p)).countClaims(p.getUUID());
            ctx.getSource().sendSuccess(() -> Component.literal("§6Je hebt §f" + n + " §6chunk(s) geclaimd."), false);
            return 1;
        }));

        d.register(Commands.literal("trust")
                .then(Commands.argument("speler", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "speler");
                            ClaimsData.get(server(p)).trust(p.getUUID(), target.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "§a" + target.getName().getString() + " mag nu bouwen in jouw claims."), false);
                            return 1;
                        })));

        d.register(Commands.literal("untrust")
                .then(Commands.argument("speler", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "speler");
                            boolean ok = ClaimsData.get(server(p)).untrust(p.getUUID(), target.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(ok
                                    ? "§a" + target.getName().getString() + " mag niet meer bouwen in jouw claims."
                                    : "§7Die speler had geen toegang."), false);
                            return 1;
                        })));
    }

    private static MinecraftServer server(ServerPlayer p) {
        return p.level().getServer();
    }
}
