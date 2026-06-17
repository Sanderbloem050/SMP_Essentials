package com.sanderbloem.currencymod.entity;

import com.sanderbloem.currencymod.ModItems;
import com.sanderbloem.currencymod.data.WalletData;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShopkeeperEntity extends PathfinderMob implements Merchant {

    private final List<ShopTrade> trades = new ArrayList<>();
    /** Parallel aan de MerchantOffers — bijgehouden per buildOffers(). */
    private final List<ShopTrade> offerIndex = new ArrayList<>();
    private UUID ownerUUID = null;

    private Player tradingPlayer = null;
    private MerchantOffers offers = null;

    public ShopkeeperEntity(EntityType<? extends PathfinderMob> type, net.minecraft.world.level.Level world) {
        super(type, world);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    @Override protected void registerGoals() {}

    @Override
    public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) { return true; }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }

    @Override public boolean isPushable() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            Player p = level().getNearestPlayer(this, 12.0);
            if (p != null) faceTowards(p);
        }
    }

    private void faceTowards(Player p) {
        double dx = p.getX() - this.getX();
        double dz = p.getZ() - this.getZ();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0f;
        this.setYRot(yaw); this.setYBodyRot(yaw); this.setYHeadRot(yaw);
        this.yRotO = yaw; this.yBodyRotO = yaw; this.yHeadRotO = yaw;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide()) {
            boolean isOwnerOrOp = net.minecraft.commands.Commands.LEVEL_GAMEMASTERS.check(player.permissions()) ||
                    (ownerUUID != null && ownerUUID.equals(player.getUUID()));

            if (player.isShiftKeyDown() && isOwnerOrOp) {
                openAdminMenu((ServerPlayer) player);
            } else if (!trades.isEmpty()) {
                this.offers = buildOffers();
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            } else {
                player.sendSystemMessage(Component.literal("§cDeze winkel heeft nog geen artikelen."));
                if (isOwnerOrOp) {
                    player.sendSystemMessage(Component.literal("§7Shift+klik om te beheren."));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    private void openAdminMenu(ServerPlayer player) {
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (id, inv, p) -> new com.sanderbloem.currencymod.menu.AdminShopMenu(id, inv, this),
                Component.literal("Beheer: " + getDisplayName().getString())));
    }

    public List<ShopTrade> getTrades() { return List.copyOf(trades); }
    public void addTrade(ShopTrade trade) { trades.add(trade); this.offers = null; }
    public boolean removeTrade(int index) {
        if (index < 0 || index >= trades.size()) return false;
        trades.remove(index);
        this.offers = null;
        return true;
    }
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(UUID uuid) { this.ownerUUID = uuid; }

    // ===== Container helpers =====

    public Container getStockContainer() {
        if (level().isClientSide()) return null;
        Container c = HopperBlockEntity.getContainerAt(level(), getOnPos());
        if (c == null) c = HopperBlockEntity.getContainerAt(level(), blockPosition().below());
        return c;
    }

    private static int countInContainer(Container c, ItemStack ref) {
        int total = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, ref)) total += s.getCount();
        }
        return total;
    }

    private static void removeFromContainer(Container c, ItemStack ref, int amount) {
        int remaining = amount;
        for (int i = 0; i < c.getContainerSize() && remaining > 0; i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, ref)) {
                int take = Math.min(s.getCount(), remaining);
                c.removeItem(i, take);
                remaining -= take;
            }
        }
        c.setChanged();
    }

    private void depositToContainer(Container c, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemStack rem = stack.copy();
        for (int i = 0; i < c.getContainerSize() && !rem.isEmpty(); i++) {
            ItemStack s = c.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, rem)) {
                int can = Math.min(c.getMaxStackSize(s), s.getMaxStackSize()) - s.getCount();
                if (can > 0) {
                    int move = Math.min(can, rem.getCount());
                    s.grow(move); rem.shrink(move);
                    c.setItem(i, s);
                }
            }
        }
        for (int i = 0; i < c.getContainerSize() && !rem.isEmpty(); i++) {
            if (c.getItem(i).isEmpty()) {
                int move = Math.min(c.getMaxStackSize(rem), rem.getCount());
                ItemStack put = rem.copy(); put.setCount(move);
                c.setItem(i, put); rem.shrink(move);
            }
        }
        c.setChanged();
        if (!rem.isEmpty() && level() instanceof ServerLevel sl) this.spawnAtLocation(sl, rem);
    }

    private static boolean containerHasSpace(Container c, ItemStack stack) {
        int need = stack.getCount();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(s, stack)) {
                int can = Math.min(c.getMaxStackSize(s), stack.getMaxStackSize()) - s.getCount();
                need -= can;
                if (need <= 0) return true;
            }
        }
        return false;
    }

    private static int countInPlayerInv(Player p, ItemStack ref) {
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, ref)) total += s.getCount();
        }
        return total;
    }

    private static void removeFromPlayerInv(Player p, ItemStack ref, int amount) {
        int remaining = amount;
        for (int i = 0; i < p.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, ref)) {
                int take = Math.min(s.getCount(), remaining);
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    // ===== Merchant offers =====

    private MerchantOffers buildOffers() {
        Container stock = getStockContainer();
        MerchantOffers result = new MerchantOffers();
        offerIndex.clear();

        for (ShopTrade t : trades) {
            if (t.getMode() == ShopTradeMode.BUY_FROM_PLAYER) {
                result.add(toBuyOffer(t, stock));
            } else {
                result.add(toSellOffer(t, stock));
            }
            offerIndex.add(t);
        }
        return result;
    }

    /** SELL: speler betaalt munten, krijgt item. Standaard gedrag. */
    private MerchantOffer toSellOffer(ShopTrade trade, Container stock) {
        long price = Math.max(1, trade.getPrice());
        int gold = (int) Math.min(64, price / WalletData.GOLD_VALUE);
        long rem = price - (long) gold * WalletData.GOLD_VALUE;

        ItemCost costA;
        Optional<ItemCost> costB;

        if (gold > 0) {
            costA = new ItemCost(ModItems.GOLD_COIN, gold);
            costB = rem <= 0 ? Optional.empty()
                    : rem <= 64 ? Optional.of(new ItemCost(ModItems.COPPER_COIN, (int) rem))
                    : Optional.of(new ItemCost(ModItems.SILVER_COIN, (int) Math.round(rem / (double) WalletData.SILVER_VALUE)));
        } else {
            int silver = (int) (price / WalletData.SILVER_VALUE);
            int copper = (int) (price % WalletData.SILVER_VALUE);
            if (silver > 0 && copper > 0) {
                costA = new ItemCost(ModItems.SILVER_COIN, silver);
                costB = Optional.of(new ItemCost(ModItems.COPPER_COIN, copper));
            } else if (silver > 0) {
                costA = new ItemCost(ModItems.SILVER_COIN, silver);
                costB = Optional.empty();
            } else {
                costA = new ItemCost(ModItems.COPPER_COIN, Math.max(1, copper));
                costB = Optional.empty();
            }
        }

        ItemStack result = trade.getItem();
        int bundle = Math.max(1, result.getCount());
        int maxUses = stock != null ? countInContainer(stock, result) / bundle : 0;
        return new MerchantOffer(costA, costB, result, 0, maxUses, 0, 0.0f);
    }

    /**
     * BUY: speler geeft item, krijgt munten (fysiek) + owner wallet wordt gedebiteerd.
     * costA = het item, result = muntwissel voor de prijs.
     * maxUses = hoeveel keer de eigenaar dit kan betalen EN er plek is in de kist.
     */
    private MerchantOffer toBuyOffer(ShopTrade trade, Container stock) {
        long price = Math.max(1, trade.getPrice());
        ItemStack itemToBuy = trade.getItem();

        // result: grootste denominatie die in één slot past
        ItemStack resultCoins = priceToCoins(price);

        // maxUses: begrensd door eigenaar-wallet en kistplek
        int maxUsesByWallet = Integer.MAX_VALUE;
        if (ownerUUID != null && level() instanceof ServerLevel sl) {
            long ownerBal = WalletData.get(sl.getServer()).getBalance(ownerUUID);
            maxUsesByWallet = (int) Math.min(999, ownerBal / price);
        }
        int maxUsesBySpace = (stock != null && containerHasSpace(stock, itemToBuy)) ? 999 : 0;
        int maxUses = Math.min(maxUsesByWallet, maxUsesBySpace);

        ItemCost costA = new ItemCost(itemToBuy.getItem(), Math.max(1, itemToBuy.getCount()));
        return new MerchantOffer(costA, Optional.empty(), resultCoins, 0, maxUses, 0, 0.0f);
    }

    /** Zet een koperen prijs om in de grootste munt die in één slot past (max 64). */
    private static ItemStack priceToCoins(long price) {
        if (price >= WalletData.GOLD_VALUE) {
            int gold = (int) Math.min(64, price / WalletData.GOLD_VALUE);
            return new ItemStack(ModItems.GOLD_COIN, gold);
        } else if (price >= WalletData.SILVER_VALUE) {
            int silver = (int) Math.min(64, price / WalletData.SILVER_VALUE);
            return new ItemStack(ModItems.SILVER_COIN, silver);
        } else {
            return new ItemStack(ModItems.COPPER_COIN, (int) Math.min(64, price));
        }
    }

    // ===== Merchant callbacks =====

    @Override public void setTradingPlayer(Player player) { this.tradingPlayer = player; }
    @Override public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) this.offers = buildOffers();
        return this.offers;
    }

    @Override public void overrideOffers(MerchantOffers o) { this.offers = o; }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();

        // zoek de bijbehorende ShopTrade op basis van positie in de offers-lijst
        MerchantOffers all = getOffers();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) == offer) { idx = i; break; }
        }
        ShopTrade source = (idx >= 0 && idx < offerIndex.size()) ? offerIndex.get(idx) : null;

        Container stock = getStockContainer();

        if (source != null && source.getMode() == ShopTradeMode.BUY_FROM_PLAYER) {
            // BUY: item is al door vanilla afgenomen van de speler, munt is al gegeven
            // Wij: item in kist deponeren + eigenaar-wallet debiteren
            if (stock != null) {
                depositToContainer(stock, source.getItem());
            }
            if (ownerUUID != null && level() instanceof ServerLevel sl) {
                WalletData.get(sl.getServer()).subtractBalance(ownerUUID, source.getPrice());
            }
            // Ververs offers zodat maxUses wordt bijgewerkt
            this.offers = null;
        } else {
            // SELL: standaard gedrag
            if (stock != null) {
                removeFromContainer(stock, offer.getResult(), offer.getResult().getCount());
                depositToContainer(stock, offer.getCostA().copy());
                ItemStack costB = offer.getCostB();
                if (!costB.isEmpty()) depositToContainer(stock, costB.copy());
            }
        }
    }

    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide(); }

    @Override
    public boolean stillValid(Player player) {
        return this.tradingPlayer == player && this.isAlive() && player.distanceToSqr(this) < 64.0;
    }

    // ===== Opslag =====

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.store("shopTrades", ShopTrade.CODEC.listOf(), List.copyOf(trades));
        if (ownerUUID != null) out.store("shopOwner", UUIDUtil.CODEC, ownerUUID);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        trades.clear();
        in.read("shopTrades", ShopTrade.CODEC.listOf()).ifPresent(trades::addAll);
        ownerUUID = in.read("shopOwner", UUIDUtil.CODEC).orElse(null);
        this.offers = null;
    }
}
