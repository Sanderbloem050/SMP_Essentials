package com.sanderbloem.currencymod.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanderbloem.currencymod.CurrencyMod;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-instellingen om hele mod-onderdelen aan/uit te zetten. */
public class ModConfig extends SavedData {

    public boolean jobsEnabled = true;
    public boolean claimsEnabled = true;
    public boolean deathChestEnabled = true;
    public boolean cratesEnabled = true;
    public boolean bountiesEnabled = true;
    public boolean auctionEnabled = true;

    public static final Codec<ModConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("jobs", true).forGetter(c -> c.jobsEnabled),
            Codec.BOOL.optionalFieldOf("claims", true).forGetter(c -> c.claimsEnabled),
            Codec.BOOL.optionalFieldOf("deathChest", true).forGetter(c -> c.deathChestEnabled),
            Codec.BOOL.optionalFieldOf("crates", true).forGetter(c -> c.cratesEnabled),
            Codec.BOOL.optionalFieldOf("bounties", true).forGetter(c -> c.bountiesEnabled),
            Codec.BOOL.optionalFieldOf("auction", true).forGetter(c -> c.auctionEnabled)
    ).apply(i, ModConfig::create));

    private static ModConfig create(boolean jobs, boolean claims, boolean deathChest,
                                     boolean crates, boolean bounties, boolean auction) {
        ModConfig c = new ModConfig();
        c.jobsEnabled = jobs;
        c.claimsEnabled = claims;
        c.deathChestEnabled = deathChest;
        c.cratesEnabled = crates;
        c.bountiesEnabled = bounties;
        c.auctionEnabled = auction;
        return c;
    }

    private static final SavedDataType<ModConfig> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "config"), ModConfig::new, CODEC, null);

    public static ModConfig get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
}
