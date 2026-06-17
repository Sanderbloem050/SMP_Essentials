package com.sanderbloem.currencymod.quests;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** V1-quest-categorieën. Bewust beperkt: economy-only, geen buffs/progressie. */
public enum QuestType implements StringRepresentable {
    FETCH("fetch"),
    TRAVEL_DISTANCE("travel_distance"),
    COMMUNITY_FETCH("community_fetch");

    public static final Codec<QuestType> CODEC = StringRepresentable.fromEnum(QuestType::values);

    private final String key;

    QuestType(String key) { this.key = key; }

    @Override
    public String getSerializedName() { return key; }
}
