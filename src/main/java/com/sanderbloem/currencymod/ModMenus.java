package com.sanderbloem.currencymod;

import com.sanderbloem.currencymod.menu.AdminShopMenu;
import com.sanderbloem.currencymod.menu.AtmMenu;
import com.sanderbloem.currencymod.menu.ConfigMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    public static MenuType<AdminShopMenu> ADMIN_SHOP;
    public static MenuType<AtmMenu> ATM_MENU;
    public static MenuType<ConfigMenu> CONFIG_MENU;

    public static void register() {
        ADMIN_SHOP = new MenuType<>(
                (MenuType.MenuSupplier<AdminShopMenu>) AdminShopMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "admin_shop"), ADMIN_SHOP);

        ATM_MENU = new MenuType<>(
                (MenuType.MenuSupplier<AtmMenu>) AtmMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "atm"), ATM_MENU);

        CONFIG_MENU = new MenuType<>(
                (MenuType.MenuSupplier<ConfigMenu>) ConfigMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "config"), CONFIG_MENU);
    }
}
