package com.sanderbloem.currencymod.qol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Onthoudt de vorige locatie van een speler (voor teleports) en sterfplekken. */
public class BackManager {

    private static final Map<UUID, Location> lastLocation = new HashMap<>();
    private static final Map<UUID, Location> deathLocation = new HashMap<>();

    public static void setLast(UUID id, Location loc) { lastLocation.put(id, loc); }
    public static Location getLast(UUID id) { return lastLocation.get(id); }

    public static void setDeath(UUID id, Location loc) { deathLocation.put(id, loc); }

    /** /back gaat naar de sterfplek als die er is, anders naar de laatste teleport-locatie. */
    public static Location consumeBackTarget(UUID id) {
        Location death = deathLocation.remove(id);
        if (death != null) return death;
        return lastLocation.get(id);
    }
}
