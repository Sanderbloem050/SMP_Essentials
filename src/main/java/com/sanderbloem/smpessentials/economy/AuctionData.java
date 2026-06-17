package com.sanderbloem.smpessentials.economy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanderbloem.smpessentials.CurrencyMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionData extends SavedData {

    public record Listing(int id, UUID seller, String sellerName, ItemStack item, long price) {
        public static final Codec<Listing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("id").forGetter(Listing::id),
                UUIDUtil.CODEC.fieldOf("seller").forGetter(Listing::seller),
                Codec.STRING.fieldOf("sellerName").forGetter(Listing::sellerName),
                ItemStack.CODEC.fieldOf("item").forGetter(Listing::item),
                Codec.LONG.fieldOf("price").forGetter(Listing::price)
        ).apply(i, Listing::new));
    }

    private final Map<Integer, Listing> listings = new HashMap<>();
    private int nextId = 1;

    public static final Codec<AuctionData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("nextId").forGetter(d -> d.nextId),
            Listing.CODEC.listOf().fieldOf("listings").forGetter(d -> new ArrayList<>(d.listings.values()))
    ).apply(i, AuctionData::create));

    private static AuctionData create(int nextId, List<Listing> list) {
        AuctionData d = new AuctionData();
        d.nextId = nextId;
        for (Listing l : list) d.listings.put(l.id(), l);
        return d;
    }

    private static final SavedDataType<AuctionData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "auctions"), AuctionData::new, CODEC, null);

    public static AuctionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public int add(UUID seller, String name, ItemStack item, long price) {
        int id = nextId++;
        listings.put(id, new Listing(id, seller, name, item, price));
        setDirty();
        return id;
    }

    public Listing get(int id) { return listings.get(id); }

    public Listing remove(int id) {
        Listing l = listings.remove(id);
        if (l != null) setDirty();
        return l;
    }

    public Collection<Listing> all() { return listings.values(); }
}
