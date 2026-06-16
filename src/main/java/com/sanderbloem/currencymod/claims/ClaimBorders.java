package com.sanderbloem.currencymod.claims;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Toont gekleurde particle-randen rond geclaimde chunks voor spelers die het aanzetten. */
public class ClaimBorders {

    private static final Set<UUID> enabled = new HashSet<>();
    private static final int OWN_COLOR = 0x44DD55;     // groen = van jou
    private static final int OTHER_COLOR = 0xDD4444;   // rood = van iemand anders
    private static final int RADIUS = 2;               // chunks rond de speler

    /** Aan/uit; geeft de nieuwe stand terug. */
    public static boolean toggle(UUID id) {
        if (enabled.remove(id)) return false;
        enabled.add(id);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (enabled.isEmpty() || server.getTickCount() % 10 != 0) return;
        ClaimsData data = ClaimsData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (enabled.contains(p.getUUID())) render(p, data);
        }
    }

    private static void render(ServerPlayer p, ClaimsData data) {
        ServerLevel level = p.level();
        int pcx = p.blockPosition().getX() >> 4;
        int pcz = p.blockPosition().getZ() >> 4;
        double y = p.getY() + 0.1;

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                int cx = pcx + dx, cz = pcz + dz;
                UUID owner = data.ownerOf(ClaimsData.chunkKey(level, cx, cz));
                if (owner == null) continue;
                int color = owner.equals(p.getUUID()) ? OWN_COLOR : OTHER_COLOR;
                DustParticleOptions dust = new DustParticleOptions(color, 1.2f);
                drawEdges(level, p, dust, cx, cz, y);
            }
        }
    }

    private static void drawEdges(ServerLevel lvl, ServerPlayer p, DustParticleOptions dust, int cx, int cz, double y) {
        int x0 = cx << 4, z0 = cz << 4;
        for (int i = 0; i <= 16; i += 2) {
            send(lvl, p, dust, x0 + i, y, z0);          // noord
            send(lvl, p, dust, x0 + i, y, z0 + 16);     // zuid
            send(lvl, p, dust, x0, y, z0 + i);          // west
            send(lvl, p, dust, x0 + 16, y, z0 + i);     // oost
        }
    }

    private static void send(ServerLevel lvl, ServerPlayer p, DustParticleOptions dust, double x, double y, double z) {
        lvl.sendParticles(p, dust, true, true, x, y, z, 1, 0, 0, 0, 0);
    }
}
