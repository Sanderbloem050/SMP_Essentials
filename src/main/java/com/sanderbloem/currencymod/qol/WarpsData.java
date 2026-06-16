package com.sanderbloem.currencymod.qol;

import com.mojang.serialization.Codec;
import com.sanderbloem.currencymod.CurrencyMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WarpsData extends SavedData {

    private final Map<String, Location> warps = new HashMap<>();

    public static final Codec<WarpsData> CODEC = Codec.unboundedMap(Codec.STRING, Location.CODEC)
            .xmap(WarpsData::fromMap, w -> w.warps);

    private static WarpsData fromMap(Map<String, Location> map) {
        WarpsData d = new WarpsData();
        d.warps.putAll(map);
        return d;
    }

    private static final SavedDataType<WarpsData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "warps"), WarpsData::new, CODEC, null);

    public static WarpsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void setWarp(String name, Location loc) { warps.put(name, loc); setDirty(); }
    public Location getWarp(String name) { return warps.get(name); }
    public boolean delWarp(String name) {
        if (warps.remove(name) != null) { setDirty(); return true; }
        return false;
    }
    public Set<String> listWarps() { return warps.keySet(); }
}
