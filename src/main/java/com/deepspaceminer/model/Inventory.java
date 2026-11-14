package com.deepspaceminer.model;

import java.util.EnumMap;
import java.util.Map;

/** Stores how many units of each mineral the player has collected. */
public class Inventory {

    private final Map<MineralType, Integer> amounts = new EnumMap<>(MineralType.class);

    public void add(MineralType type, int amount) {
        amounts.merge(type, amount, Integer::sum);
    }

    public int get(MineralType type) {
        return amounts.getOrDefault(type, 0);
    }
}