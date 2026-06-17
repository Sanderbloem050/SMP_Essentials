package com.sanderbloem.smpessentials.menu;

import com.sanderbloem.smpessentials.ModMenus;
import com.sanderbloem.smpessentials.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public class ConfigMenu extends AbstractContainerMenu {

    public static final int BTN_JOBS = 0;
    public static final int BTN_CLAIMS = 1;
    public static final int BTN_DEATHCHEST = 2;
    public static final int BTN_CRATES = 3;
    public static final int BTN_BOUNTIES = 4;
    public static final int BTN_AUCTION = 5;

    private final Player owner;
    private final DataSlot jobs = DataSlot.standalone();
    private final DataSlot claims = DataSlot.standalone();
    private final DataSlot deathChest = DataSlot.standalone();
    private final DataSlot crates = DataSlot.standalone();
    private final DataSlot bounties = DataSlot.standalone();
    private final DataSlot auction = DataSlot.standalone();

    public ConfigMenu(int id, Inventory inv) {
        super(ModMenus.CONFIG_MENU, id);
        this.owner = inv.player;
        this.addDataSlot(jobs);
        this.addDataSlot(claims);
        this.addDataSlot(deathChest);
        this.addDataSlot(crates);
        this.addDataSlot(bounties);
        this.addDataSlot(auction);
        refresh();
    }

    private MinecraftServer server() { return owner.level().getServer(); }

    private void refresh() {
        if (owner.level().isClientSide()) return;
        ModConfig c = ModConfig.get(server());
        jobs.set(c.jobsEnabled ? 1 : 0);
        claims.set(c.claimsEnabled ? 1 : 0);
        deathChest.set(c.deathChestEnabled ? 1 : 0);
        crates.set(c.cratesEnabled ? 1 : 0);
        bounties.set(c.bountiesEnabled ? 1 : 0);
        auction.set(c.auctionEnabled ? 1 : 0);
    }

    public boolean isJobs() { return jobs.get() != 0; }
    public boolean isClaims() { return claims.get() != 0; }
    public boolean isDeathChest() { return deathChest.get() != 0; }
    public boolean isCrates() { return crates.get() != 0; }
    public boolean isBounties() { return bounties.get() != 0; }
    public boolean isAuction() { return auction.get() != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (owner.level().isClientSide()) return false;
        ModConfig c = ModConfig.get(server());
        switch (id) {
            case BTN_JOBS -> c.jobsEnabled = !c.jobsEnabled;
            case BTN_CLAIMS -> c.claimsEnabled = !c.claimsEnabled;
            case BTN_DEATHCHEST -> c.deathChestEnabled = !c.deathChestEnabled;
            case BTN_CRATES -> c.cratesEnabled = !c.cratesEnabled;
            case BTN_BOUNTIES -> c.bountiesEnabled = !c.bountiesEnabled;
            case BTN_AUCTION -> c.auctionEnabled = !c.auctionEnabled;
            default -> { return false; }
        }
        c.setDirty();
        refresh();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
