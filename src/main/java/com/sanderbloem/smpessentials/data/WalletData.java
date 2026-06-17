package com.sanderbloem.smpessentials.data;

import com.mojang.serialization.Codec;
import com.sanderbloem.smpessentials.CurrencyMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WalletData extends SavedData {

    public static final long SILVER_VALUE = 9L;
    public static final long GOLD_VALUE   = 81L;

    private final Map<UUID, Long> balances = new HashMap<>();

    public static final Codec<WalletData> CODEC =
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG)
                    .xmap(WalletData::fromMap, w -> w.balances);

    private static WalletData fromMap(Map<UUID, Long> map) {
        WalletData data = new WalletData();
        data.balances.putAll(map);
        return data;
    }

    private static final SavedDataType<WalletData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "wallets"),
            WalletData::new,
            CODEC,
            null
    );

    public static WalletData get(MinecraftServer server) {
        SavedDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(TYPE);
    }

    public long getBalance(UUID id) {
        return balances.getOrDefault(id, 0L);
    }

    public void addBalance(UUID id, long amount) {
        balances.merge(id, amount, Long::sum);
        setDirty();
    }

    public boolean subtractBalance(UUID id, long amount) {
        long current = getBalance(id);
        if (current < amount) return false;
        balances.put(id, current - amount);
        setDirty();
        return true;
    }

    public static String formatBalance(long copper) {
        long gold = copper / GOLD_VALUE;
        long rem1 = copper % GOLD_VALUE;
        long silver = rem1 / SILVER_VALUE;
        long copperLeft = rem1 % SILVER_VALUE;
        StringBuilder sb = new StringBuilder();
        if (gold > 0) sb.append(gold).append("g ");
        if (silver > 0) sb.append(silver).append("s ");
        sb.append(copperLeft).append("c");
        return sb.toString().trim();
    }
}
