package com.sanderbloem.smpessentials.menu;

import com.sanderbloem.smpessentials.ModMenus;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.economy.BountyData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyBoardMenu extends AbstractContainerMenu {

    public static final int BTN_PREV_TARGET = 0;
    public static final int BTN_NEXT_TARGET = 1;
    public static final int BTN_ADD_COPPER = 2;
    public static final int BTN_ADD_SILVER = 3;
    public static final int BTN_ADD_GOLD = 4;
    public static final int BTN_REFRESH = 5;

    private static final int ENTRY_COUNT = 5;
    private static final int NAME_LEN = 14;

    private final Player owner;
    private final DataSlot gold = DataSlot.standalone();
    private final DataSlot silver = DataSlot.standalone();
    private final DataSlot copper = DataSlot.standalone();
    private final DataSlot targetPresent = DataSlot.standalone();
    private final DataSlot targetBounty = DataSlot.standalone();
    private final DataSlot[] entryAmounts = new DataSlot[ENTRY_COUNT];
    private final DataSlot[][] entryNameChars = new DataSlot[ENTRY_COUNT][NAME_LEN];
    private final DataSlot[] targetNameChars = new DataSlot[NAME_LEN];

    private final List<UUID> onlineTargets = new ArrayList<>();
    private UUID currentTarget;
    private int targetCursor = 0;

    public BountyBoardMenu(int id, Inventory inv) {
        super(ModMenus.BOUNTY_BOARD_MENU, id);
        this.owner = inv.player;

        this.addDataSlot(gold);
        this.addDataSlot(silver);
        this.addDataSlot(copper);
        this.addDataSlot(targetPresent);
        this.addDataSlot(targetBounty);

        for (int i = 0; i < ENTRY_COUNT; i++) {
            entryAmounts[i] = DataSlot.standalone();
            this.addDataSlot(entryAmounts[i]);
            for (int j = 0; j < NAME_LEN; j++) {
                entryNameChars[i][j] = DataSlot.standalone();
                this.addDataSlot(entryNameChars[i][j]);
            }
        }

        for (int i = 0; i < NAME_LEN; i++) {
            targetNameChars[i] = DataSlot.standalone();
            this.addDataSlot(targetNameChars[i]);
        }

        refreshAll();
    }

    public int getGold() { return gold.get(); }
    public int getSilver() { return silver.get(); }
    public int getCopper() { return copper.get(); }
    public boolean hasTarget() { return targetPresent.get() != 0; }
    public String getTargetName() { return decode(targetNameChars); }
    public long getTargetBounty() { return Integer.toUnsignedLong(targetBounty.get()); }
    public String getEntryName(int index) { return decode(entryNameChars[index]); }
    public long getEntryAmount(int index) { return Integer.toUnsignedLong(entryAmounts[index].get()); }
    public boolean hasEntry(int index) { return getEntryAmount(index) > 0 && !getEntryName(index).isBlank(); }

    private MinecraftServer server() {
        return owner.level().getServer();
    }

    private void refreshAll() {
        refreshBalance();
        refreshBountyEntries();
        refreshTargets();
    }

    private void refreshBalance() {
        if (owner.level().isClientSide()) return;
        long bal = WalletData.get(server()).getBalance(owner.getUUID());
        gold.set((int) Math.min(Integer.MAX_VALUE, bal / WalletData.GOLD_VALUE));
        silver.set((int) ((bal % WalletData.GOLD_VALUE) / WalletData.SILVER_VALUE));
        copper.set((int) (bal % WalletData.SILVER_VALUE));
    }

    private void refreshBountyEntries() {
        if (owner.level().isClientSide()) return;
        List<Map.Entry<UUID, Long>> sorted = BountyData.get(server()).all().entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<UUID, Long> e) -> e.getValue()).reversed())
                .limit(ENTRY_COUNT)
                .toList();

        for (int i = 0; i < ENTRY_COUNT; i++) {
            if (i < sorted.size()) {
                Map.Entry<UUID, Long> entry = sorted.get(i);
                entryAmounts[i].set((int) Math.min(Integer.MAX_VALUE, entry.getValue()));
                encodeInto(entryNameChars[i], shortName(nameOf(entry.getKey())));
            } else {
                entryAmounts[i].set(0);
                encodeInto(entryNameChars[i], "");
            }
        }
    }

    private void refreshTargets() {
        if (owner.level().isClientSide()) return;
        onlineTargets.clear();
        for (ServerPlayer player : server().getPlayerList().getPlayers()) {
            if (!player.getUUID().equals(owner.getUUID())) {
                onlineTargets.add(player.getUUID());
            }
        }
        onlineTargets.sort(Comparator.comparing(this::nameOf, String.CASE_INSENSITIVE_ORDER));

        if (onlineTargets.isEmpty()) {
            currentTarget = null;
            targetCursor = 0;
            targetPresent.set(0);
            targetBounty.set(0);
            encodeInto(targetNameChars, "Geen doelwit");
            return;
        }

        if (targetCursor < 0) targetCursor = onlineTargets.size() - 1;
        if (targetCursor >= onlineTargets.size()) targetCursor = 0;

        currentTarget = onlineTargets.get(targetCursor);
        targetPresent.set(1);
        targetBounty.set((int) Math.min(Integer.MAX_VALUE, BountyData.get(server()).get(currentTarget)));
        encodeInto(targetNameChars, shortName(nameOf(currentTarget)));
    }

    @Override
    public void broadcastChanges() {
        refreshAll();
        super.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner.level().isClientSide()) return false;

        switch (id) {
            case BTN_PREV_TARGET -> targetCursor--;
            case BTN_NEXT_TARGET -> targetCursor++;
            case BTN_ADD_COPPER -> addToCurrentTarget(1L);
            case BTN_ADD_SILVER -> addToCurrentTarget(WalletData.SILVER_VALUE);
            case BTN_ADD_GOLD -> addToCurrentTarget(WalletData.GOLD_VALUE);
            case BTN_REFRESH -> {
            }
            default -> {
                return false;
            }
        }

        refreshAll();
        broadcastChanges();
        return true;
    }

    private void addToCurrentTarget(long amount) {
        refreshTargets();
        if (currentTarget == null) return;

        WalletData wallet = WalletData.get(server());
        if (!wallet.subtractBalance(owner.getUUID(), amount)) {
            owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cOnvoldoende saldo. Je hebt " + WalletData.formatBalance(wallet.getBalance(owner.getUUID()))));
            return;
        }

        BountyData bounties = BountyData.get(server());
        bounties.add(currentTarget, amount);
        long total = bounties.get(currentTarget);
        String msg = "§6" + owner.getName().getString() + " §ezette een bounty van §6"
                + WalletData.formatBalance(amount) + " §eop §c" + nameOf(currentTarget)
                + "§e! §7(totaal: " + WalletData.formatBalance(total) + ")";
        server().getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(msg), false);
    }

    private String nameOf(UUID id) {
        ServerPlayer player = server().getPlayerList().getPlayer(id);
        return player != null ? player.getName().getString() : id.toString().substring(0, 8);
    }

    private String shortName(String value) {
        return value.length() <= NAME_LEN ? value : value.substring(0, NAME_LEN);
    }

    private void encodeInto(DataSlot[] slots, String value) {
        for (int i = 0; i < slots.length; i++) {
            int ch = i < value.length() ? value.charAt(i) : 0;
            slots[i].set(ch);
        }
    }

    private String decode(DataSlot[] slots) {
        StringBuilder sb = new StringBuilder();
        for (DataSlot slot : slots) {
            int value = slot.get();
            if (value <= 0) break;
            sb.append((char) value);
        }
        return sb.toString();
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
