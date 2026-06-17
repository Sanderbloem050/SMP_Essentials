package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.entity.ModEntities;
import com.sanderbloem.smpessentials.entity.PaymentHelper;
import com.sanderbloem.smpessentials.entity.ShopTrade;
import com.sanderbloem.smpessentials.entity.ShopkeeperEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ShopkeeperCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("shopkeeper")

                .then(Commands.literal("spawn")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ServerLevel world = ctx.getSource().getLevel();

                            net.minecraft.world.phys.HitResult hit = player.pick(20.0, 1.0f, false);
                            if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                                ctx.getSource().sendFailure(Component.literal("§cKijk naar een blok (bijv. een kist) om de winkel te plaatsen."));
                                return 0;
                            }
                            net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
                            net.minecraft.core.BlockPos spawnPos = blockHit.getBlockPos().relative(blockHit.getDirection());

                            ShopkeeperEntity npc = new ShopkeeperEntity(ModEntities.SHOPKEEPER, world);
                            npc.snapTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                                    player.getYRot() + 180.0f, 0.0f);
                            npc.setOwnerUUID(player.getUUID());
                            world.addFreshEntity(npc);
                            ctx.getSource().sendSuccess(() -> Component.literal("§aShopkeeper geplaatst!"), false);
                            return 1;
                        }))

                .then(Commands.literal("addtrade")
                        .then(Commands.argument("prijs", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    long price = LongArgumentType.getLong(ctx, "prijs");
                                    ShopkeeperEntity npc = findNearestOwnedShop(player);
                                    if (npc == null) {
                                        ctx.getSource().sendFailure(Component.literal("Geen jouw winkel in de buurt."));
                                        return 0;
                                    }
                                    var hand = player.getMainHandItem();
                                    if (hand.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("Houd het item vast dat je wilt verkopen."));
                                        return 0;
                                    }
                                    npc.addTrade(new ShopTrade(hand, price, com.sanderbloem.smpessentials.entity.ShopTradeMode.SELL_TO_PLAYER));
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aArtikel toegevoegd: §f" + hand.getHoverName().getString() +
                                                    " §avoor §e" + WalletData.formatBalance(price)), false);
                                    return 1;
                                })))

                .then(Commands.literal("removetrade")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    ShopkeeperEntity npc = findNearestOwnedShop(player);
                                    if (npc == null) {
                                        ctx.getSource().sendFailure(Component.literal("Geen jouw winkel in de buurt."));
                                        return 0;
                                    }
                                    if (!npc.removeTrade(index)) {
                                        ctx.getSource().sendFailure(Component.literal("Ongeldig index."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aArtikel verwijderd."), false);
                                    return 1;
                                })))

                .then(Commands.literal("setname")
                        .then(Commands.argument("naam", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String naam = StringArgumentType.getString(ctx, "naam");
                                    ShopkeeperEntity npc = findNearestOwnedShop(player);
                                    if (npc == null) {
                                        ctx.getSource().sendFailure(Component.literal("Geen jouw winkel in de buurt."));
                                        return 0;
                                    }
                                    npc.setCustomName(Component.literal(naam));
                                    npc.setCustomNameVisible(true);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aNaam ingesteld: §f" + naam), false);
                                    return 1;
                                })))

                .then(Commands.literal("buy")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int index = IntegerArgumentType.getInteger(ctx, "index");
                                    ShopkeeperEntity npc = findNearestShop(player, 5.0);
                                    if (npc == null) {
                                        ctx.getSource().sendFailure(Component.literal("Geen winkel in de buurt."));
                                        return 0;
                                    }
                                    List<ShopTrade> trades = npc.getTrades();
                                    if (index >= trades.size()) {
                                        ctx.getSource().sendFailure(Component.literal("Ongeldig index."));
                                        return 0;
                                    }
                                    ShopTrade trade = trades.get(index);
                                    WalletData wallet = WalletData.get(player.level().getServer());

                                    long physical = PaymentHelper.countPhysicalCoins(player);
                                    long walletBal = wallet.getBalance(player.getUUID());

                                    if (physical + walletBal < trade.getPrice()) {
                                        ctx.getSource().sendFailure(Component.literal(
                                                "§cOnvoldoende saldo. Je mist §e" +
                                                        WalletData.formatBalance(trade.getPrice() - physical - walletBal)));
                                        return 0;
                                    }

                                    if (!PaymentHelper.processPayment(player, trade.getPrice(), wallet)) {
                                        ctx.getSource().sendFailure(Component.literal("§cBetaling mislukt."));
                                        return 0;
                                    }

                                    if (!player.getInventory().add(trade.getItem())) {
                                        player.drop(trade.getItem(), false);
                                    }

                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aGekocht: §f" + trade.getItem().getHoverName().getString() +
                                                    " §avoor §e" + WalletData.formatBalance(trade.getPrice())), false);
                                    return 1;
                                })))

                .then(Commands.literal("remove")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ShopkeeperEntity npc = findNearestOwnedShop(player);
                            if (npc == null) {
                                ctx.getSource().sendFailure(Component.literal("Geen jouw winkel in de buurt."));
                                return 0;
                            }
                            npc.remove(Entity.RemovalReason.DISCARDED);
                            ctx.getSource().sendSuccess(() -> Component.literal("§aWinkel verwijderd."), false);
                            return 1;
                        }))
        );
    }

    private static ShopkeeperEntity findNearestOwnedShop(ServerPlayer player) {
        boolean isOp = Commands.LEVEL_GAMEMASTERS.check(player.permissions());
        return player.level()
                .getEntitiesOfClass(ShopkeeperEntity.class,
                        AABB.ofSize(player.position(), 10, 10, 10),
                        e -> player.getUUID().equals(e.getOwnerUUID()) || isOp)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
    }

    private static ShopkeeperEntity findNearestShop(ServerPlayer player, double range) {
        return player.level()
                .getEntitiesOfClass(ShopkeeperEntity.class,
                        AABB.ofSize(player.position(), range * 2, range * 2, range * 2),
                        e -> true)
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
    }
}
