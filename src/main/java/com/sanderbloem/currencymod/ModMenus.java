package com.sanderbloem.currencymod;

import com.sanderbloem.currencymod.menu.AdminShopMenu;
import com.sanderbloem.currencymod.menu.AtmMenu;
import com.sanderbloem.currencymod.menu.AuctionTerminalMenu;
import com.sanderbloem.currencymod.menu.BountyBoardMenu;
import com.sanderbloem.currencymod.menu.ConfigMenu;
import com.sanderbloem.currencymod.menu.QuestBoardMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    public static MenuType<AdminShopMenu> ADMIN_SHOP;
    public static MenuType<AtmMenu> ATM_MENU;
    public static MenuType<BountyBoardMenu> BOUNTY_BOARD_MENU;
    public static MenuType<AuctionTerminalMenu> AUCTION_TERMINAL;
    public static MenuType<ConfigMenu> CONFIG_MENU;
    public static MenuType<QuestBoardMenu> QUEST_BOARD_MENU;

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

        BOUNTY_BOARD_MENU = new MenuType<>(
                (MenuType.MenuSupplier<BountyBoardMenu>) BountyBoardMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "bounty_board"), BOUNTY_BOARD_MENU);

        AUCTION_TERMINAL = new MenuType<>(
                (MenuType.MenuSupplier<AuctionTerminalMenu>) AuctionTerminalMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "auction_terminal"), AUCTION_TERMINAL);

        CONFIG_MENU = new MenuType<>(
                (MenuType.MenuSupplier<ConfigMenu>) ConfigMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "config"), CONFIG_MENU);

        QUEST_BOARD_MENU = new MenuType<>(
                (MenuType.MenuSupplier<QuestBoardMenu>) QuestBoardMenu::new,
                FeatureFlags.VANILLA_SET);
        Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "quest_board"), QUEST_BOARD_MENU);
    }
}
