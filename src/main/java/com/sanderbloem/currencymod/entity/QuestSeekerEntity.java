package com.sanderbloem.currencymod.entity;

import com.sanderbloem.currencymod.data.WalletData;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Een verstopte schatzoeker-NPC: elke speler die hem vindt mag eenmalig de beloning claimen. */
public class QuestSeekerEntity extends PathfinderMob {

    private long reward = 0L;
    private UUID firstFinder = null;
    private final Set<UUID> claimed = new HashSet<>();

    public QuestSeekerEntity(EntityType<? extends PathfinderMob> type, net.minecraft.world.level.Level world) {
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
        claim((ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }

    private void claim(ServerPlayer p) {
        if (claimed.contains(p.getUUID())) {
            p.sendSystemMessage(Component.literal("§7Je hebt deze Quest Seeker al gevonden."));
            return;
        }
        claimed.add(p.getUUID());
        WalletData.get(p.level().getServer()).addBalance(p.getUUID(), reward);
        p.sendSystemMessage(Component.literal(
                "§6Je hebt de Quest Seeker gevonden! §7Je ontving §6" + WalletData.formatBalance(reward)));

        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.7f, 1.0f);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.TOTEM_OF_UNDYING,
                    getX(), getY() + 1.0, getZ(), 20, 0.5, 0.5, 0.5, 0.1);

            if (firstFinder == null) {
                firstFinder = p.getUUID();
                String msg = "§6" + p.getName().getString() + " §ewas de eerste die de §6Quest Seeker §eheeft gevonden!";
                sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
            }
        }
    }

    public void setReward(long amount) { this.reward = amount; }
    public long getReward() { return reward; }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putLong("reward", reward);
        if (firstFinder != null) out.store("firstFinder", UUIDUtil.CODEC, firstFinder);
        out.store("claimed", UUIDUtil.CODEC_SET, claimed);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        reward = in.getLongOr("reward", 0L);
        firstFinder = in.read("firstFinder", UUIDUtil.CODEC).orElse(null);
        claimed.clear();
        in.read("claimed", UUIDUtil.CODEC_SET).ifPresent(claimed::addAll);
    }
}
