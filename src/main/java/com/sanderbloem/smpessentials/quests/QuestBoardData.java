package com.sanderbloem.smpessentials.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sanderbloem.smpessentials.CurrencyMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persistente staat van het Quest Board: actieve quests, community-progress en per-speler voortgang. */
public class QuestBoardData extends SavedData {

    private List<QuestDefinition> activeQuests = new ArrayList<>();
    private final Map<String, Long> communityProgress = new HashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> playerProgress = new HashMap<>();
    private int generation = 0;

    public static final Codec<QuestBoardData> CODEC = RecordCodecBuilder.create(i -> i.group(
            QuestDefinition.CODEC.listOf().fieldOf("activeQuests").forGetter(d -> d.activeQuests),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("communityProgress").forGetter(d -> d.communityProgress),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.unboundedMap(Codec.STRING, QuestProgress.CODEC))
                    .fieldOf("playerProgress").forGetter(d -> d.playerProgress),
            Codec.INT.fieldOf("generation").forGetter(d -> d.generation)
    ).apply(i, QuestBoardData::create));

    private static QuestBoardData create(List<QuestDefinition> active, Map<String, Long> community,
                                          Map<UUID, Map<String, QuestProgress>> progress, int generation) {
        QuestBoardData d = new QuestBoardData();
        d.activeQuests = new ArrayList<>(active);
        d.communityProgress.putAll(community);
        progress.forEach((k, v) -> d.playerProgress.put(k, new HashMap<>(v)));
        d.generation = generation;
        if (d.activeQuests.isEmpty()) d.activeQuests = defaultQuests(generation);
        return d;
    }

    private static final SavedDataType<QuestBoardData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(CurrencyMod.MOD_ID, "quest_board"), QuestBoardData::new, CODEC, null);

    public static QuestBoardData get(MinecraftServer server) {
        QuestBoardData data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        if (data.activeQuests.isEmpty()) {
            data.activeQuests = defaultQuests(data.generation);
            data.setDirty();
        }
        return data;
    }

    /** Vaste V1-quest-pool: precies één van elk type. Geen JSON-config, bewust simpel. */
    private static List<QuestDefinition> defaultQuests(int generation) {
        String suffix = "-" + generation;
        return List.of(
                new QuestDefinition("fetch" + suffix, "Houtleverantie",
                        "Lever 32x eikenhout in", QuestType.FETCH, Items.OAK_LOG, 32, 81L),
                new QuestDefinition("travel" + suffix, "Verkenner",
                        "Reis 500 blokken", QuestType.TRAVEL_DISTANCE, Items.AIR, 500, 81L),
                new QuestDefinition("community" + suffix, "Gemeenschapsproject",
                        "Server brengt samen 256x cobblestone in", QuestType.COMMUNITY_FETCH, Items.COBBLESTONE, 256, 27L)
        );
    }

    public List<QuestDefinition> getActiveQuests() { return List.copyOf(activeQuests); }

    /** Verwijdert oude quests en zet nieuwe instanties neer (oude progress raakt vanzelf wees, geen reset nodig). */
    public void reroll() {
        generation++;
        activeQuests = defaultQuests(generation);
        setDirty();
    }

    public QuestProgress getPlayerProgress(UUID player, String questId) {
        Map<String, QuestProgress> m = playerProgress.get(player);
        if (m == null) return new QuestProgress(player, questId, 0, false, false);
        return m.getOrDefault(questId, new QuestProgress(player, questId, 0, false, false));
    }

    public void setPlayerProgress(QuestProgress progress) {
        playerProgress.computeIfAbsent(progress.playerId(), k -> new HashMap<>()).put(progress.questId(), progress);
        setDirty();
    }

    public long getCommunityProgress(String questId) {
        return communityProgress.getOrDefault(questId, 0L);
    }

    /** Reset community-progress en wist alle per-speler claims/contributies voor deze quest. */
    public void resetQuest(String questId) {
        communityProgress.remove(questId);
        for (Map<String, QuestProgress> m : playerProgress.values()) {
            m.remove(questId);
        }
        setDirty();
    }

    public long addCommunityProgress(String questId, long amount) {
        long total = communityProgress.merge(questId, amount, Long::sum);
        setDirty();
        return total;
    }

    public boolean hasContributed(UUID player, String questId) {
        return getPlayerProgress(player, questId).amountProgress() > 0;
    }

    public void markContributed(UUID player, String questId, long amount) {
        QuestProgress p = getPlayerProgress(player, questId);
        setPlayerProgress(p.withProgress(p.amountProgress() + amount, p.completed()));
    }

    public boolean hasClaimed(UUID player, String questId) {
        return getPlayerProgress(player, questId).claimed();
    }

    public void markClaimed(UUID player, String questId) {
        setPlayerProgress(getPlayerProgress(player, questId).claim());
    }
}
