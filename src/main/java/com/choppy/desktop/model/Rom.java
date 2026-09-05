package com.choppy.desktop.model;

public enum Rom {
    FIRE_RED_10("FireRed EN 1.0"), LEAF_GREEN_10("LeafGreen EN 1.0"),
    FIRE_RED_11("FireRed EN 1.1"), LEAF_GREEN_11("LeafGreen EN 1.1");
    private final String label;
    Rom(String label) { this.label = label; }
    @Override public String toString() { return label; }
}
