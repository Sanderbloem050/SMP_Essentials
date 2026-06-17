package com.sanderbloem.smpessentials.claims;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanderbloem.smpessentials.CurrencyMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Per-chunk eigendom + per-eigenaar trust-lijst. */
public class ClaimsData extends SavedData {

    private final Map<String, UUID> owners = new HashMap<>();
    private final Map<UUID, Set<UUID>> trusted = new HashMap<>();

    public static final Codec<ClaimsData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, UUIDUtil.STRING_CODEC).fieldOf("owners").forGetter(d -> d.owners),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC.listOf()).fieldOf("trusted").forGetter(ClaimsData::trustedAsList)
    ).apply(i, ClaimsData::create));

    private static Map<UUID, List<UUID>> trustedAsList(ClaimsData d) {
        Map<UUID, List<UUID>> m = new HashMap<>();
        d.trusted.forEach((k, v) -> m.put(k, new ArrayList<>(v)));
        return m;
    }

    private static ClaimsData create(Map<String, UUID> owners, Map<UUID, List<UUID>> trusted) {
        ClaimsData d = new ClaimsData();
        d.owners.putAll(owners);
        trusted.forEach((k, v) -> d.trusted.put(k, new HashSet<>(v)));
        return d;
    }

    private static final SavedDataType<ClaimsData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "claims"), ClaimsData::new, CODEC, null);

    public static ClaimsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static String key(Level level, BlockPos pos) {
        return chunkKey(level, pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static String chunkKey(Level level, int chunkX, int chunkZ) {
        return level.dimension().identifier() + ":" + chunkX + ":" + chunkZ;
    }

    public UUID ownerOf(String key) { return owners.get(key); }

    public boolean claim(String key, UUID owner) {
        if (owners.containsKey(key)) return false;
        owners.put(key, owner);
        setDirty();
        return true;
    }

    public boolean unclaim(String key, UUID owner) {
        UUID o = owners.get(key);
        if (o != null && o.equals(owner)) {
            owners.remove(key);
            setDirty();
            return true;
        }
        return false;
    }

    public long countClaims(UUID owner) {
        return owners.values().stream().filter(owner::equals).count();
    }

    public void trust(UUID owner, UUID other) {
        trusted.computeIfAbsent(owner, k -> new HashSet<>()).add(other);
        setDirty();
    }

    public boolean untrust(UUID owner, UUID other) {
        Set<UUID> t = trusted.get(owner);
        if (t != null && t.remove(other)) { setDirty(); return true; }
        return false;
    }

    /** Mag deze speler bouwen/interacteren op deze chunk? */
    public boolean canBuild(UUID player, String key, boolean op) {
        UUID owner = owners.get(key);
        if (owner == null) return true;       // niet geclaimd
        if (owner.equals(player) || op) return true;
        Set<UUID> t = trusted.get(owner);
        return t != null && t.contains(player);
    }
}
