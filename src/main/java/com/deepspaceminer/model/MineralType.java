package com.deepspaceminer.model;

import com.jme3.math.ColorRGBA;

import java.util.Random;

/** Kinds of minerals that can be mined, with their look, rarity and yield. */
public enum MineralType {

    IRON("Iron", ColorRGBA.Brown, 6, 3),
    GOLD("Gold", ColorRGBA.Yellow, 3, 2),
    CRYSTAL("Crystal", ColorRGBA.Magenta, 1, 1);

    private final String displayName;
    private final ColorRGBA color;
    private final int spawnWeight;
    private final int yield;

    MineralType(String displayName, ColorRGBA color, int spawnWeight, int yield) {
        this.displayName = displayName;
        this.color = color;
        this.spawnWeight = spawnWeight;
        this.yield = yield;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ColorRGBA getColor() {
        return color;
    }

    /** Resource units added to the inventory when one mineral is mined. */
    public int getYield() {
        return yield;
    }

    /** Picks a random type; types with a higher spawn weight are more common. */
    public static MineralType pickRandom(Random random) {
        int totalWeight = 0;
        for (MineralType type : values()) {
            totalWeight += type.spawnWeight;
        }

        int roll = random.nextInt(totalWeight);
        for (MineralType type : values()) {
            roll -= type.spawnWeight;
            if (roll < 0) {
                return type;
            }
        }
        throw new IllegalStateException("Unreachable: weights were not exhausted");
    }
}