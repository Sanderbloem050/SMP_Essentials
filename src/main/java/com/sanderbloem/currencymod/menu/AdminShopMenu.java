package com.sanderbloem.currencymod.menu;

import com.sanderbloem.currencymod.ModMenus;
import com.sanderbloem.currencymod.data.WalletData;
import com.sanderbloem.currencymod.entity.ShopTrade;
import com.sanderbloem.currencymod.entity.ShopkeeperEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AdminShopMenu extends AbstractContainerMenu {

    public static final int DISPLAY_COUNT = 7;
    public static final int INPUT_INDEX = DISPLAY_COUNT;       // 7
    public static final int INV_START = DISPLAY_COUNT + 1;     // 8
    public static final int TOTAL_SLOTS = INV_START + 36;      // 44

    // Knop-id's
    public static final int BTN_ADD_COPPER = 0;   // +1
    public static final int BTN_ADD_SILVER = 1;   // +9
    public static final int BTN_ADD_GOLD   = 2;   // +81
    public static final int BTN_RESET      = 3;   // prijs naar 0
    public static final int BTN_ADD = 10;         // artikel toevoegen
    public static final int BTN_REMOVE_BASE = 100; // 100..106

    private final ShopkeeperEntity shopkeeper;                 // null op client
    private final SimpleContainer display = new SimpleContainer(DISPLAY_COUNT);
    private final SimpleContainer input = new SimpleContainer(1);
    private final DataSlot pendingPrice = DataSlot.standalone();

    // Client-constructor (via MenuType)
    public AdminShopMenu(int id, Inventory inv) {
        this(id, inv, null);
    }

    // Server-constructor
    public AdminShopMenu(int id, Inventory inv, ShopkeeperEntity shopkeeper) {
        super(ModMenus.ADMIN_SHOP, id);
        this.shopkeeper = shopkeeper;

        // Display-slots (0..6) — alleen tonen, niet pakken/plaatsen
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            this.addSlot(new Slot(display, i, 8 + i * 18, 30) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }
        // Invoer-slot (7)
        this.addSlot(new Slot(input, 0, 8, 74));

        // Spelers-inventory (8..34) en hotbar (35..43)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 200));
        }

        this.addDataSlot(pendingPrice);
        if (shopkeeper != null) {
            pendingPrice.set(0);
            refreshDisplay();
        }
    }

    public int getPendingPrice() { return pendingPrice.get(); }

    private void refreshDisplay() {
        if (shopkeeper == null) return;
        List<ShopTrade> trades = shopkeeper.getTrades();
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            if (i < trades.size()) {
                ShopTrade t = trades.get(i);
                ItemStack icon = t.getItem();
                icon.set(DataComponents.CUSTOM_NAME, Component.literal(
                        icon.getHoverName().getString() + " — " + WalletData.formatBalance(t.getPrice())));
                display.setItem(i, icon);
            } else {
                display.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (shopkeeper == null) return false;

        int delta = switch (id) {
            case BTN_ADD_COPPER -> 1;
            case BTN_ADD_SILVER -> 9;
            case BTN_ADD_GOLD   -> 81;
            default -> 0;
        };
        if (delta != 0) {
            long p = pendingPrice.get() + delta;
            pendingPrice.set((int) Math.min(32000, p));
            return true;
        }

        if (id == BTN_RESET) {
            pendingPrice.set(0);
            return true;
        }

        if (id == BTN_ADD) {
            ItemStack stack = input.getItem(0);
            int price = pendingPrice.get();
            if (!stack.isEmpty() && price >= 1) {
                // Item is alleen een sjabloon: artikel definiëren en item teruggeven
                shopkeeper.addTrade(new ShopTrade(stack.copy(), price));
                player.getInventory().placeItemBackInInventory(stack.copy());
                input.setItem(0, ItemStack.EMPTY);
                pendingPrice.set(0);
                refreshDisplay();
                broadcastChanges();
            }
            return true;
        }

        if (id >= BTN_REMOVE_BASE && id < BTN_REMOVE_BASE + DISPLAY_COUNT) {
            shopkeeper.removeTrade(id - BTN_REMOVE_BASE);
            refreshDisplay();
            broadcastChanges();
            return true;
        }

        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == INPUT_INDEX) {
            if (!moveItemStackTo(stack, INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else if (index >= INV_START) {
            if (!moveItemStackTo(stack, INPUT_INDEX, INPUT_INDEX + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY; // display-slots verplaatsen niet
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (shopkeeper != null && !player.level().isClientSide()) {
            ItemStack stack = input.getItem(0);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
                input.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (shopkeeper == null) return true;
        return shopkeeper.isAlive() && player.distanceToSqr(shopkeeper) < 64.0;
    }
}
