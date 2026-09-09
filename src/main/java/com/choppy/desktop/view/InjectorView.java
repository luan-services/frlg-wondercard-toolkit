package com.choppy.desktop.view;

import com.choppy.desktop.controller.InjectorViewModel;
import com.choppy.desktop.controller.InjectorViewModel.Mode;
import com.choppy.desktop.model.InjectorData.*;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

/** Wonder Card transport selector and save-file inject/extract workflows. */
public final class InjectorView extends BorderPane {
    private final InjectorViewModel vm;
    private final VBox workflow = new VBox(16);
    private final Label operationBadge = label("","status-badge");

    public InjectorView(InjectorViewModel vm) {
        this.vm = vm;
        getStyleClass().addAll("app","injector-view");
        operationBadge.textProperty().bind(vm.operationMessage);
        operationBadge.visibleProperty().bind(vm.operationMessage.isNotEmpty().and(vm.connected));
        operationBadge.managedProperty().bind(operationBadge.visibleProperty());
        VBox body = new VBox(20, heading(), transports(), workflow);
        body.getStyleClass().add("body");
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setCenter(scroll);
        Label footer = label("POKÉMON FIRERED / LEAFGREEN WONDERCARD TOOLKIT . Local workspace . v1.0.0", "footer");
        ProgressBar activity = new ProgressBar();
        activity.setPrefWidth(110); activity.setMaxWidth(110);
        activity.visibleProperty().bind(vm.inspectingSave.or(vm.verifyingWc3).or(vm.transferring));
        HBox footerRow = new HBox(12,footer,spacer(),activity);
        footerRow.setAlignment(Pos.CENTER_LEFT); footerRow.setPadding(new Insets(0,24,0,0));
        setBottom(footerRow);
        vm.mode.addListener((o,a,b) -> refresh());
        vm.saveFile.addListener((o,a,b) -> refresh());
        vm.wc3File.addListener((o,a,b) -> refresh());
        vm.saveInspection.addListener((o,a,b) -> refresh());
        vm.wc3Verification.addListener((o,a,b) -> refresh());
        vm.connected.addListener((o,a,b) -> refresh());
        vm.inspectingSave.addListener((o,a,b) -> refresh());
        vm.verifyingWc3.addListener((o,a,b) -> refresh());
        vm.transferring.addListener((o,a,b) -> refresh());
        vm.connectionMessage.addListener((o,a,b) -> refresh());
        vm.result.addListener((o,a,b) -> { refresh(); if (b != null) showResult(b); });
        refresh();
    }

