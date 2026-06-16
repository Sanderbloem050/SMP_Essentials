package com.sanderbloem.currencymod.menu;

import com.sanderbloem.currencymod.ModItems;
import com.sanderbloem.currencymod.ModMenus;
import com.sanderbloem.currencymod.data.WalletData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AtmMenu extends AbstractContainerMenu {

    public static final int BTN_DEPOSIT = 0;
    public static final int BTN_WD_COPPER = 1;
    public static final int BTN_WD_SILVER = 2;
    public static final int BTN_WD_GOLD = 3;

    private final Player owner;
    private final DataSlot gold = DataSlot.standalone();
    private final DataSlot silver = DataSlot.standalone();
    private final DataSlot copper = DataSlot.standalone();

    public AtmMenu(int id, Inventory inv) {
        super(ModMenus.ATM_MENU, id);
        this.owner = inv.player;

        // Spelers-inventory (0..26) en hotbar (27..35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 108 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 168));
        }

        this.addDataSlot(gold);
        this.addDataSlot(silver);
        this.addDataSlot(copper);
        refreshBalance();
    }

    public int getGold() { return gold.get(); }
    public int getSilver() { return silver.get(); }
    public int getCopper() { return copper.get(); }

    private MinecraftServer server() { return owner.level().getServer(); }

    private void refreshBalance() {
        if (owner.level().isClientSide()) return;
        long bal = WalletData.get(server()).getBalance(owner.getUUID());
        gold.set((int) Math.min(Integer.MAX_VALUE, bal / WalletData.GOLD_VALUE));
        silver.set((int) ((bal % WalletData.GOLD_VALUE) / WalletData.SILVER_VALUE));
        copper.set((int) (bal % WalletData.SILVER_VALUE));
    }

    @Override
    public void broadcastChanges() {
        refreshBalance();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner.level().isClientSide()) return false;
        WalletData wallet = WalletData.get(server());

        switch (id) {
            case BTN_DEPOSIT -> {
                long total = collectCoins();
                if (total > 0) wallet.addBalance(owner.getUUID(), total);
            }
            case BTN_WD_COPPER -> withdraw(wallet, 1L, ModItems.COPPER_COIN);
            case BTN_WD_SILVER -> withdraw(wallet, WalletData.SILVER_VALUE, ModItems.SILVER_COIN);
            case BTN_WD_GOLD   -> withdraw(wallet, WalletData.GOLD_VALUE, ModItems.GOLD_COIN);
            default -> { return false; }
        }
        refreshBalance();
        broadcastChanges();
        return true;
    }

    private void withdraw(WalletData wallet, long value, Item coin) {
        if (wallet.subtractBalance(owner.getUUID(), value)) {
            give(new ItemStack(coin, 1));
        }
    }

    private long collectCoins() {
        long total = 0;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (s.is(ModItems.COPPER_COIN)) { total += s.getCount(); s.setCount(0); }
            else if (s.is(ModItems.SILVER_COIN)) { total += (long) s.getCount() * WalletData.SILVER_VALUE; s.setCount(0); }
            else if (s.is(ModItems.GOLD_COIN))   { total += (long) s.getCount() * WalletData.GOLD_VALUE;  s.setCount(0); }
        }
        return total;
    }

    private void give(ItemStack stack) {
        if (!owner.getInventory().add(stack)) owner.drop(stack, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
