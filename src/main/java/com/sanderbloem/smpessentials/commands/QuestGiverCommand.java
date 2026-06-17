package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.entity.ModEntities;
import com.sanderbloem.smpessentials.entity.QuestGiverEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class QuestGiverCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("questgiver")

                .then(Commands.literal("spawn").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel world = ctx.getSource().getLevel();

                    HitResult hit = player.pick(20.0, 1.0f, false);
                    if (hit.getType() != HitResult.Type.BLOCK) {
                        ctx.getSource().sendFailure(Component.literal("§cKijk naar een blok om de quest-NPC te plaatsen."));
                        return 0;
                    }
                    BlockHitResult bh = (BlockHitResult) hit;
                    var pos = bh.getBlockPos().relative(bh.getDirection());

                    QuestGiverEntity npc = new QuestGiverEntity(ModEntities.QUEST_GIVER, world);
                    npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
                    npc.setOwnerUUID(player.getUUID());
                    world.addFreshEntity(npc);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aQuest-NPC geplaatst!"), false);
                    return 1;
                }))

                .then(Commands.literal("setitem")
                        .then(Commands.argument("aantal", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "aantal");
                                    QuestGiverEntity npc = findNearestOwned(player);
                                    if (npc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen jouw quest-NPC in de buurt.")); return 0; }
                                    ItemStack hand = player.getMainHandItem();
                                    if (hand.isEmpty()) { ctx.getSource().sendFailure(Component.literal("§cHoud het vereiste item vast.")); return 0; }
                                    ItemStack template = hand.copy();
                                    template.setCount(amount);
                                    npc.setQuest(template);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aQuest-item ingesteld: §f" + amount + "× " + hand.getHoverName().getString()), false);
                                    return 1;
                                })))

                .then(Commands.literal("setreward")
                        .then(Commands.argument("bedrag", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    long amount = LongArgumentType.getLong(ctx, "bedrag");
                                    QuestGiverEntity npc = findNearestOwned(player);
                                    if (npc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen jouw quest-NPC in de buurt.")); return 0; }
                                    npc.setReward(amount);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aBeloning ingesteld: §6" + WalletData.formatBalance(amount)), false);
                                    return 1;
                                })))

                .then(Commands.literal("toggleonce").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    QuestGiverEntity npc = findNearestOwned(player);
                    if (npc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen jouw quest-NPC in de buurt.")); return 0; }
                    npc.toggleOnce();
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aEenmalig: §f" + (npc.isOnceOnly() ? "ja" : "nee")), false);
                    return 1;
                }))

                .then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    QuestGiverEntity npc = findNearestOwned(player);
                    if (npc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen jouw quest-NPC in de buurt.")); return 0; }
                    npc.remove(Entity.RemovalReason.DISCARDED);
                    ctx.getSource().sendSuccess(() -> Component.literal("§aQuest-NPC verwijderd."), false);
                    return 1;
                }))
        );
    }

    private static QuestGiverEntity findNearestOwned(ServerPlayer player) {
        return player.level()
                .getEntitiesOfClass(QuestGiverEntity.class,
                        AABB.ofSize(player.position(), 10, 10, 10),
                        e -> player.getUUID().equals(e.getOwnerUUID()) || Commands.LEVEL_GAMEMASTERS.check(player.permissions()))
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
    }
}
