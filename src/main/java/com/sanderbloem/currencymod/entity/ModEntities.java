package com.sanderbloem.currencymod.entity;

import com.sanderbloem.currencymod.CurrencyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    private static final ResourceKey<EntityType<?>> SHOPKEEPER_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "shopkeeper"));

    public static final EntityType<ShopkeeperEntity> SHOPKEEPER = EntityType.Builder
            .<ShopkeeperEntity>of(ShopkeeperEntity::new, MobCategory.MISC)
            .sized(0.6f, 1.95f)
            .build(SHOPKEEPER_KEY);

    public static void register() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "shopkeeper"), SHOPKEEPER);
    }
}
