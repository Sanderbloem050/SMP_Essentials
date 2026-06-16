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
        MenuScreens.register(ModMenus.ADMIN_SHOP, AdminShopScreen::new);
        MenuScreens.register(ModMenus.ATM_MENU, AtmScreen::new);
    }
}
