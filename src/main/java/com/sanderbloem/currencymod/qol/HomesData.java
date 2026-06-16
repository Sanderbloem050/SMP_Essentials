package com.sanderbloem.currencymod.qol;

import com.mojang.serialization.Codec;
import com.sanderbloem.currencymod.CurrencyMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class HomesData extends SavedData {

    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();

    public static final Codec<HomesData> CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC,
            Codec.unboundedMap(Codec.STRING, Location.CODEC)
    ).xmap(HomesData::fromMap, h -> h.homes);

    private static HomesData fromMap(Map<UUID, Map<String, Location>> map) {
        HomesData d = new HomesData();
        map.forEach((u, m) -> d.homes.put(u, new HashMap<>(m)));
        return d;
    }

    private static final SavedDataType<HomesData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "homes"), HomesData::new, CODEC, null);

    public static HomesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void setHome(UUID id, String name, Location loc) {
        homes.computeIfAbsent(id, k -> new HashMap<>()).put(name, loc);
        setDirty();
    }

    public Location getHome(UUID id, String name) {
        Map<String, Location> m = homes.get(id);
        return m == null ? null : m.get(name);
    }

    public boolean delHome(UUID id, String name) {
        Map<String, Location> m = homes.get(id);
        if (m != null && m.remove(name) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public Set<String> listHomes(UUID id) {
        Map<String, Location> m = homes.get(id);
        return m == null ? Set.of() : m.keySet();
    }
}