    private HBox heading() {
        VBox copy = new VBox(6, label("Wonder Card Transporter","title"),
            label("Choose how you want to transfer Wonder Card data.","muted"));
        Button reconnect = new Button("Reconnect"); reconnect.setOnAction(e -> vm.reconnect());
        reconnect.visibleProperty().bind(vm.connected.not()); reconnect.managedProperty().bind(reconnect.visibleProperty());
        Label error = label("", "connection-error"); error.textProperty().bind(vm.connectionMessage);
        error.visibleProperty().bind(vm.connected.not()); error.managedProperty().bind(error.visibleProperty());
        HBox row = new HBox(12,copy,spacer(),error,reconnect);
        row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private VBox transports() {
        ToggleGroup group = new ToggleGroup();
        RadioButton inject = transport("Inject into save file", null, group, Mode.INJECT, false);
        RadioButton extract = transport("Extract Wonder Card from save file", null, group, Mode.EXTRACT, false);
        RadioButton celio = transport("Receive via Celio's GB-Link", null, group, null, true);
        RadioButton rom = transport("Generate distribution ROM", null, group, null, true);
        inject.setSelected(true);
        HBox options = new HBox(10,inject,extract,celio,rom);
        options.getStyleClass().add("transport-options");
        VBox box = new VBox(10,label("Transport","section-title"),options);
        box.getStyleClass().addAll("card","transport-card"); return box;
    }

    private RadioButton transport(String title,String note,ToggleGroup group,Mode mode,boolean disabled) {
        RadioButton button = new RadioButton(note == null ? title : title + "  ·  " + note);
        button.setToggleGroup(group); button.setDisable(disabled);
        if (mode != null) button.setOnAction(e -> vm.mode.set(mode));
        button.getStyleClass().addAll("transport-option","primary-radio"); return button;
    }

    private void refresh() {
        workflow.getChildren().clear();
        workflow.getStyleClass().setAll("card","transport-workflow");
        HBox workflowHeading = new HBox(10,
            label(vm.mode.get() == Mode.INJECT ? "Inject into save file" : "Extract Wonder Card from save file","section-title"),
            spacer(),operationBadge);
        workflowHeading.setAlignment(Pos.CENTER_LEFT);
        workflow.getChildren().add(workflowHeading);
        workflow.getChildren().add(fileRow("Save file",vm.saveFile.get(),"Select .sav", "Game Boy Advance save (*.sav)","*.sav","*.SAV", file -> vm.saveFile.set(file)));
        if (vm.mode.get() == Mode.INJECT)
            workflow.getChildren().add(fileRow("Wonder Card",vm.wc3File.get(),"Select .wc3", "Wonder Card (*.wc3)","*.wc3","*.WC3", file -> vm.wc3File.set(file)));
        renderPreviews();
        Button action = new Button(vm.transferring.get() ? "Working…" : vm.mode.get() == Mode.INJECT ? "Inject Wonder Card" : "Extract Wonder Card");
        action.getStyleClass().add("primary"); action.setDisable(!vm.canTransfer()); action.setOnAction(e -> chooseOutput());
        workflow.getChildren().add(action);
    }

    private void renderPreviews() {
        HBox previews = new HBox(14);
        previews.getStyleClass().add("preview-row");
        SaveInspection save = vm.saveInspection.get();
        if (save != null) {
            VBox details = new VBox(5,label("Save preview","eyebrow"));
            for (Slot slot : save.slots()) details.getChildren().add(wrapped("Slot " + slot.index() + ": " + slot.status()
                + (slot.counter() == null ? "" : " · counter " + slot.counter())));
            details.getChildren().add(wrapped(save.activeSlot() == null ? "No valid active slot" : "Active slot: " + save.activeSlot().index()));
            details.getStyleClass().add("preview-panel");
            HBox.setHgrow(details,Priority.ALWAYS); details.setMaxWidth(Double.MAX_VALUE);
            previews.getChildren().add(details);
        }
        Wc3Verification wc3 = vm.mode.get() == Mode.INJECT ? vm.wc3Verification.get() : null;
        if (wc3 != null) {
            VBox details = new VBox(5,label("Wonder Card preview","eyebrow"),
                wrapped("Flag ID " + wc3.flagId() + " · icon species " + wc3.iconSpecies()),
                wrapped("Card CRC: " + validity(wc3.cardCrcValid()) + " · RamScript checksum: " + validity(wc3.ramScriptChecksumValid())));
            wc3.warnings().forEach(w -> details.getChildren().add(warning(w)));
            details.getStyleClass().add("preview-panel");
            HBox.setHgrow(details,Priority.ALWAYS); details.setMaxWidth(Double.MAX_VALUE);
            previews.getChildren().add(details);
        }
        if (!previews.getChildren().isEmpty()) workflow.getChildren().add(previews);
    }

    private HBox fileRow(String title,File current,String buttonText,String description,String ext1,String ext2,java.util.function.Consumer<File> selected) {
        Label path = wrapped(current == null ? "No file selected" : current.getAbsolutePath()); path.getStyleClass().add("file-path");
        Button choose = new Button(buttonText); choose.setDisable(vm.transferring.get()); choose.setOnAction(e -> {
            FileChooser picker = new FileChooser(); picker.setTitle(title); picker.getExtensionFilters().add(new FileChooser.ExtensionFilter(description,ext1,ext2));
            File file = picker.showOpenDialog(getScene().getWindow()); if (file != null) selected.accept(file);
        });
        VBox copy = new VBox(5,label(title.toUpperCase(),"eyebrow"),path);
        HBox row = new HBox(14,copy,spacer(),choose); row.setAlignment(Pos.CENTER_LEFT); return row;
    }

    private void chooseOutput() {
        FileChooser picker = new FileChooser();
        boolean inject = vm.mode.get() == Mode.INJECT;
        picker.setTitle(inject ? "Save injected copy" : "Save extracted Wonder Card");
        picker.setInitialFileName(inject ? "injected.sav" : "extracted.wc3");
        picker.getExtensionFilters().add(new FileChooser.ExtensionFilter(inject ? "Save file (*.sav)" : "Wonder Card (*.wc3)",inject ? "*.sav" : "*.wc3"));
        File output = picker.showSaveDialog(getScene().getWindow());
        if (output == null) return;
        vm.transfer(output.toPath());
    }

    private void showResult(TransferResult result) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(result.injection() ? "Injection complete" : "Extraction complete");
        Label icon = label("✓","success-icon");
        Label title = label(result.injection() ? "Wonder Card injected" : "Wonder Card extracted","success-title");
        VBox heading = new VBox(3,title,label("Operation completed successfully.","muted"));
        HBox hero = new HBox(12,icon,heading); hero.setAlignment(Pos.CENTER_LEFT);
        VBox details = new VBox(9,
            resultRow("OUTPUT",result.output().path().toString()),
            resultRow("SIZE",result.output().size() + " B"),
            resultRow("ACTIVE SLOT",Integer.toString(result.slotIndex())),
            resultRow("SAVE COUNTER",Long.toString(result.saveCounter())),
            resultRow("PHYSICAL SECTOR",Integer.toString(result.physicalSector())));
        details.getStyleClass().add("result-details");
        VBox content = new VBox(16,hero,details);
        if (result.flagId() != null) details.getChildren().add(resultRow("FLAG ID",result.flagId().toString()));
        if (result.sectorChecksum() != null) details.getChildren().add(resultRow("SECTOR CHECKSUM",result.sectorChecksum()));
        if (result.cardCrcValid() != null) {
            details.getChildren().add(resultRow("CARD CRC",validity(result.cardCrcValid())));
            details.getChildren().add(resultRow("RAMSCRIPT",validity(result.ramScriptChecksumValid())));
        }
        result.warnings().forEach(w -> content.getChildren().add(warning(w)));
        ButtonType close = new ButtonType("Close",ButtonBar.ButtonData.OK_DONE);
        configureDialog(dialog,content,close);
        dialog.getDialogPane().lookupButton(close).getStyleClass().add("primary");
        dialog.showAndWait();
    }

