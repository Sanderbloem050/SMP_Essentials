package com.sanderbloem.currencymod.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

/** Per-speler voortgang op een quest (gebruikt voor TRAVEL_DISTANCE en COMMUNITY_FETCH-deelname). */
public record QuestProgress(UUID playerId, String questId, long amountProgress, boolean completed, boolean claimed) {

    public static final Codec<QuestProgress> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("playerId").forGetter(QuestProgress::playerId),
            Codec.STRING.fieldOf("questId").forGetter(QuestProgress::questId),
            Codec.LONG.fieldOf("amountProgress").forGetter(QuestProgress::amountProgress),
            Codec.BOOL.fieldOf("completed").forGetter(QuestProgress::completed),
            Codec.BOOL.fieldOf("claimed").forGetter(QuestProgress::claimed)
    ).apply(i, QuestProgress::new));

    public QuestProgress withProgress(long amount, boolean completed) {
        return new QuestProgress(playerId, questId, amount, completed, claimed);
    }

    public QuestProgress claim() {
        return new QuestProgress(playerId, questId, amountProgress, completed, true);
    }
}
