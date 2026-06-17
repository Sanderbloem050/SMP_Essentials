package com.sanderbloem.smpessentials.menu;

import com.sanderbloem.smpessentials.ModMenus;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.economy.AuctionData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

public class AuctionTerminalMenu extends AbstractContainerMenu {

    public static final int PAGE_SIZE = 5;
    public static final int DISPLAY_COUNT = PAGE_SIZE;
    public static final int INPUT_INDEX = DISPLAY_COUNT;
    public static final int INV_START = DISPLAY_COUNT + 1;

    public static final int BTN_ACTION_BASE = 0;
    public static final int BTN_PREV = 10;
    public static final int BTN_NEXT = 11;
    public static final int BTN_FILTER = 12;
    public static final int BTN_ADD_COPPER = 20;
    public static final int BTN_ADD_SILVER = 21;
    public static final int BTN_ADD_GOLD = 22;
    public static final int BTN_RESET = 23;
    public static final int BTN_POST = 24;

    private final Player owner;
    private final SimpleContainer display = new SimpleContainer(DISPLAY_COUNT);
    private final SimpleContainer input = new SimpleContainer(1);

    private final DataSlot page = DataSlot.standalone();
    private final DataSlot totalPages = DataSlot.standalone();
    private final DataSlot filterMine = DataSlot.standalone();
    private final DataSlot pendingPrice = DataSlot.standalone();
    private final DataSlot[] slotIsMine = new DataSlot[DISPLAY_COUNT];

    public AuctionTerminalMenu(int id, Inventory inv) {
        super(ModMenus.AUCTION_TERMINAL, id);
        this.owner = inv.player;

        for (int i = 0; i < DISPLAY_COUNT; i++) {
            this.addSlot(new Slot(display, i, 28 + i * 24, 38) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }

        this.addSlot(new Slot(input, 0, 140, 52));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 16 + col * 18, 186 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 16 + col * 18, 244));
        }

        this.addDataSlot(page);
        this.addDataSlot(totalPages);
        this.addDataSlot(filterMine);
        this.addDataSlot(pendingPrice);
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            slotIsMine[i] = DataSlot.standalone();
            this.addDataSlot(slotIsMine[i]);
        }

        if (!owner.level().isClientSide()) {
            page.set(0);
            filterMine.set(0);
            pendingPrice.set(0);
            refreshDisplay();
        }
    }

    public int getPage() { return page.get(); }
    public int getTotalPages() { return Math.max(1, totalPages.get()); }
    public boolean isFilterMine() { return filterMine.get() != 0; }
    public int getPendingPrice() { return pendingPrice.get(); }
    public int slotOwnership(int i) { return slotIsMine[i].get(); }

    private MinecraftServer server() { return owner.level().getServer(); }

    private List<AuctionData.Listing> sourceListings() {
        List<AuctionData.Listing> all = new java.util.ArrayList<>(AuctionData.get(server()).all());
        if (isFilterMine()) {
            all.removeIf(l -> !l.seller().equals(owner.getUUID()));
        }
        all.sort(Comparator.comparingInt(AuctionData.Listing::id));
        return all;
    }

    private List<AuctionData.Listing> currentPageListings() {
        List<AuctionData.Listing> source = sourceListings();
        int pages = Math.max(1, (int) Math.ceil(source.size() / (double) PAGE_SIZE));
        int p = Math.max(0, Math.min(page.get(), pages - 1));
        page.set(p);
        totalPages.set(pages);
        int from = p * PAGE_SIZE;
        int to = Math.min(source.size(), from + PAGE_SIZE);
        return from >= to ? List.of() : source.subList(from, to);
    }

    private void refreshDisplay() {
        if (owner.level().isClientSide()) return;
        List<AuctionData.Listing> pageItems = currentPageListings();
        for (int i = 0; i < DISPLAY_COUNT; i++) {
            if (i < pageItems.size()) {
                AuctionData.Listing l = pageItems.get(i);
                ItemStack icon = l.item().copy();
                icon.set(DataComponents.CUSTOM_NAME, Component.literal(
                        icon.getHoverName().getString() + " — " + WalletData.formatBalance(l.price())
                                + " (" + l.sellerName() + ")"));
                display.setItem(i, icon);
                slotIsMine[i].set(l.seller().equals(owner.getUUID()) ? 1 : 0);
            } else {
                display.setItem(i, ItemStack.EMPTY);
                slotIsMine[i].set(-1);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner.level().isClientSide()) return false;

        if (id >= BTN_ACTION_BASE && id < BTN_ACTION_BASE + DISPLAY_COUNT) {
            handleAction(id - BTN_ACTION_BASE);
            return true;
        }

        switch (id) {
            case BTN_PREV -> { page.set(Math.max(0, page.get() - 1)); refreshDisplay(); }
            case BTN_NEXT -> { page.set(page.get() + 1); refreshDisplay(); }
            case BTN_FILTER -> { filterMine.set(filterMine.get() == 0 ? 1 : 0); page.set(0); refreshDisplay(); }
            case BTN_ADD_COPPER -> pendingPrice.set(Math.min(32000, pendingPrice.get() + 1));
            case BTN_ADD_SILVER -> pendingPrice.set(Math.min(32000, pendingPrice.get() + 9));
            case BTN_ADD_GOLD -> pendingPrice.set(Math.min(32000, pendingPrice.get() + 81));
            case BTN_RESET -> pendingPrice.set(0);
            case BTN_POST -> postListing();
            default -> { return false; }
        }
        broadcastChanges();
        return true;
    }

    private void handleAction(int slotIndex) {
        List<AuctionData.Listing> pageItems = currentPageListings();
        if (slotIndex >= pageItems.size()) return;
        AuctionData.Listing l = pageItems.get(slotIndex);
        AuctionData data = AuctionData.get(server());

        if (l.seller().equals(owner.getUUID())) {
            if (data.remove(l.id()) != null) {
                ItemStack item = l.item().copy();
                if (!owner.getInventory().add(item)) owner.drop(item, false);
            }
        } else {
            WalletData wallet = WalletData.get(server());
            if (!wallet.subtractBalance(owner.getUUID(), l.price())) return;
            if (data.remove(l.id()) == null) {
                wallet.addBalance(owner.getUUID(), l.price());
                return;
            }
            wallet.addBalance(l.seller(), l.price());
            ItemStack item = l.item().copy();
            if (!owner.getInventory().add(item)) owner.drop(item, false);
        }
        refreshDisplay();
        broadcastChanges();
    }

    private void postListing() {
        ItemStack stack = input.getItem(0);
        int price = pendingPrice.get();
        if (stack.isEmpty() || price < 1) return;
        AuctionData.get(server()).add(owner.getUUID(), owner.getName().getString(), stack.copy(), price);
        input.setItem(0, ItemStack.EMPTY);
        pendingPrice.set(0);
        refreshDisplay();
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.getSlot(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == INPUT_INDEX) {
            if (!moveItemStackTo(stack, INV_START, INV_START + 36, true)) return ItemStack.EMPTY;
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
        if (!player.level().isClientSide()) {
            ItemStack stack = input.getItem(0);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
                input.setItem(0, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }
}
