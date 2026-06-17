package com.sanderbloem.smpessentials.menu;

import com.sanderbloem.smpessentials.ModMenus;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.entity.ShopTrade;
import com.sanderbloem.smpessentials.entity.ShopTradeMode;
import com.sanderbloem.smpessentials.entity.ShopkeeperEntity;
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
    public static final int INPUT_INDEX   = DISPLAY_COUNT;       // 7
    public static final int INV_START     = DISPLAY_COUNT + 1;   // 8
    public static final int TOTAL_SLOTS   = INV_START + 36;      // 44

    // Knop-id's
    public static final int BTN_ADD_COPPER  = 0;
    public static final int BTN_ADD_SILVER  = 1;
    public static final int BTN_ADD_GOLD    = 2;
    public static final int BTN_RESET       = 3;
    public static final int BTN_TOGGLE_MODE = 4;
    public static final int BTN_ADD         = 10;
    public static final int BTN_PREV        = 11;
    public static final int BTN_NEXT        = 12;
    public static final int BTN_REMOVE_BASE = 100; // 100..106

    private final ShopkeeperEntity shopkeeper;
    private final SimpleContainer display = new SimpleContainer(DISPLAY_COUNT);
    private final SimpleContainer input   = new SimpleContainer(1);
    private final DataSlot pendingPrice = DataSlot.standalone();
    private final DataSlot page         = DataSlot.standalone();
    private final DataSlot totalPages   = DataSlot.standalone();
    /** 0 = SELL_TO_PLAYER, 1 = BUY_FROM_PLAYER */
    private final DataSlot pendingMode  = DataSlot.standalone();

    public AdminShopMenu(int id, Inventory inv) {
        this(id, inv, null);
    }

    public AdminShopMenu(int id, Inventory inv, ShopkeeperEntity shopkeeper) {
        super(ModMenus.ADMIN_SHOP, id);
        this.shopkeeper = shopkeeper;

        for (int i = 0; i < DISPLAY_COUNT; i++) {
            this.addSlot(new Slot(display, i, 8 + i * 18, 30) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }
        this.addSlot(new Slot(input, 0, 8, 74));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 200));
        }

        this.addDataSlot(pendingPrice);
        this.addDataSlot(page);
        this.addDataSlot(totalPages);
        this.addDataSlot(pendingMode);

        if (shopkeeper != null) {
            pendingPrice.set(0);
            pendingMode.set(0);
            page.set(0);
            refreshDisplay();
        }
    }

    public int getPendingPrice() { return pendingPrice.get(); }
    public int getPage()        { return page.get(); }
    public int getTotalPages()  { return Math.max(1, totalPages.get()); }
    public boolean isBuyMode()  { return pendingMode.get() == 1; }

    public ShopTradeMode getPendingMode() {
        return pendingMode.get() == 1 ? ShopTradeMode.BUY_FROM_PLAYER : ShopTradeMode.SELL_TO_PLAYER;
    }

    private List<ShopTrade> currentPageTrades() {
        List<ShopTrade> all = shopkeeper.getTrades();
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) DISPLAY_COUNT));
        int p = Math.max(0, Math.min(page.get(), pages - 1));
        page.set(p);
        totalPages.set(pages);
        int from = p * DISPLAY_COUNT;
        int to   = Math.min(all.size(), from + DISPLAY_COUNT);
        return from >= to ? List.of() : all.subList(from, to);
    }

    private void refreshDisplay() {
        if (shopkeeper == null) return;
        List<ShopTrade> pageTrades = currentPageTrades();
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            if (i < pageTrades.size()) {
                ShopTrade t = pageTrades.get(i);
                ItemStack icon = t.getItem();
                String modeTag = t.getMode() == ShopTradeMode.BUY_FROM_PLAYER ? "§b[KOOP] " : "§a[VERKOOP] ";
                icon.set(DataComponents.CUSTOM_NAME, Component.literal(
                        modeTag + icon.getHoverName().getString() + " — " + WalletData.formatBalance(t.getPrice())));
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
            pendingPrice.set((int) Math.min(32000, pendingPrice.get() + delta));
            return true;
        }

        if (id == BTN_RESET) {
            pendingPrice.set(0);
            return true;
        }

        if (id == BTN_TOGGLE_MODE) {
            pendingMode.set(pendingMode.get() == 0 ? 1 : 0);
            return true;
        }

        if (id == BTN_PREV) {
            page.set(Math.max(0, page.get() - 1));
            refreshDisplay();
            broadcastChanges();
            return true;
        }

        if (id == BTN_NEXT) {
            page.set(page.get() + 1);
            refreshDisplay();
            broadcastChanges();
            return true;
        }

        if (id == BTN_ADD) {
            ItemStack stack = input.getItem(0);
            int price = pendingPrice.get();
            if (!stack.isEmpty() && price >= 1) {
                shopkeeper.addTrade(new ShopTrade(stack.copy(), price, getPendingMode()));
                player.getInventory().placeItemBackInInventory(stack.copy());
                input.setItem(0, ItemStack.EMPTY);
                pendingPrice.set(0);
                refreshDisplay();
                broadcastChanges();
            }
            return true;
        }

        if (id >= BTN_REMOVE_BASE && id < BTN_REMOVE_BASE + DISPLAY_COUNT) {
            List<ShopTrade> pageTrades = currentPageTrades();
            int localIndex = id - BTN_REMOVE_BASE;
            if (localIndex < pageTrades.size()) {
                ShopTrade target = pageTrades.get(localIndex);
                int globalIndex = shopkeeper.getTrades().indexOf(target);
                if (globalIndex >= 0) shopkeeper.removeTrade(globalIndex);
            }
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
        ItemStack copy  = stack.copy();

        if (index == INPUT_INDEX) {
            if (!moveItemStackTo(stack, INV_START, TOTAL_SLOTS, true)) return ItemStack.EMPTY;
        } else if (index >= INV_START) {
            if (!moveItemStackTo(stack, INPUT_INDEX, INPUT_INDEX + 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
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
