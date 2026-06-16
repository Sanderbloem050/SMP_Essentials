package com.sanderbloem.currencymod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.currencymod.data.WalletData;
import com.sanderbloem.currencymod.economy.AuctionData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AuctionCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {

        d.register(Commands.literal("ah")
                // /ah  -> overzicht
                .executes(AuctionCommands::list)

                // /ah sell <prijs>
                .then(Commands.literal("sell")
                        .then(Commands.argument("prijs", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    if (!com.sanderbloem.currencymod.config.ModConfig.get(server(p)).auctionEnabled) {
                                        ctx.getSource().sendFailure(Component.literal("§7Het veilinghuis is momenteel uitgeschakeld."));
                                        return 0;
                                    }
                                    long price = LongArgumentType.getLong(ctx, "prijs");
                                    ItemStack hand = p.getMainHandItem();
                                    if (hand.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("§cHoud het item vast dat je wilt verkopen."));
                                        return 0;
                                    }
                                    ItemStack toSell = hand.copy();
                                    hand.setCount(0);
                                    int id = AuctionData.get(server(p)).add(p.getUUID(), p.getName().getString(), toSell, price);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aTe koop gezet §7(#" + id + ")§a: §f"
                                            + toSell.getCount() + "× " + toSell.getHoverName().getString()
                                            + " §7voor §6" + WalletData.formatBalance(price)), false);
                                    return 1;
                                })))

                // /ah buy <id>
                .then(Commands.literal("buy")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> buy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))

                // /ah cancel <id>
                .then(Commands.literal("cancel")
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> cancel(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id")))))

                // /ah mine
                .then(Commands.literal("mine").executes(AuctionCommands::mine)));
    }

    private static int list(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        List<AuctionData.Listing> all = new java.util.ArrayList<>(AuctionData.get(server(p)).all());
        if (all.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7Er staat niets te koop. Gebruik §f/ah sell <prijs>§7."), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6=== Veilinghuis §7(" + all.size() + ") ==="), false);
        all.sort((a, b) -> Integer.compare(a.id(), b.id()));
        for (AuctionData.Listing l : all) {
            ctx.getSource().sendSuccess(() -> line(l, "§7[§aKoop§7]", "/ah buy " + l.id()), false);
        }
        return 1;
    }

    private static int mine(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        List<AuctionData.Listing> mine = AuctionData.get(server(p)).all().stream()
                .filter(l -> l.seller().equals(p.getUUID())).sorted((a, b) -> Integer.compare(a.id(), b.id())).toList();
        if (mine.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("§7Je hebt niets te koop staan."), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("§6=== Jouw aanbod ==="), false);
        for (AuctionData.Listing l : mine) {
            ctx.getSource().sendSuccess(() -> line(l, "§7[§cAnnuleer§7]", "/ah cancel " + l.id()), false);
        }
        return 1;
    }

    private static int buy(CommandSourceStack src, int id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!com.sanderbloem.currencymod.config.ModConfig.get(server(p)).auctionEnabled) {
            src.sendFailure(Component.literal("§7Het veilinghuis is momenteel uitgeschakeld."));
            return 0;
        }
        AuctionData data = AuctionData.get(server(p));
        AuctionData.Listing l = data.get(id);
        if (l == null) { src.sendFailure(Component.literal("§cDat aanbod bestaat niet (meer).")); return 0; }
        if (l.seller().equals(p.getUUID())) { src.sendFailure(Component.literal("§cDit is je eigen aanbod — gebruik /ah cancel " + id)); return 0; }

        WalletData wallet = WalletData.get(server(p));
        if (!wallet.subtractBalance(p.getUUID(), l.price())) {
            src.sendFailure(Component.literal("§cOnvoldoende saldo. Nodig: §6" + WalletData.formatBalance(l.price())));
            return 0;
        }
        wallet.addBalance(l.seller(), l.price());
        data.remove(id);

        ItemStack item = l.item().copy();
        if (!p.getInventory().add(item)) p.drop(item, false);
        src.sendSuccess(() -> Component.literal("§aGekocht: §f" + l.item().getCount() + "× " + l.item().getHoverName().getString()
                + " §7voor §6" + WalletData.formatBalance(l.price())), false);

        ServerPlayer seller = server(p).getPlayerList().getPlayer(l.seller());
        if (seller != null) seller.sendSystemMessage(Component.literal("§a" + p.getName().getString()
                + " kocht je §f" + l.item().getHoverName().getString() + " §avoor §6" + WalletData.formatBalance(l.price())));
        return 1;
    }

    private static int cancel(CommandSourceStack src, int id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        AuctionData data = AuctionData.get(server(p));
        AuctionData.Listing l = data.get(id);
        if (l == null || !l.seller().equals(p.getUUID())) {
            src.sendFailure(Component.literal("§cDat is niet jouw aanbod."));
            return 0;
        }
        data.remove(id);
        ItemStack item = l.item().copy();
        if (!p.getInventory().add(item)) p.drop(item, false);
        src.sendSuccess(() -> Component.literal("§aAanbod geannuleerd, item teruggegeven."), false);
        return 1;
    }

    private static MutableComponent line(AuctionData.Listing l, String btn, String command) {
        MutableComponent button = Component.literal(" " + btn)
                .withStyle(s -> s.withClickEvent(new ClickEvent.RunCommand(command)));
        return Component.literal("§e#" + l.id() + " §f" + l.item().getCount() + "× " + l.item().getHoverName().getString()
                + " §7— §6" + WalletData.formatBalance(l.price()) + " §7van §f" + l.sellerName()).append(button);
    }

    private static MinecraftServer server(ServerPlayer p) {
        return p.level().getServer();
    }
}
