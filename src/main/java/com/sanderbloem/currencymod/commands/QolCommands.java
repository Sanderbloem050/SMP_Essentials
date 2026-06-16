package com.sanderbloem.currencymod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.sanderbloem.currencymod.qol.BackManager;
import com.sanderbloem.currencymod.qol.HomesData;
import com.sanderbloem.currencymod.qol.Location;
import com.sanderbloem.currencymod.qol.TpaManager;
import com.sanderbloem.currencymod.qol.WarpsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class QolCommands {

    /** Suggesties: de homes van de speler zelf. */
    private static final SuggestionProvider<CommandSourceStack> HOME_SUGGEST = (ctx, builder) -> {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p != null) SharedSuggestionProvider.suggest(HomesData.get(server(p)).listHomes(p.getUUID()), builder);
        return builder.buildFuture();
    };

    /** Suggesties: alle server-warps. */
    private static final SuggestionProvider<CommandSourceStack> WARP_SUGGEST = (ctx, builder) -> {
        ServerPlayer p = ctx.getSource().getPlayer();
        if (p != null) SharedSuggestionProvider.suggest(WarpsData.get(server(p)).listWarps(), builder);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        registerHomes(d);
        registerWarps(d);
        registerSpawn(d);
        registerTpa(d);
        registerBack(d);
    }

    // ---------- Homes ----------
    private static void registerHomes(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sethome")
                .executes(ctx -> setHome(ctx, "home"))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .executes(ctx -> setHome(ctx, StringArgumentType.getString(ctx, "naam")))));

        d.register(Commands.literal("home")
                .executes(ctx -> goHome(ctx, "home"))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests(HOME_SUGGEST)
                        .executes(ctx -> goHome(ctx, StringArgumentType.getString(ctx, "naam")))));

        d.register(Commands.literal("delhome")
                .executes(ctx -> delHome(ctx, "home"))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests(HOME_SUGGEST)
                        .executes(ctx -> delHome(ctx, StringArgumentType.getString(ctx, "naam")))));

        d.register(Commands.literal("homes").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            var names = HomesData.get(server(p)).listHomes(p.getUUID());
            if (names.isEmpty()) ctx.getSource().sendFailure(Component.literal("§7Je hebt nog geen homes. Gebruik /sethome"));
            else ctx.getSource().sendSuccess(() -> Component.literal("§6Homes: §f" + String.join(", ", names)), false);
            return 1;
        }));
    }

    private static int setHome(CommandContext<CommandSourceStack> ctx, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        HomesData.get(server(p)).setHome(p.getUUID(), name, Location.of(p));
        ctx.getSource().sendSuccess(() -> Component.literal("§aHome §f" + name + " §aopgeslagen."), false);
        return 1;
    }

    private static int goHome(CommandContext<CommandSourceStack> ctx, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Location loc = HomesData.get(server(p)).getHome(p.getUUID(), name);
        if (loc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen home '" + name + "'.")); return 0; }
        BackManager.setLast(p.getUUID(), Location.of(p));
        if (!loc.teleport(p)) { ctx.getSource().sendFailure(Component.literal("§cDimensie bestaat niet meer.")); return 0; }
        ctx.getSource().sendSuccess(() -> Component.literal("§aTeleported naar home §f" + name), false);
        return 1;
    }

    private static int delHome(CommandContext<CommandSourceStack> ctx, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        boolean ok = HomesData.get(server(p)).delHome(p.getUUID(), name);
        if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§aHome §f" + name + " §averwijderd."), false);
        else ctx.getSource().sendFailure(Component.literal("§cGeen home '" + name + "'."));
        return ok ? 1 : 0;
    }

    // ---------- Warps ----------
    private static void registerWarps(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("warp")
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests(WARP_SUGGEST)
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "naam");
                            Location loc = WarpsData.get(server(p)).getWarp(name);
                            if (loc == null) { ctx.getSource().sendFailure(Component.literal("§cGeen warp '" + name + "'.")); return 0; }
                            BackManager.setLast(p.getUUID(), Location.of(p));
                            if (!loc.teleport(p)) { ctx.getSource().sendFailure(Component.literal("§cDimensie bestaat niet meer.")); return 0; }
                            ctx.getSource().sendSuccess(() -> Component.literal("§aWarp naar §f" + name), false);
                            return 1;
                        })));

        d.register(Commands.literal("warps").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            var names = WarpsData.get(server(p)).listWarps();
            if (names.isEmpty()) ctx.getSource().sendFailure(Component.literal("§7Er zijn nog geen warps."));
            else ctx.getSource().sendSuccess(() -> Component.literal("§6Warps: §f" + String.join(", ", names)), false);
            return 1;
        }));

        d.register(Commands.literal("setwarp")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "naam");
                            WarpsData.get(server(p)).setWarp(name, Location.of(p));
                            ctx.getSource().sendSuccess(() -> Component.literal("§aWarp §f" + name + " §agezet."), false);
                            return 1;
                        })));

        d.register(Commands.literal("delwarp")
                .requires(s -> Commands.LEVEL_GAMEMASTERS.check(s.permissions()))
                .then(Commands.argument("naam", StringArgumentType.word())
                        .suggests(WARP_SUGGEST)
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "naam");
                            boolean ok = WarpsData.get(server(p)).delWarp(name);
                            if (ok) ctx.getSource().sendSuccess(() -> Component.literal("§aWarp §f" + name + " §averwijderd."), false);
                            else ctx.getSource().sendFailure(Component.literal("§cGeen warp '" + name + "'."));
                            return ok ? 1 : 0;
                        })));
    }

    // ---------- Spawn ----------
    private static void registerSpawn(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("spawn").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            net.minecraft.world.level.storage.LevelData.RespawnData rd = server(p).overworld().getRespawnData();
            BlockPos sp = rd.pos();
            Location loc = new Location(rd.dimension(), sp.getX() + 0.5, sp.getY(), sp.getZ() + 0.5, rd.yaw(), rd.pitch());
            BackManager.setLast(p.getUUID(), Location.of(p));
            loc.teleport(p);
            ctx.getSource().sendSuccess(() -> Component.literal("§aWelkom bij spawn!"), false);
            return 1;
        }));
    }

    // ---------- TPA ----------
    private static void registerTpa(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("tpa")
                .then(Commands.argument("speler", EntityArgument.player())
                        .executes(ctx -> sendTpa(ctx, false))));
        d.register(Commands.literal("tpahere")
                .then(Commands.argument("speler", EntityArgument.player())
                        .executes(ctx -> sendTpa(ctx, true))));

        d.register(Commands.literal("tpaccept").executes(ctx -> {
            ServerPlayer target = ctx.getSource().getPlayerOrException();
            TpaManager.Request r = TpaManager.consume(target.getUUID());
            if (r == null) { ctx.getSource().sendFailure(Component.literal("§cGeen openstaand verzoek.")); return 0; }
            ServerPlayer from = server(target).getPlayerList().getPlayer(r.from());
            if (from == null) { ctx.getSource().sendFailure(Component.literal("§cDie speler is offline.")); return 0; }
            if (r.here()) {
                BackManager.setLast(target.getUUID(), Location.of(target));
                Location.of(from).teleport(target);   // jij gaat naar de verzoeker
            } else {
                BackManager.setLast(from.getUUID(), Location.of(from));
                Location.of(target).teleport(from);    // verzoeker komt naar jou
            }
            ctx.getSource().sendSuccess(() -> Component.literal("§aVerzoek geaccepteerd."), false);
            from.sendSystemMessage(Component.literal("§a" + target.getName().getString() + " accepteerde je teleport."));
            return 1;
        }));

        d.register(Commands.literal("tpdeny").executes(ctx -> {
            ServerPlayer target = ctx.getSource().getPlayerOrException();
            boolean ok = TpaManager.deny(target.getUUID());
            ctx.getSource().sendSuccess(() -> Component.literal(ok ? "§7Verzoek geweigerd." : "§7Geen verzoek."), false);
            return 1;
        }));
    }

    private static int sendTpa(CommandContext<CommandSourceStack> ctx, boolean here) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer from = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "speler");
        if (from.getUUID().equals(target.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("§cJe kunt jezelf niet vragen."));
            return 0;
        }
        TpaManager.add(target.getUUID(), from.getUUID(), here);
        ctx.getSource().sendSuccess(() -> Component.literal("§aVerzoek verstuurd naar §f" + target.getName().getString()), false);
        target.sendSystemMessage(Component.literal("§e" + from.getName().getString() +
                (here ? " wil dat je naar hem/haar teleporteert." : " wil naar je toe teleporteren.") +
                " §7/tpaccept §7of §7/tpdeny"));
        return 1;
    }

    // ---------- Back ----------
    private static void registerBack(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("back").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Location target = BackManager.consumeBackTarget(p.getUUID());
            if (target == null) {
                ctx.getSource().sendFailure(Component.literal("§cGeen vorige locatie bekend."));
                return 0;
            }
            Location current = Location.of(p);
            if (!target.teleport(p)) {
                ctx.getSource().sendFailure(Component.literal("§cDimensie bestaat niet meer."));
                return 0;
            }
            BackManager.setLast(p.getUUID(), current);
            ctx.getSource().sendSuccess(() -> Component.literal("§aTerug naar je vorige locatie."), false);
            return 1;
        }));
    }

    private static MinecraftServer server(ServerPlayer p) {
        return p.level().getServer();
    }
}
