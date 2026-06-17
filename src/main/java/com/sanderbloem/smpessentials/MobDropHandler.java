package com.sanderbloem.smpessentials;

import com.sanderbloem.smpessentials.entity.ShopkeeperEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.skeleton.Stray;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.item.ItemStack;

public class MobDropHandler {

    public static void onDeath(LivingEntity entity, DamageSource source) {
        if (entity instanceof ShopkeeperEntity) return;
        if (source.getEntity() == null) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        RandomSource random = entity.level().getRandom();
        int copper = 0, silver = 0, gold = 0;

        if (entity instanceof WitherBoss) {
            gold = 2 + random.nextInt(2);
        } else if (entity instanceof EnderDragon) {
            gold = 3 + random.nextInt(3);
        } else if (entity instanceof ElderGuardian) {
            silver = 3 + random.nextInt(3);
        } else if (entity instanceof Warden) {
            silver = 2 + random.nextInt(3);
        } else if (entity instanceof EnderMan) {
            silver = 1;
        } else if (entity instanceof Blaze || entity instanceof WitherSkeleton) {
            copper = 5 + random.nextInt(6);
        } else if (entity instanceof Creeper) {
            copper = 4 + random.nextInt(7);
        } else if (entity instanceof Stray || entity instanceof Skeleton) {
            copper = 2 + random.nextInt(4);
        } else if (entity instanceof Husk || entity instanceof ZombieVillager || entity instanceof Zombie) {
            copper = 1 + random.nextInt(3);
        } else if (entity instanceof CaveSpider || entity instanceof Spider) {
            copper = 1 + random.nextInt(3);
        } else if (entity instanceof Pillager || entity instanceof Vindicator) {
            copper = 3 + random.nextInt(5);
        } else if (entity instanceof Ravager) {
            silver = 1 + random.nextInt(2);
        } else if (entity instanceof Piglin) {
            copper = 2 + random.nextInt(4);
        }

        if (copper > 0) drop(serverLevel, entity, ModItems.COPPER_COIN, copper);
        if (silver > 0) drop(serverLevel, entity, ModItems.SILVER_COIN, silver);
        if (gold > 0)   drop(serverLevel, entity, ModItems.GOLD_COIN,   gold);
    }

    private static void drop(ServerLevel level, LivingEntity entity, net.minecraft.world.item.Item item, int count) {
        entity.spawnAtLocation(level, new ItemStack(item, count));
    }
}
