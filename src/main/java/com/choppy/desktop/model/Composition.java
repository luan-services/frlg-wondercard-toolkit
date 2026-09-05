package com.choppy.desktop.model;

public record Composition(int runtimeUsed, int runtimeCapacity, int sb1Used, int sb1Capacity,
                          int sb2Used, int sb2Capacity, int hotkeys, String status) {}
