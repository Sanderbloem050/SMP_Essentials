package com.sanderbloem.currencymod.block;

import com.sanderbloem.currencymod.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public enum CrateType {

    BRONZE {
        @Override public Item key() { return ModItems.BRONZE_KEY; }
        @Override public ParticleOptions particles() { return ParticleTypes.SMOKE; }
        @Override public String color() { return "§6"; }
        @Override public List<ItemStack> roll(RandomSource rng) {
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int r = rng.nextInt(100);
                if      (r < 40) out.add(new ItemStack(ModItems.COPPER_COIN, 2 + rng.nextInt(4)));
                else if (r < 65) out.add(new ItemStack(ModItems.SILVER_COIN, 1 + rng.nextInt(2)));
                else if (r < 78) out.add(new ItemStack(Items.IRON_INGOT, 3 + rng.nextInt(6)));
                else if (r < 88) out.add(new ItemStack(Items.BREAD, 4 + rng.nextInt(4)));
                else if (r < 95) out.add(new ItemStack(ModItems.GOLD_COIN, 1));
                else             out.add(new ItemStack(Items.EMERALD, 1));
            }
            return out;
        }
    },

    SILVER {
        @Override public Item key() { return ModItems.SILVER_KEY; }
        @Override public ParticleOptions particles() { return ParticleTypes.CRIT; }
        @Override public String color() { return "§7"; }
        @Override public List<ItemStack> roll(RandomSource rng) {
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                int r = rng.nextInt(100);
                if      (r < 30) out.add(new ItemStack(ModItems.GOLD_COIN, 1 + rng.nextInt(2)));
                else if (r < 55) out.add(new ItemStack(ModItems.SILVER_COIN, 3 + rng.nextInt(5)));
                else if (r < 70) out.add(new ItemStack(Items.DIAMOND, 1 + rng.nextInt(2)));
                else if (r < 82) out.add(new ItemStack(Items.IRON_INGOT, 6 + rng.nextInt(8)));
                else if (r < 91) out.add(new ItemStack(Items.EMERALD, 2 + rng.nextInt(3)));
                else if (r < 97) out.add(new ItemStack(ModItems.GOLD_COIN, 3 + rng.nextInt(3)));
                else             out.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
            }
            return out;
        }
    },

    GOLD {
        @Override public Item key() { return ModItems.GOLD_KEY; }
        @Override public ParticleOptions particles() { return ParticleTypes.TOTEM_OF_UNDYING; }
        @Override public String color() { return "§e"; }
        @Override public List<ItemStack> roll(RandomSource rng) {
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                int r = rng.nextInt(100);
                if      (r < 25) out.add(new ItemStack(ModItems.GOLD_COIN, 3 + rng.nextInt(5)));
                else if (r < 45) out.add(new ItemStack(Items.DIAMOND, 2 + rng.nextInt(4)));
                else if (r < 60) out.add(new ItemStack(Items.EMERALD, 3 + rng.nextInt(5)));
                else if (r < 72) out.add(new ItemStack(Items.NETHERITE_SCRAP, 1 + rng.nextInt(2)));
                else if (r < 82) out.add(new ItemStack(ModItems.GOLD_COIN, 8 + rng.nextInt(8)));
                else if (r < 92) out.add(new ItemStack(Items.DIAMOND_BLOCK, 1));
                else             out.add(new ItemStack(Items.NETHERITE_INGOT, 1));
            }
            return out;
        }
    };

    public abstract Item key();
    public abstract ParticleOptions particles();
    public abstract String color();
    public abstract java.util.List<ItemStack> roll(RandomSource rng);
}
