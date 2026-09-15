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
    private static final ButtonType CLOSE = new ButtonType("Close",ButtonBar.ButtonData.CANCEL_CLOSE);
    public javafx.beans.binding.BooleanExpression activityProperty() { return vm.inspectingSave.or(vm.verifyingWc3).or(vm.transferring); }
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
        vm.baseRom.addListener((o,a,b) -> refresh());
        vm.distributionOutput.addListener((o,a,b) -> refresh());
        vm.distributionResult.addListener((o,a,b) -> { if (b != null) showDistributionResult(b); });
        vm.distributionError.addListener((o,a,b) -> { if (b != null) showDistributionError(b); });
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
        RadioButton rom = transport("Generate distribution ROM", null, group, Mode.DISTRIBUTION, false);
        inject.setSelected(true);
        HBox options = new HBox(10,inject,extract,celio,rom);
        options.getStyleClass().add("transport-options");
        Button help = new Button("Help");
        help.getStyleClass().add("transport-help");
        help.setAccessibleText("Help with Wonder Card transport methods");
        help.setOnAction(e -> showTransportHelp());
        HBox title = new HBox(12,label("Transport","section-title"),spacer(),help);
        title.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(10,title,options);
        box.getStyleClass().addAll("card","transport-card"); return box;
    }

    private void showTransportHelp() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Wonder Card Transport Help");
        VBox content = new VBox(16,
            label("Wonder Card Transport","success-title"),
            helpSection("Injection",
                "Adds a Wonder Card to a save file. First, extract the save from your cartridge using compatible cartridge backup hardware, or a Nintendo DS / DS Lite with an R4-compatible flashcart and suitable GBA save-backup homebrew. Guides for these methods are available online.",
                "Select the extracted .sav and the .wc3 to inject. The toolkit creates a separate save file, which you then restore to your cartridge using your backup tool. Keep the original save as a backup."),
            helpSection("Extraction",
                "Uses the same cartridge save-backup methods described above. Select an existing .sav containing a Wonder Card to extract it into a .wc3 file.",
                "The toolkit reports card and RamScript checksum warnings, so check the result before reusing the extracted card."),
            helpSection("Generate distribution ROM",
                "Choose this method for the original wireless distribution experience. You need two Game Boy Advance systems, a compatible Wireless Adapter for each system, and a writable GBA cartridge / flashcart for the generated ROM. The receiving system runs FireRed or LeafGreen.",
                "Generation uses the official USA Aurora Ticket distribution ROM dump as its base. We do not provide this ROM; you must supply your own copy.",
                "The generated ROM can send the supplied Wonder Card to western FireRed and LeafGreen releases across their supported languages and revisions. Japanese releases are not supported.",
                "Custom RamScripts may depend on a particular game, revision or language. The sender's broad compatibility does not guarantee that every custom payload will work on every receiving game."));
        configureDialog(dialog,content,CLOSE);
        dialog.getDialogPane().lookupButton(CLOSE).getStyleClass().add("primary");
        dialog.showAndWait();
    }

    private static VBox helpSection(String title, String... paragraphs) {
        VBox section = new VBox(8,label(title,"section-title"));
        for (String paragraph : paragraphs) section.getChildren().add(wrapped(paragraph));
        return section;
    }

    private RadioButton transport(String title,String note,ToggleGroup group,Mode mode,boolean disabled) {
        RadioButton button = new RadioButton(note == null ? title : title + "  ·  " + note);
        button.setToggleGroup(group);
        button.disableProperty().bind(vm.transferring.or(new javafx.beans.property.SimpleBooleanProperty(disabled)));
        if (mode != null) button.setOnAction(e -> vm.mode.set(mode));
        button.getStyleClass().addAll("transport-option","primary-radio"); return button;
    }

    private void refresh() {
        workflow.getChildren().clear();
        workflow.getStyleClass().setAll("card","transport-workflow");
        if (vm.mode.get() == Mode.DISTRIBUTION) { renderDistribution(); return; }
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

    private void renderDistribution() {
        HBox heading = new HBox(10,label("Generate distribution ROM","section-title"),spacer(),operationBadge);
        heading.setAlignment(Pos.CENTER_LEFT);
        workflow.getChildren().addAll(heading,
            fileRow("Wonder Card",vm.wc3File.get(),"Select .wc3","Wonder Card (*.wc3)","*.wc3","*.WC3",vm.wc3File::set),
            fileRow("Aurora Ticket Distribution ROM (USA)",vm.baseRom.get(),"Select .gba","GBA ROM (*.gba)","*.gba","*.GBA",vm.baseRom::set),
            label("Original USA Aurora Ticket distribution ROM required.","muted"));
        File output = vm.distributionOutput.get();
        Button browse = new Button("Save as...");
        browse.setDisable(vm.transferring.get());
        browse.setOnAction(e -> chooseDistributionOutput());
        Label path = wrapped(output == null ? "Select a Wonder Card to suggest a filename" : output.getAbsolutePath());
        path.getStyleClass().add("file-path");
        HBox outputRow = new HBox(14,new VBox(5,label("OUTPUT ROM","eyebrow"),path),spacer(),browse);
        outputRow.setAlignment(Pos.CENTER_LEFT);
        Button generate = new Button(vm.transferring.get() ? "Working..." : "Generate Distribution ROM");
        generate.getStyleClass().add("primary"); generate.setDisable(!vm.canDistribute());
        generate.setOnAction(e -> {
            File target = vm.distributionOutput.get();
            if (target != null && target.exists()) {
                Dialog<ButtonType> confirm = new Dialog<>();
                confirm.setTitle("Replace existing ROM?");
                configureDialog(confirm,new VBox(12,wrapped("Replace this existing output file?"),wrapped(target.getAbsolutePath())),
                    ButtonType.YES,ButtonType.CANCEL);
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.YES) return;
            }
            vm.generateDistribution();
        });
        workflow.getChildren().addAll(outputRow,generate);
    }

    private void chooseDistributionOutput() {
        FileChooser picker = new FileChooser();
        picker.setTitle("Save distribution ROM");
        picker.getExtensionFilters().add(new FileChooser.ExtensionFilter("GBA ROM (*.gba)","*.gba"));
        File current = vm.distributionOutput.get();
        if (current != null) {
            picker.setInitialFileName(current.getName());
            if (current.getAbsoluteFile().getParentFile().isDirectory())
                picker.setInitialDirectory(current.getAbsoluteFile().getParentFile());
        }
        File selected = picker.showSaveDialog(getScene().getWindow());
        if (selected != null) vm.selectDistributionOutput(selected);
    }

    private void showDistributionResult(DistributionResult result) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Wonder Card Distribution");
        VBox details = new VBox(9,resultRow("OUTPUT",result.output().path().toString()),
            resultRow("SIZE",result.output().size() + " B"),resultRow("TARGET",result.target()),
            resultRow("OUTPUT SHA-1",result.outputSha1()));
        details.getStyleClass().add("artifact-code");
        VBox content = new VBox(16,label("Wonder Card Distribution","success-title"),
            label("Distribution ROM generated successfully!","muted"),details);
        result.warnings().forEach(w -> content.getChildren().add(warning(w)));
        configureDialog(dialog,content,CLOSE);
        dialog.showAndWait();
    }

    private void showDistributionError(Exception error) {
        String code = error instanceof com.choppy.desktop.service.Wc3InjectorService.InjectorException api ? api.code() : "";
        String message = switch (code) {
            case "INVALID_BASE_ROM" -> "Invalid base ROM. Please select the original USA Aurora Ticket Distribution ROM.";
            case "INVALID_WC3" -> "Invalid Wonder Card. The selected file is not a valid WC3 file.";
            case "IO_ERROR" -> "Could not create distribution ROM. Check the output location and file permissions.";
            default -> error instanceof IllegalArgumentException ? error.getMessage() : "Distribution ROM generation failed.";
        };
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Wonder Card Distribution");
        TitledPane technical = new TitledPane("Technical details",wrapped(error.getMessage()));
        technical.setExpanded(false); technical.getStyleClass().add("integration-details");
        configureDialog(dialog,new VBox(16,label("Wonder Card Distribution","success-title"),wrapped(message),technical),CLOSE);
        dialog.showAndWait();
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
        dialog.setTitle(result.injection() ? "Wonder Card Injection" : "Wonder Card Extraction");

        Label title = label(result.injection() ? "Wonder Card Injection" : "Wonder Card Extraction","success-title");
        VBox hero = new VBox(12,title,label(result.injection() ? "Wonder card injected successfully!" : "Wonder card extracted successfully!","muted"));
        VBox details = new VBox(9,
            resultRow("OUTPUT",result.output().path().toString()),
            resultRow("SIZE",result.output().size() + " B"),
            resultRow("ACTIVE SLOT",Integer.toString(result.slotIndex())),
            resultRow("SAVE COUNTER",Long.toString(result.saveCounter())),
            resultRow("PHYSICAL SECTOR",Integer.toString(result.physicalSector())));
        details.getStyleClass().add("artifact-code");
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
        pane.setContent(new StackPane(inset)); pane.getButtonTypes().setAll(buttons);
        dialog.setResizable(true);
        dialog.setOnShowing(event -> {
            var owner = getScene().getWindow();
            var screens = javafx.stage.Screen.getScreensForRectangle(owner.getX(),owner.getY(),owner.getWidth(),owner.getHeight());
            var bounds = (screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.getFirst()).getVisualBounds();
            double width = Math.min(720,bounds.getWidth()-80);
            pane.setPrefWidth(width);
            pane.applyCss();
            pane.setPrefHeight(inset.prefHeight(width-24)+80);
        });
    }

    private static HBox resultRow(String name,String value) {
        Label key = label(name,"eyebrow"); key.setMinWidth(130);
        Label text = wrapped(value); text.getStyleClass().add("artifact-code-text");
        text.setMinWidth(0); text.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(text,Priority.ALWAYS);
        HBox row = new HBox(12,key,text); row.setAlignment(Pos.TOP_LEFT); return row;
    }

    private static String validity(boolean value) { return value ? "valid" : "warning"; }
    private static Label warning(Warning warning) { Label label = wrapped(warning.code() + ": " + warning.message()); label.getStyleClass().add("warning-text"); return label; }
    private static Label label(String text,String style) { Label label = new Label(text); label.getStyleClass().add(style); return label; }
    private static Label wrapped(String text) { Label label = new Label(text); label.setWrapText(true); return label; }
    private static Region spacer() { Region region = new Region(); HBox.setHgrow(region,Priority.ALWAYS); return region; }
}
