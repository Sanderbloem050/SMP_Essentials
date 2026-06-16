package com.sanderbloem.currencymod.qol;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Beheert openstaande teleport-verzoeken in het geheugen (niet persistent). */
public class TpaManager {

    public record Request(UUID from, boolean here, long expiry) {}

    private static final long TIMEOUT_MS = 120_000; // 2 minuten
    private static final Map<UUID, Request> byTarget = new HashMap<>();

    public static void add(UUID target, UUID from, boolean here) {
        byTarget.put(target, new Request(from, here, System.currentTimeMillis() + TIMEOUT_MS));
    }

    /** Haalt het verzoek op en verwijdert het; null als er geen geldig verzoek is. */
    public static Request consume(UUID target) {
        Request r = byTarget.remove(target);
        if (r == null || r.expiry() < System.currentTimeMillis()) return null;
        return r;
    }

    public static boolean deny(UUID target) {
        return byTarget.remove(target) != null;
    }
}
