package com.sanderbloem.smpessentials.entity;

import com.sanderbloem.smpessentials.data.WalletData;
import net.minecraft.commands.Commands;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class QuestGiverEntity extends PathfinderMob {

    private UUID ownerUUID = null;
    private ItemStack questItem = ItemStack.EMPTY; // count = vereiste hoeveelheid
    private long reward = 0L;
    private boolean onceOnly = false;
    private final Set<UUID> completed = new HashSet<>();

    public QuestGiverEntity(EntityType<? extends PathfinderMob> type, net.minecraft.world.level.Level world) {
        super(type, world);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean isInvulnerableTo(ServerLevel level, net.minecraft.world.damagesource.DamageSource source) { return true; }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }

    @Override
    public boolean isPushable() { return false; }

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
        if (level().isClientSide()) return InteractionResult.SUCCESS;

        boolean isOwnerOrOp = Commands.LEVEL_GAMEMASTERS.check(player.permissions()) ||
                (ownerUUID != null && ownerUUID.equals(player.getUUID()));

        if (player.isShiftKeyDown() && isOwnerOrOp) {
            sendAdminInfo((ServerPlayer) player);
        } else {
            turnIn((ServerPlayer) player);
        }
        return InteractionResult.SUCCESS;
    }

    private void sendAdminInfo(ServerPlayer p) {
        p.sendSystemMessage(Component.literal("§6=== Quest beheer ==="));
        if (questItem.isEmpty()) {
            p.sendSystemMessage(Component.literal("§7Nog geen quest ingesteld."));
        } else {
            p.sendSystemMessage(Component.literal("§7Vereist: §f" + questItem.getCount() + "× " + questItem.getHoverName().getString()));
            p.sendSystemMessage(Component.literal("§7Beloning: §6" + WalletData.formatBalance(reward)));
            p.sendSystemMessage(Component.literal("§7Eenmalig: §f" + (onceOnly ? "ja" : "nee")));
        }
        p.sendSystemMessage(Component.literal("§7/questgiver setitem <aantal> · setreward <bedrag> · toggleonce · remove"));
    }

    private void turnIn(ServerPlayer p) {
        if (questItem.isEmpty()) {
            p.sendSystemMessage(Component.literal("§7Deze NPC heeft nog geen quest."));
            return;
        }
        if (onceOnly && completed.contains(p.getUUID())) {
            p.sendSystemMessage(Component.literal("§7Je hebt deze quest al voltooid."));
            return;
        }
        int needed = questItem.getCount();
        int have = countMatching(p, questItem);
        if (have < needed) {
            p.sendSystemMessage(Component.literal("§cJe hebt nog §f" + (needed - have) + "× " +
                    questItem.getHoverName().getString() + " §cnodig."));
            return;
        }
        removeMatching(p, questItem, needed);
        WalletData.get(p.level().getServer()).addBalance(p.getUUID(), reward);
        if (onceOnly) completed.add(p.getUUID());

        p.sendSystemMessage(Component.literal("§aQuest voltooid! §7Je ontving §6" + WalletData.formatBalance(reward)));
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.2f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 1.0, getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        }
    }

    private static int countMatching(ServerPlayer p, ItemStack ref) {
        int total = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, ref)) total += s.getCount();
        }
        return total;
    }

    private static void removeMatching(ServerPlayer p, ItemStack ref, int amount) {
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

    public void setQuest(ItemStack item) { this.questItem = item.copy(); }
    public void setReward(long amount) { this.reward = amount; }
    public void toggleOnce() { this.onceOnly = !this.onceOnly; }
    public boolean isOnceOnly() { return onceOnly; }
    public ItemStack getQuestItem() { return questItem; }
    public long getReward() { return reward; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(UUID uuid) { this.ownerUUID = uuid; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        if (ownerUUID != null) out.store("owner", UUIDUtil.CODEC, ownerUUID);
        if (!questItem.isEmpty()) out.store("questItem", ItemStack.CODEC, questItem);
        out.putLong("reward", reward);
        out.putBoolean("onceOnly", onceOnly);
        out.store("completed", UUIDUtil.CODEC_SET, completed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        ownerUUID = in.read("owner", UUIDUtil.CODEC).orElse(null);
        questItem = in.read("questItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        reward = in.getLongOr("reward", 0L);
        onceOnly = in.getBooleanOr("onceOnly", false);
        completed.clear();
        in.read("completed", UUIDUtil.CODEC_SET).ifPresent(completed::addAll);
    }
}
