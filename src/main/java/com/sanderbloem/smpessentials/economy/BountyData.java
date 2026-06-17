package com.sanderbloem.smpessentials.economy;

import com.mojang.serialization.Codec;
import com.sanderbloem.smpessentials.CurrencyMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bounties: opgestapelde beloningen op het hoofd van spelers. */
public class BountyData extends SavedData {

    private final Map<UUID, Long> bounties = new HashMap<>();

    public static final Codec<BountyData> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG)
            .xmap(BountyData::fromMap, b -> b.bounties);

    private static BountyData fromMap(Map<UUID, Long> map) {
        BountyData d = new BountyData();
        d.bounties.putAll(map);
        return d;
    }

    private static final SavedDataType<BountyData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "bounties"), BountyData::new, CODEC, null);

    public static BountyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void add(UUID target, long amount) {
        bounties.merge(target, amount, Long::sum);
        setDirty();
    }

    public long get(UUID target) {
        return bounties.getOrDefault(target, 0L);
    }

    /** Haalt de bounty op en verwijdert hem (na uitbetaling). */
    public long claim(UUID target) {
        Long v = bounties.remove(target);
        if (v != null && v > 0) setDirty();
        return v == null ? 0L : v;
    }

    public Map<UUID, Long> all() { return bounties; }
}
