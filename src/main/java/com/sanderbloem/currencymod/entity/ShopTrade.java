package com.sanderbloem.currencymod.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public class ShopTrade {

    private final ItemStack item;
    private final long priceCopper;
    private final ShopTradeMode mode;

    public static final Codec<ShopTrade> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.CODEC.fieldOf("item").forGetter(t -> t.item),
            Codec.LONG.fieldOf("price").forGetter(t -> t.priceCopper),
            ShopTradeMode.CODEC.optionalFieldOf("mode", ShopTradeMode.SELL_TO_PLAYER).forGetter(t -> t.mode)
    ).apply(inst, ShopTrade::new));

    public ShopTrade(ItemStack item, long priceCopper, ShopTradeMode mode) {
        this.item = item.copy();
        this.priceCopper = priceCopper;
        this.mode = mode;
    }

    public ItemStack getItem() { return item.copy(); }
    public long getPrice() { return priceCopper; }
    public ShopTradeMode getMode() { return mode; }
}
