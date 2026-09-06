package com.choppy.desktop.controller;

import com.choppy.desktop.model.InjectorData.*;
import com.choppy.desktop.service.InjectorService;
import javafx.application.Platform;
import javafx.beans.property.*;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Owns asynchronous state for the save-file transport tab. */
public final class InjectorViewModel implements AutoCloseable {
    public enum Mode { INJECT, EXTRACT }
    private final InjectorService service;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r,"wc3-injector-worker"); thread.setDaemon(true); return thread;
    });
    private long saveRevision;
    private long wc3Revision;
    private boolean closed;

    public final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.INJECT);
    public final ObjectProperty<File> saveFile = new SimpleObjectProperty<>();
    public final ObjectProperty<File> wc3File = new SimpleObjectProperty<>();
    public final ObjectProperty<SaveInspection> saveInspection = new SimpleObjectProperty<>();
    public final ObjectProperty<Wc3Verification> wc3Verification = new SimpleObjectProperty<>();
    public final ObjectProperty<TransferResult> result = new SimpleObjectProperty<>();
    public final BooleanProperty connected = new SimpleBooleanProperty(false);
    public final BooleanProperty inspectingSave = new SimpleBooleanProperty(false);
    public final BooleanProperty verifyingWc3 = new SimpleBooleanProperty(false);
    public final BooleanProperty transferring = new SimpleBooleanProperty(false);
    public final StringProperty connectionMessage = new SimpleStringProperty("Connecting to injector…");
    public final StringProperty operationMessage = new SimpleStringProperty("");

    public InjectorViewModel(InjectorService service) {
        this.service = service;
        saveFile.addListener((o,a,b) -> inspect(b));
        wc3File.addListener((o,a,b) -> verify(b));
        mode.addListener((o,a,b) -> { result.set(null); operationMessage.set(""); });
        reconnect();
    }

    public void reconnect() {
        connected.set(false); connectionMessage.set("Connecting to injector…");
        worker.submit(() -> {
            try {
                service.verifyVersion();
                Platform.runLater(() -> { if (!closed) { connected.set(true); connectionMessage.set("Injector API v1 ready"); } });
            } catch (Exception error) { Platform.runLater(() -> { if (!closed) connectionMessage.set(message(error)); }); }
        });
    }

    private void inspect(File file) {
        long revision = ++saveRevision;
        saveInspection.set(null);
        if (file == null) { inspectingSave.set(false); return; }
        inspectingSave.set(true); operationMessage.set("Inspecting save…");
        worker.submit(() -> {
            try {
                SaveInspection inspection = service.inspectSave(file.toPath());
                Platform.runLater(() -> { if (!closed && revision == saveRevision) {
                    saveInspection.set(inspection); inspectingSave.set(false); operationMessage.set("Save inspected");
                }});
            } catch (Exception error) { previewFailure(revision,true,error); }
        });
    }

    private void verify(File file) {
        long revision = ++wc3Revision;
        wc3Verification.set(null);
        if (file == null) { verifyingWc3.set(false); return; }
        verifyingWc3.set(true); operationMessage.set("Verifying Wonder Card…");
        worker.submit(() -> {
            try {
                Wc3Verification verification = service.verifyWonderCard(file.toPath());
                Platform.runLater(() -> { if (!closed && revision == wc3Revision) {
                    wc3Verification.set(verification); verifyingWc3.set(false); operationMessage.set("Wonder Card verified");
                }});
            } catch (Exception error) { previewFailure(revision,false,error); }
        });
    }

    private void previewFailure(long revision, boolean save, Exception error) {
        Platform.runLater(() -> {
            if (closed || revision != (save ? saveRevision : wc3Revision)) return;
            if (save) inspectingSave.set(false); else verifyingWc3.set(false);
            operationMessage.set(message(error));
        });
    }

    public boolean canTransfer() {
        if (!connected.get() || transferring.get() || inspectingSave.get() || saveInspection.get() == null
                || saveInspection.get().activeSlot() == null) return false;
        return mode.get() == Mode.EXTRACT || (!verifyingWc3.get() && wc3Verification.get() != null);
    }

    public void transfer(Path output) {
        if (!canTransfer()) return;
        Path save = saveFile.get().toPath();
        Path wc3 = wc3File.get() == null ? null : wc3File.get().toPath();
        Mode requestedMode = mode.get();
        transferring.set(true); result.set(null);
        operationMessage.set(requestedMode == Mode.INJECT ? "Injecting Wonder Card…" : "Extracting Wonder Card…");
        worker.submit(() -> {
            try {
                TransferResult completed = requestedMode == Mode.INJECT
                    ? service.inject(save,wc3,output) : service.extract(save,output);
                Platform.runLater(() -> { if (!closed) {
                    transferring.set(false); result.set(completed);
                    operationMessage.set(completed.injection() ? "Wonder Card injected successfully" : "Wonder Card extracted successfully");
                }});
            } catch (Exception error) { Platform.runLater(() -> { if (!closed) {
                transferring.set(false); operationMessage.set(message(error));
            }}); }
        });
    }

    private static String message(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank() ? error.getClass().getSimpleName() : error.getMessage();
    }
    @Override public void close() { closed = true; worker.shutdownNow(); }
}
