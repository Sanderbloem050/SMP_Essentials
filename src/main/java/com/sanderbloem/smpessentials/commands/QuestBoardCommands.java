package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.quests.QuestBoardData;
import com.sanderbloem.smpessentials.quests.QuestDefinition;
import com.sanderbloem.smpessentials.quests.QuestType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class QuestBoardCommands {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("questboard")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))

                .then(Commands.literal("list").executes(ctx -> {
                    var data = QuestBoardData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("§6=== Actieve quests ==="), false);
                    for (QuestDefinition q : data.getActiveQuests()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§e" + q.id() + " §7[" + q.type() + "] §f" + q.title()
                                        + " §7— beloning §6" + WalletData.formatBalance(q.reward())), false);
                    }
                    return 1;
                }))

                .then(Commands.literal("reroll").executes(ctx -> {
                    QuestBoardData.get(ctx.getSource().getServer()).reroll();
                    ctx.getSource().sendSuccess(() -> Component.literal("§aQuest Board ge-rerolled — nieuwe quests actief."), false);
                    return 1;
                }))

                .then(Commands.literal("forcecomplete")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "questId");
                                    var data = QuestBoardData.get(ctx.getSource().getServer());
                                    QuestDefinition q = data.getActiveQuests().stream()
                                            .filter(x -> x.id().equals(id)).findFirst().orElse(null);
                                    if (q == null) {
                                        ctx.getSource().sendFailure(Component.literal("§cOnbekend quest-id."));
                                        return 0;
                                    }
                                    if (q.type() != QuestType.COMMUNITY_FETCH) {
                                        ctx.getSource().sendFailure(Component.literal("§cForcecomplete werkt alleen voor community-quests."));
                                        return 0;
                                    }
                                    long remaining = q.amount() - data.getCommunityProgress(q.id());
                                    if (remaining > 0) data.addCommunityProgress(q.id(), remaining);
                                    ctx.getSource().sendSuccess(() -> Component.literal("§aCommunity-doel geforceerd voltooid."), false);
                                    return 1;
                                })))

                .then(Commands.literal("reset")
                        .then(Commands.argument("questId", StringArgumentType.word())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "questId");
                                    QuestBoardData.get(ctx.getSource().getServer()).resetQuest(id);
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "§aProgress/claims voor §f" + id + " §agereset."), false);
                                    return 1;
                                })))

                .then(Commands.literal("debug").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    var data = QuestBoardData.get(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.literal("§6=== Quest Board debug ==="), false);
                    for (QuestDefinition q : data.getActiveQuests()) {
                        String extra = switch (q.type()) {
                            case COMMUNITY_FETCH -> "globaal: " + data.getCommunityProgress(q.id()) + "/" + q.amount();
                            case TRAVEL_DISTANCE -> "jouw progress: " + data.getPlayerProgress(p.getUUID(), q.id()).amountProgress() + "/" + q.amount();
                            case FETCH -> "geclaimd door jou: " + data.hasClaimed(p.getUUID(), q.id());
                        };
                        ctx.getSource().sendSuccess(() -> Component.literal("§e" + q.id() + " §7— " + extra), false);
                    }
                    return 1;
                }))
        );
    }
}
