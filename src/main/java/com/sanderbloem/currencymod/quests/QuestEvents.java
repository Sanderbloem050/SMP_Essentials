package com.sanderbloem.currencymod.quests;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Houdt per-speler de gereisde afstand bij voor TRAVEL_DISTANCE-quests. */
public class QuestEvents {

    // Boven deze afstand per tick gaat het om een teleport (/home, /warp, /tpa, dood) — niet meetellen.
    private static final double MAX_VALID_TICK_DISTANCE = 10.0;

    public static void tick(MinecraftServer server) {
        QuestBoardData data = QuestBoardData.get(server);
        QuestDefinition travelQuest = data.getActiveQuests().stream()
                .filter(q -> q.type() == QuestType.TRAVEL_DISTANCE)
                .findFirst().orElse(null);
        if (travelQuest == null) return;

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (data.hasClaimed(p.getUUID(), travelQuest.id())) continue;

            Vec3 from = p.oldPosition();
            Vec3 to = p.position();
            double dist = from.distanceTo(to);
            if (dist <= 0 || dist > MAX_VALID_TICK_DISTANCE) continue;

            QuestProgress current = data.getPlayerProgress(p.getUUID(), travelQuest.id());
            long updated = current.amountProgress() + Math.round(dist);
            boolean completed = updated >= travelQuest.amount();
            data.setPlayerProgress(current.withProgress(updated, completed));
        }
    }
}
