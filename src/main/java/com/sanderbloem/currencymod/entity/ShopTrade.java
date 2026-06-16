package com.sanderbloem.currencymod.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public class ShopTrade {

    private final ItemStack item;
    private final long priceCopper;

    public static final Codec<ShopTrade> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStack.CODEC.fieldOf("item").forGetter(t -> t.item),
            Codec.LONG.fieldOf("price").forGetter(t -> t.priceCopper)
    ).apply(inst, ShopTrade::new));

    public ShopTrade(ItemStack item, long priceCopper) {
        this.item = item.copy();
        this.priceCopper = priceCopper;
    }

    public ItemStack getItem() { return item.copy(); }
    public long getPrice() { return priceCopper; }
}
