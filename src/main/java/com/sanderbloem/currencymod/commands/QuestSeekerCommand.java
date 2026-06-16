package com.sanderbloem.currencymod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.sanderbloem.currencymod.data.WalletData;
import com.sanderbloem.currencymod.entity.ModEntities;
import com.sanderbloem.currencymod.entity.QuestSeekerEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;

public class QuestSeekerCommand {

    // Onthoudt de actieve seeker (in-memory; reset bij server-restart).
    private static UUID activeId = null;
    private static ResourceKey<Level> activeDim = null;

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("questseeker")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))

                .then(Commands.literal("spawn")
                        .then(Commands.argument("straal", IntegerArgumentType.integer(50, 5000))
                                .then(Commands.argument("beloning", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer admin = ctx.getSource().getPlayerOrException();
                                            int radius = IntegerArgumentType.getInteger(ctx, "straal");
                                            long reward = LongArgumentType.getLong(ctx, "beloning");

                                            removeActive(admin);

                                            ServerLevel world = admin.level().getServer().overworld();
                                            BlockPos spawn = world.getRespawnData().pos();
                                            BlockPos pos = randomPos(world, spawn, radius);

                                            QuestSeekerEntity npc = new QuestSeekerEntity(ModEntities.QUEST_SEEKER, world);
                                            npc.setReward(reward);
                                            npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
                                            world.addFreshEntity(npc);

                                            activeId = npc.getUUID();
                                            activeDim = world.dimension();

                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "§6Quest Seeker verstopt binnen " + radius + " blokken van spawn! §7Beloning: §6"
                                                            + WalletData.formatBalance(reward)), false);
                                            return 1;
                                        }))))

                .then(Commands.literal("remove").executes(ctx -> {
                    ServerPlayer admin = ctx.getSource().getPlayerOrException();
                    boolean ok = removeActive(admin);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            ok ? "§aQuest Seeker verwijderd." : "§7Er is geen actieve Quest Seeker."), false);
                    return 1;
                }))
        );
    }

    private static boolean removeActive(ServerPlayer admin) {
        if (activeId == null || activeDim == null) return false;
        ServerLevel lvl = admin.level().getServer().getLevel(activeDim);
        if (lvl != null) {
            Entity e = lvl.getEntity(activeId);
            if (e != null) e.remove(Entity.RemovalReason.DISCARDED);
        }
        activeId = null;
        activeDim = null;
        return true;
    }

    private static BlockPos randomPos(ServerLevel world, BlockPos center, int radius) {
        var rng = world.getRandom();
        double angle = rng.nextDouble() * Math.PI * 2;
        int dist = 20 + rng.nextInt(Math.max(1, radius - 20));
        int x = center.getX() + (int) (Math.cos(angle) * dist);
        int z = center.getZ() + (int) (Math.sin(angle) * dist);
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new BlockPos(x, y, z);
    }
}
