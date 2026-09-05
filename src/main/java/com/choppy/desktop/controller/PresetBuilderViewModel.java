package com.choppy.desktop.controller;

import com.choppy.desktop.model.*;
import com.choppy.desktop.service.ToolkitService;
import javafx.beans.property.*;
import javafx.collections.*;
import java.io.File;
import java.util.Set;

public final class PresetBuilderViewModel {
    private final ToolkitService service;
    public final ObjectProperty<Rom> rom = new SimpleObjectProperty<>(Rom.FIRE_RED_10);
    public final ObservableList<Preset> presets = FXCollections.observableArrayList();
    public final ObservableSet<String> selected = FXCollections.observableSet("seed", "repel", "party-iv", "secret-id");
    public final ObjectProperty<Composition> composition = new SimpleObjectProperty<>();
    public final ObjectProperty<File> wc3File = new SimpleObjectProperty<>();

    public PresetBuilderViewModel(ToolkitService service) {
        this.service = service;
        rom.addListener((obs, previous, current) -> reload());
        selected.addListener((SetChangeListener<String>) change -> refresh());
        reload();
    }
    private void reload() {
        presets.setAll(service.listPresets(rom.get()));
        refresh();
    }
    private void refresh() {
        composition.set(service.planComposition(rom.get(), Set.copyOf(selected)));
    }
}
