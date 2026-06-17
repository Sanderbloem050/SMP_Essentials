package com.sanderbloem.smpessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class JobsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("jobs").executes(ctx -> {
            var src = ctx.getSource();
            src.sendSuccess(() -> Component.literal("§6=== Jobs — verdien munten ==="), false);
            src.sendSuccess(() -> Component.literal("§e⛏ Mining §7(per erts):"), false);
            src.sendSuccess(() -> Component.literal("§7 kool/koper/quartz §f2c §7· ijzer/lapis/redstone §f3c §7· goud §f5c"), false);
            src.sendSuccess(() -> Component.literal("§7 diamant §f8c §7· emerald §f10c §7· ancient debris §f25c"), false);
            src.sendSuccess(() -> Component.literal("§a🌾 Farming §7(per rijp gewas): §f1c"), false);
            src.sendSuccess(() -> Component.literal("§7Beloningen gaan automatisch naar je wallet (alleen in survival)."), false);
            return 1;
        }));
    }
}
