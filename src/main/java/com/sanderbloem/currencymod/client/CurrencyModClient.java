package com.sanderbloem.currencymod.client;

import com.sanderbloem.currencymod.ModMenus;
import com.sanderbloem.currencymod.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

public class CurrencyModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SHOPKEEPER, ShopkeeperRenderer::new);
        EntityRendererRegistry.register(ModEntities.QUEST_GIVER, QuestGiverRenderer::new);
        EntityRendererRegistry.register(ModEntities.QUEST_SEEKER, QuestSeekerRenderer::new);
        MenuScreens.register(ModMenus.ADMIN_SHOP, AdminShopScreen::new);
        MenuScreens.register(ModMenus.ATM_MENU, AtmScreen::new);
        MenuScreens.register(ModMenus.BOUNTY_BOARD_MENU, BountyBoardScreen::new);
        MenuScreens.register(ModMenus.AUCTION_TERMINAL, AuctionTerminalScreen::new);
        MenuScreens.register(ModMenus.CONFIG_MENU, ConfigScreen::new);
        MenuScreens.register(ModMenus.QUEST_BOARD_MENU, QuestBoardScreen::new);
    }
}
