package com.sanderbloem.smpessentials;

import com.sanderbloem.smpessentials.commands.ShopkeeperCommand;
import com.sanderbloem.smpessentials.commands.WalletCommand;
import com.sanderbloem.smpessentials.entity.ModEntities;
import com.sanderbloem.smpessentials.loot.CoinLootEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrencyMod implements ModInitializer {

    public static final String MOD_ID = "smpessentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();
        ModBlocks.register();
        ModItemGroups.register();
        ModMenus.register();
        ModEntities.register();
        FabricDefaultAttributeRegistry.register(ModEntities.SHOPKEEPER, Mob.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.QUEST_GIVER, Mob.createMobAttributes());
        FabricDefaultAttributeRegistry.register(ModEntities.QUEST_SEEKER, Mob.createMobAttributes());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WalletCommand.register(dispatcher);
            ShopkeeperCommand.register(dispatcher);
            com.sanderbloem.smpessentials.commands.QolCommands.register(dispatcher);
            com.sanderbloem.smpessentials.commands.JobsCommand.register(dispatcher);
            com.sanderbloem.smpessentials.commands.ClaimCommands.register(dispatcher);
            com.sanderbloem.smpessentials.commands.AuctionCommands.register(dispatcher);
            com.sanderbloem.smpessentials.commands.BountyCommands.register(dispatcher);
            com.sanderbloem.smpessentials.commands.QuestGiverCommand.register(dispatcher);
            com.sanderbloem.smpessentials.commands.QuestSeekerCommand.register(dispatcher);
            com.sanderbloem.smpessentials.commands.AdminCommand.register(dispatcher);
            com.sanderbloem.smpessentials.commands.QuestBoardCommands.register(dispatcher);
        });

        // Claim-protectie: breken
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
            if (world.isClientSide()) return true;
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return true;
            if (claimAllowed(sp, pos)) return true;
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cDit gebied is geclaimd."), true);
            return false;
        });

        // Claim-protectie: plaatsen / kisten openen / interacties
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide()) return net.minecraft.world.InteractionResult.PASS;
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return net.minecraft.world.InteractionResult.PASS;
            if (claimAllowed(sp, hit.getBlockPos())) return net.minecraft.world.InteractionResult.PASS;
            sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cDit gebied is geclaimd."), true);
            return net.minecraft.world.InteractionResult.FAIL;
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(MobDropHandler::onDeath);
        ServerLivingEntityEvents.AFTER_DEATH.register(CurrencyMod::onBountyDeath);
        ServerLivingEntityEvents.ALLOW_DEATH.register(com.sanderbloem.smpessentials.qol.DeathChestHandler::onAllowDeath);

        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                com.sanderbloem.smpessentials.claims.ClaimBorders::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(
                com.sanderbloem.smpessentials.quests.QuestEvents::tick);

        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (world.isClientSide()) return;
            if (!(player instanceof net.minecraft.server.level.ServerPlayer sp)) return;
            if (sp.getAbilities().instabuild) return; // geen beloning in creatief
            if (!com.sanderbloem.smpessentials.config.ModConfig.get(sp.level().getServer()).jobsEnabled) return;
            long reward = com.sanderbloem.smpessentials.jobs.JobRewards.rewardFor(state);
            if (reward > 0) {
                com.sanderbloem.smpessentials.data.WalletData.get(sp.level().getServer())
                        .addBalance(sp.getUUID(), reward);
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§6+ §e" + com.sanderbloem.smpessentials.data.WalletData.formatBalance(reward) + " §7(job)"), true);
            }
        });

        CoinLootEvents.register();

        LOGGER.info("SMP Essentials geladen!");
    }

    private static void onBountyDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer victim)) return;
        if (!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer killer)) return;
        if (killer.getUUID().equals(victim.getUUID())) return;
        if (!com.sanderbloem.smpessentials.config.ModConfig.get(victim.level().getServer()).bountiesEnabled) return;

        com.sanderbloem.smpessentials.economy.BountyData bounties =
                com.sanderbloem.smpessentials.economy.BountyData.get(victim.level().getServer());
        long reward = bounties.claim(victim.getUUID());
        if (reward > 0) {
            com.sanderbloem.smpessentials.data.WalletData.get(killer.level().getServer())
                    .addBalance(killer.getUUID(), reward);
            String msg = "§6" + killer.getName().getString() + " §eincasseerde de bounty op §c"
                    + victim.getName().getString() + " §evoor §6"
                    + com.sanderbloem.smpessentials.data.WalletData.formatBalance(reward) + "§e!";
            killer.level().getServer().getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal(msg), false);
        }
    }

    private static boolean claimAllowed(net.minecraft.server.level.ServerPlayer sp, net.minecraft.core.BlockPos pos) {
        if (!com.sanderbloem.smpessentials.config.ModConfig.get(sp.level().getServer()).claimsEnabled) return true;
        String key = com.sanderbloem.smpessentials.claims.ClaimsData.key(sp.level(), pos);
        boolean op = net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(sp.permissions());
        return com.sanderbloem.smpessentials.claims.ClaimsData.get(sp.level().getServer())
                .canBuild(sp.getUUID(), key, op);
    }
}