    private void configureDialog(Dialog<?> dialog,VBox content,ButtonType... buttons) {
        dialog.initOwner(getScene().getWindow());
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().addAll(getScene().getStylesheets());
        pane.getStyleClass().addAll("toolkit-dialog","transport-dialog");
        content.getStyleClass().add("dialog-card");
        VBox inset = new VBox(content); inset.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(inset);
        scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportWidth(520); scroll.setMaxHeight(560);
        pane.setContent(scroll); pane.getButtonTypes().setAll(buttons);
        pane.setPrefWidth(568);
    }

    private static HBox resultRow(String name,String value) {
        Label key = label(name,"eyebrow"); key.setMinWidth(130);
        Label text = wrapped(value); text.getStyleClass().add("result-value");
        HBox row = new HBox(12,key,text); row.setAlignment(Pos.TOP_LEFT); return row;
    }

    private static String validity(boolean value) { return value ? "valid" : "warning"; }
    private static Label warning(Warning warning) { Label label = wrapped(warning.code() + ": " + warning.message()); label.getStyleClass().add("warning-text"); return label; }
    private static Label label(String text,String style) { Label label = new Label(text); label.getStyleClass().add(style); return label; }
    private static Label wrapped(String text) { Label label = new Label(text); label.setWrapText(true); return label; }
    private static Region spacer() { Region region = new Region(); HBox.setHgrow(region,Priority.ALWAYS); return region; }
}
