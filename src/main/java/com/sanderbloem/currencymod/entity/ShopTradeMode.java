package com.sanderbloem.currencymod.entity;

import com.mojang.serialization.Codec;

public enum ShopTradeMode {
    SELL_TO_PLAYER,
    BUY_FROM_PLAYER;

    public static final Codec<ShopTradeMode> CODEC = Codec.STRING.xmap(
            s -> s.equals("BUY") ? BUY_FROM_PLAYER : SELL_TO_PLAYER,
            m -> m == BUY_FROM_PLAYER ? "BUY" : "SELL"
    );
}
