package com.choppy.desktop.service;

import com.choppy.desktop.model.*;
import java.util.List;
import java.util.Set;

public final class MockToolkitService implements ToolkitService {
    @Override public List<Preset> listPresets(Rom rom) {
        return List.of(
            preset("seed", "Seed Modifier", "R+SELECT"),
            preset("box14", "BOX14 Seed Modifier", "R+SELECT"),
            preset("repel", "Repel", "R+B"),
            preset("party-iv", "Party IV Viewer", "R+A"),
            preset("lead-iv", "Lead IV Viewer", "R+LEFT"),
            preset("party-ev", "Party EV Viewer", "R+UP"),
            preset("lead-ev", "Lead EV Viewer", "R+RIGHT"),
            preset("secret-id", "Show Secret ID", "R+START"),
            preset("mute", "Mute Music", "R+DOWN"),
            preset("run", "Run Anywhere", "R+RIGHT"),
            preset("bike", "Run + Bike Anywhere", "R+RIGHT"));
    }
    private Preset preset(String id, String name, String hotkey) {
        return new Preset(id, name, hotkey, "Validated · demo");
    }
    @Override public Composition planComposition(Rom rom, Set<String> presetIds) {
        // Deliberately fixed sample: never simulate toolkit composition rules in the UI.
        return new Composition(878, 995, 40, 400, 991, 1024, 4, "Valid composition");
    }
}
