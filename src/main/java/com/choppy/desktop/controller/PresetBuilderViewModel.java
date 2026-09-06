package com.choppy.desktop.controller;

import com.choppy.desktop.model.*;
import com.choppy.desktop.model.ToolkitData.*;
import com.choppy.desktop.service.ToolkitService;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Owns UI state; background results are applied only to the matching revision. */
public final class PresetBuilderViewModel implements AutoCloseable {
    private final ToolkitService service;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "toolkit-worker"); t.setDaemon(true); return t;
    });
    private final PauseTransition debounce = new PauseTransition(Duration.millis(250));
    private long revision;
    private boolean closed;
    public final ObjectProperty<Rom> rom = new SimpleObjectProperty<>(Rom.FIRE_RED_10);
    public final ObservableList<Preset> presets = FXCollections.observableArrayList();
    public final ObservableSet<String> selected = FXCollections.observableSet();
    public final ObservableMap<String,String> parameters = FXCollections.observableHashMap();
    public final ObjectProperty<Plan> composition = new SimpleObjectProperty<>();
    public final ObjectProperty<File> wc3File = new SimpleObjectProperty<>();
    public final BooleanProperty planning = new SimpleBooleanProperty(true);
    public final BooleanProperty building = new SimpleBooleanProperty(false);
    public final BooleanProperty ready = new SimpleBooleanProperty(false);
    public final StringProperty message = new SimpleStringProperty("Connecting to toolkit…");
    public final ObjectProperty<BuildResult> result = new SimpleObjectProperty<>();

    public PresetBuilderViewModel(ToolkitService service) {
        this.service = service;
        rom.addListener((o,a,b) -> reload());
        selected.addListener((SetChangeListener<String>) c -> schedulePlan());
        parameters.addListener((MapChangeListener<String,String>) c -> schedulePlan());
        debounce.setOnFinished(e -> plan());
        reload();
    }
    public void reload() {
        long version = ++revision;
        Rom target = rom.get();
        ready.set(false); planning.set(true); composition.set(null); message.set("Loading toolkit catalog…");
        worker.submit(() -> {
            try {
                service.verifyVersion();
                List<Preset> catalog = service.listPresets(target);
                Platform.runLater(() -> {
                    if (closed || version != revision) return;
                    presets.setAll(catalog);
                    ready.set(true);
                    schedulePlan();
                });
            } catch (Exception error) { fail(version,error); }
        });
    }
    private void schedulePlan() {
        if (!ready.get() || building.get()) return;
        ++revision;
        planning.set(true); composition.set(null); message.set("Planning…");
        debounce.playFromStart();
    }
    private Request snapshot() {
        Map<String,String> active = new LinkedHashMap<>();
        for (Preset preset : presets) if (selected.contains(preset.id()))
            for (Parameter p : preset.parameters()) {
                String value = parameters.get(p.id());
                if (value != null && !value.isBlank()) active.put(p.id(),value.trim());
            }
        return new Request(rom.get(), selected.stream().sorted().toList(), active);
    }
    private void plan() {
        long version = revision;
        Request request = snapshot();
        worker.submit(() -> {
            try {
                Plan plan = service.plan(request);
                Platform.runLater(() -> {
                    if (closed || version != revision) return;
                    composition.set(plan); planning.set(false);
                    message.set(plan.valid() ? "Valid composition" : "Invalid composition");
                });
            } catch (Exception error) { fail(version,error); }
        });
    }
    public void build(Path chosenOutput) {
        if (!ready.get() || planning.get() || building.get() || composition.get() == null
                || !composition.get().valid() || wc3File.get() == null) return;
        Request request = snapshot();
        Path input = wc3File.get().toPath();
        boolean local = composition.get().localOnly();
        building.set(true); message.set("Generating Wonder Cards…"); result.set(null);
        worker.submit(() -> {
            try {
                // Isolate each build so existing files, including the base, cannot be overwritten.
                Path parent = chosenOutput.toAbsolutePath().getParent();
                String prefix = chosenOutput.getFileName().toString().replaceFirst("(?i)\\.wc3$", "");
                Path folder = Files.createTempDirectory(parent, prefix + "-");
                BuildResult built = service.build(request,input,folder.resolve(prefix + (local ? ".wc3" : "")));
                Platform.runLater(() -> {
                    if (closed) return;
                    building.set(false); result.set(built);
                    message.set(built.success() ? "Generation complete" : "Generation failed");
                });
            } catch (Exception error) {
                Platform.runLater(() -> { if (!closed) { building.set(false); message.set(error.getMessage()); } });
            }
        });
    }
    private void fail(long version, Exception error) {
        Platform.runLater(() -> {
            if (closed || version != revision) return;
            planning.set(false); composition.set(null); message.set(error.getMessage());
        });
    }
    @Override public void close() { closed = true; debounce.stop(); worker.shutdownNow(); }
}
