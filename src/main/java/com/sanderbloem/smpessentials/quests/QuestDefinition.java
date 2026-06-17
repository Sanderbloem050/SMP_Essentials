package com.sanderbloem.smpessentials.quests;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Quest-sjabloon (geen per-speler state). Voor FETCH/COMMUNITY_FETCH is {@code item}
 * het vereiste item en {@code amount} de hoeveelheid; voor TRAVEL_DISTANCE is
 * {@code amount} het aantal te reizen blokken en {@code item} ongebruikt.
 */
public record QuestDefinition(String id, String title, String description, QuestType type,
                               Item item, int amount, long reward) {

    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(QuestDefinition::id),
            Codec.STRING.fieldOf("title").forGetter(QuestDefinition::title),
            Codec.STRING.fieldOf("description").forGetter(QuestDefinition::description),
            QuestType.CODEC.fieldOf("type").forGetter(QuestDefinition::type),
            BuiltInRegistries.ITEM.byNameCodec().optionalFieldOf("item", Items.AIR).forGetter(QuestDefinition::item),
            Codec.INT.fieldOf("amount").forGetter(QuestDefinition::amount),
            Codec.LONG.fieldOf("reward").forGetter(QuestDefinition::reward)
    ).apply(i, QuestDefinition::new));
}
