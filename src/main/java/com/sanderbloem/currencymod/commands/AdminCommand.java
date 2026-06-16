package com.sanderbloem.currencymod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.sanderbloem.currencymod.menu.ConfigMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public class AdminCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("smpadmin")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    p.openMenu(new SimpleMenuProvider(
                            (id, inv, pl) -> new ConfigMenu(id, inv),
                            Component.literal("§6SMP Essentials — Instellingen")));
                    return 1;
                }));
    }
}
