package com.sanderbloem.smpessentials.menu;

import com.sanderbloem.smpessentials.ModMenus;
import com.sanderbloem.smpessentials.data.WalletData;
import com.sanderbloem.smpessentials.quests.QuestBoardData;
import com.sanderbloem.smpessentials.quests.QuestDefinition;
import com.sanderbloem.smpessentials.quests.QuestProgress;
import com.sanderbloem.smpessentials.quests.QuestType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class QuestBoardMenu extends AbstractContainerMenu {

    public static final int BTN_FETCH_TURNIN = 0;
    public static final int BTN_TRAVEL_CLAIM = 1;
    public static final int BTN_COMMUNITY_CONTRIBUTE = 2;
    public static final int BTN_COMMUNITY_CLAIM = 3;

    private static final int LINE_LEN = 42;

    private final Player owner;
    private final DataSlot[] fetchLine1 = newLine();
    private final DataSlot[] fetchLine2 = newLine();
    private final DataSlot[] travelLine1 = newLine();
    private final DataSlot[] travelLine2 = newLine();
    private final DataSlot[] communityLine1 = newLine();
    private final DataSlot[] communityLine2 = newLine();

    public QuestBoardMenu(int id, Inventory inv) {
        super(ModMenus.QUEST_BOARD_MENU, id);
        this.owner = inv.player;

        registerLine(fetchLine1);
        registerLine(fetchLine2);
        registerLine(travelLine1);
        registerLine(travelLine2);
        registerLine(communityLine1);
        registerLine(communityLine2);

        refresh();
    }

    private DataSlot[] newLine() { return new DataSlot[LINE_LEN]; }

    private void registerLine(DataSlot[] line) {
        for (int i = 0; i < line.length; i++) {
            line[i] = DataSlot.standalone();
            this.addDataSlot(line[i]);
        }
    }

    private MinecraftServer server() { return owner.level().getServer(); }

    private QuestDefinition findType(QuestType type) {
        return QuestBoardData.get(server()).getActiveQuests().stream()
                .filter(q -> q.type() == type).findFirst().orElse(null);
    }

    private void refresh() {
        if (owner.level().isClientSide()) return;
        QuestBoardData data = QuestBoardData.get(server());

        QuestDefinition fetch = findType(QuestType.FETCH);
        if (fetch != null) {
            boolean claimed = data.hasClaimed(owner.getUUID(), fetch.id());
            encode(fetchLine1, "Fetch: " + fetch.title());
            encode(fetchLine2, fetch.description() + " | beloning " + WalletData.formatBalance(fetch.reward())
                    + " | " + (claimed ? "al ingeleverd" : "open"));
        }

        QuestDefinition travel = findType(QuestType.TRAVEL_DISTANCE);
        if (travel != null) {
            QuestProgress p = data.getPlayerProgress(owner.getUUID(), travel.id());
            encode(travelLine1, "Verkenning: " + travel.title());
            encode(travelLine2, travel.description() + " (" + Math.min(p.amountProgress(), travel.amount())
                    + "/" + travel.amount() + ") | beloning " + WalletData.formatBalance(travel.reward())
                    + " | " + (p.claimed() ? "geclaimd" : p.completed() ? "klaar — claim!" : "onderweg"));
        }

        QuestDefinition community = findType(QuestType.COMMUNITY_FETCH);
        if (community != null) {
            long progress = data.getCommunityProgress(community.id());
            boolean done = progress >= community.amount();
            boolean claimed = data.hasClaimed(owner.getUUID(), community.id());
            encode(communityLine1, "Community: " + community.title());
            encode(communityLine2, community.description() + " (" + Math.min(progress, community.amount())
                    + "/" + community.amount() + ") | beloning " + WalletData.formatBalance(community.reward())
                    + " | " + (claimed ? "geclaimd" : done ? "doel gehaald — claim!" : "in opbouw"));
        }
    }

    public String getFetchLine1() { return decode(fetchLine1); }
    public String getFetchLine2() { return decode(fetchLine2); }
    public String getTravelLine1() { return decode(travelLine1); }
    public String getTravelLine2() { return decode(travelLine2); }
    public String getCommunityLine1() { return decode(communityLine1); }
    public String getCommunityLine2() { return decode(communityLine2); }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner.level().isClientSide()) return false;
        QuestBoardData data = QuestBoardData.get(server());

        switch (id) {
            case BTN_FETCH_TURNIN -> handleFetch(data);
            case BTN_TRAVEL_CLAIM -> handleTravelClaim(data);
            case BTN_COMMUNITY_CONTRIBUTE -> handleCommunityContribute(data);
            case BTN_COMMUNITY_CLAIM -> handleCommunityClaim(data);
            default -> { return false; }
        }
        refresh();
        broadcastChanges();
        return true;
    }

    private void msg(String text) {
        owner.sendSystemMessage(Component.literal(text));
    }

    private void handleFetch(QuestBoardData data) {
        QuestDefinition q = findType(QuestType.FETCH);
        if (q == null) return;
        if (data.hasClaimed(owner.getUUID(), q.id())) {
            msg("§7Je hebt deze quest al ingeleverd. Wacht op een refresh van het bord.");
            return;
        }
        int have = countMatching(q.item());
        if (have < q.amount()) {
            msg("§cJe hebt nog §f" + (q.amount() - have) + "× " + itemName(q.item()) + " §cnodig.");
            return;
        }
        removeMatching(q.item(), q.amount());
        WalletData.get(server()).addBalance(owner.getUUID(), q.reward());
        data.markClaimed(owner.getUUID(), q.id());
        msg("§aQuest voltooid! §7Je ontving §6" + WalletData.formatBalance(q.reward()));
    }

    private void handleTravelClaim(QuestBoardData data) {
        QuestDefinition q = findType(QuestType.TRAVEL_DISTANCE);
        if (q == null) return;
        if (data.hasClaimed(owner.getUUID(), q.id())) {
            msg("§7Je hebt deze beloning al geclaimd.");
            return;
        }
        QuestProgress p = data.getPlayerProgress(owner.getUUID(), q.id());
        if (p.amountProgress() < q.amount()) {
            msg("§cJe hebt nog §f" + (q.amount() - p.amountProgress()) + " §cblokken te gaan.");
            return;
        }
        WalletData.get(server()).addBalance(owner.getUUID(), q.reward());
        data.markClaimed(owner.getUUID(), q.id());
        msg("§aBeloning geclaimd! §7Je ontving §6" + WalletData.formatBalance(q.reward()));
    }

    private void handleCommunityContribute(QuestBoardData data) {
        QuestDefinition q = findType(QuestType.COMMUNITY_FETCH);
        if (q == null) return;
        long current = data.getCommunityProgress(q.id());
        long remaining = q.amount() - current;
        if (remaining <= 0) {
            msg("§7Het doel is al gehaald — klik Claim voor je beloning.");
            return;
        }
        int have = countMatching(q.item());
        if (have <= 0) {
            msg("§cJe hebt geen §f" + itemName(q.item()) + " §cbij je.");
            return;
        }
        int contribute = (int) Math.min(have, remaining);
        removeMatching(q.item(), contribute);
        data.addCommunityProgress(q.id(), contribute);
        data.markContributed(owner.getUUID(), q.id(), contribute);
        msg("§aBijgedragen: §f" + contribute + "× " + itemName(q.item()));
    }

    private void handleCommunityClaim(QuestBoardData data) {
        QuestDefinition q = findType(QuestType.COMMUNITY_FETCH);
        if (q == null) return;
        if (data.getCommunityProgress(q.id()) < q.amount()) {
            msg("§7Het gemeenschapsdoel is nog niet gehaald.");
            return;
        }
        if (!data.hasContributed(owner.getUUID(), q.id())) {
            msg("§cJe moet eerst bijdragen voor je kunt claimen.");
            return;
        }
        if (data.hasClaimed(owner.getUUID(), q.id())) {
            msg("§7Je hebt deze beloning al geclaimd.");
            return;
        }
        WalletData.get(server()).addBalance(owner.getUUID(), q.reward());
        data.markClaimed(owner.getUUID(), q.id());
        msg("§aBeloning geclaimd! §7Je ontving §6" + WalletData.formatBalance(q.reward()));
    }

    private String itemName(net.minecraft.world.item.Item item) {
        return new ItemStack(item).getHoverName().getString();
    }

    private int countMatching(net.minecraft.world.item.Item item) {
        int total = 0;
        for (int i = 0; i < owner.getInventory().getContainerSize(); i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(item)) total += s.getCount();
        }
        return total;
    }

    private void removeMatching(net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < owner.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = owner.getInventory().getItem(i);
            if (!s.isEmpty() && s.is(item)) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private void encode(DataSlot[] slots, String value) {
        String v = value.length() > slots.length ? value.substring(0, slots.length) : value;
        for (int i = 0; i < slots.length; i++) {
            slots[i].set(i < v.length() ? v.charAt(i) : 0);
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
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
