package com.sanderbloem.smpessentials.qol;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

/** Een opgeslagen positie incl. dimensie en kijkrichting. */
public record Location(ResourceKey<Level> dimension, double x, double y, double z, float yaw, float pitch) {

    public static final Codec<Location> CODEC = RecordCodecBuilder.create(i -> i.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dim").forGetter(Location::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(Location::x),
            Codec.DOUBLE.fieldOf("y").forGetter(Location::y),
            Codec.DOUBLE.fieldOf("z").forGetter(Location::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(Location::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Location::pitch)
    ).apply(i, Location::new));

    public static Location of(ServerPlayer p) {
        return new Location(p.level().dimension(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
    }

    /** Teleporteer een speler hierheen. False als de dimensie niet bestaat. */
    public boolean teleport(ServerPlayer player) {
        ServerLevel target = player.level().getServer().getLevel(dimension);
        if (target == null) return false;
        player.teleportTo(target, x, y, z, Set.of(), yaw, pitch, false);
        return true;
    }

    public String pretty() {
        String dim = dimension.identifier().getPath();
        return String.format("%s  %.0f, %.0f, %.0f", dim, x, y, z);
    }
}
