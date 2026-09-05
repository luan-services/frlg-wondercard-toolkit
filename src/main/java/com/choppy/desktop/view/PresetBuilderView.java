package com.choppy.desktop.view;

import com.choppy.desktop.controller.PresetBuilderViewModel;
import com.choppy.desktop.model.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

public final class PresetBuilderView extends BorderPane {
    private final PresetBuilderViewModel vm;
    private final VBox rows = new VBox();
    private final VBox composition = new VBox(16);
    private final Label count = new Label();

    public PresetBuilderView(PresetBuilderViewModel vm) {
        this.vm = vm;
        getStyleClass().add("app");
        createHeader();

        VBox body = new VBox(20);
        body.getStyleClass().add("body");
        body.setMinWidth(780);
        HBox heading = createHeading();

        VBox presetCard = createPresetCard();

        composition.getStyleClass().addAll("card", "composition");
        vm.composition.addListener((obs, old, value) -> renderComposition(value));
        renderComposition(vm.composition.get());



        body.getChildren().addAll(heading, presetCard, composition);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);
        Label footer = styled("POKÉMON FIRERED / LEAFGREEN WONDERCARD TOOLKIT . Local workspace . v0.1.0", "footer");
        setBottom(footer);
    }

    // Header and tool introduction
    private void createHeader() {
        Label brand = styled("FRLG Wondercard Toolkit", "brand");
        Label demo = styled("VISUAL MVP  /  MOCK DATA", "badge");
        HBox top = new HBox(16, brand, spacer(), demo);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("topbar");
        setTop(top);

    }

    private HBox createHeading() {
        Label title = styled("Preset Composition", "title");
        Label subtitle = styled("Add custom features to your game binded to key combinations.", "muted");
        subtitle.setWrapText(true);
        RomSelector rom = new RomSelector(vm.rom);
        VBox romBox = new VBox(7, styled("TARGET ROM", "eyebrow"), rom);
        HBox heading = new HBox(20, new VBox(6, title, subtitle), spacer(), createTargetInput(), romBox);
        heading.setAlignment(Pos.CENTER_LEFT);

        return heading;
    }

    // Bounded preset list with a fixed column header
    private VBox createPresetCard() {
        VBox presetCard = new VBox();
        presetCard.getStyleClass().add("card");
        HBox section = new HBox(12, styled("Available presets", "section-title"), spacer(), count);
        section.getStyleClass().add("section-header");
        section.setAlignment(Pos.CENTER_LEFT);
        HBox columns = new HBox(styled("PRESET", "eyebrow"), spacer(), fixed("HOTKEY", 155), fixed("VALIDATION", 150));
        columns.getStyleClass().add("columns");
        ScrollPane presetScroll = new ScrollPane(rows);
        presetScroll.getStyleClass().add("preset-scroll");
        presetScroll.setFitToWidth(true);
        presetScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        presetCard.getChildren().addAll(section, columns, presetScroll);
        vm.presets.addListener((ListChangeListener<Preset>) change -> renderRows());
        vm.selected.addListener((SetChangeListener<String>) change -> updateCount());
        renderRows();

        return presetCard;
    }

    // Compact input selector. The tooltip preserves the full path when truncated.
    private VBox createTargetInput() {
        Button browse = new Button("No .wc3 file selected");
        browse.getStyleClass().add("target-input");
        browse.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("empty"), vm.wc3File.get() == null);
        browse.setPrefWidth(220);
        browse.setMinWidth(220);
        browse.setMaxWidth(220);
        browse.setAlignment(Pos.CENTER_LEFT);
        browse.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);
        Tooltip fullPath = new Tooltip("No .wc3 file selected");
        browse.setTooltip(fullPath);
        vm.wc3File.addListener((obs, old, file) -> {
            String text = file == null ? "No .wc3 file selected" : file.getAbsolutePath();
            browse.setText(text);
            browse.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("empty"), file == null);
            fullPath.setText(text);
        });
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select a base Wonder Card");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wonder Card (*.wc3)", "*.wc3", "*.WC3"));
            File file = chooser.showOpenDialog(getScene().getWindow());
            if (file != null) vm.wc3File.set(file);
        });
        return new VBox(7, styled("TARGET INPUT", "eyebrow"), browse);
    }

    // Visual preview only. Real file generation belongs to the toolkit integration.
    private void showGenerationPreview() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Success!");
        dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dialog.getDialogPane().getStyleClass().add("success-dialog");
        Label icon = styled("✓", "success-icon");
        HBox heading = new HBox(12, icon, styled("Success!", "success-title"));
        heading.setAlignment(Pos.CENTER_LEFT);
        Label message = new Label("Preset composition generated successfuly.");
        message.setWrapText(true);
        VBox files = new VBox(8,
            styled("xxx-install-1", "generated-file"),
            styled("xxx-instal-2", "generated-file"),
            styled("xxx-runtime", "generated-file"));
        files.getStyleClass().add("generated-files");
        VBox content = new VBox(16, heading, message, files,
            styled("Mock preview · No files were generated.", "muted"));
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().add("primary");        dialog.showAndWait();
    }
    private void renderRows() {
        rows.getChildren().clear();
        for (Preset preset : vm.presets) {
            CheckBox check = new CheckBox(preset.name());
            check.setSelected(vm.selected.contains(preset.id()));
            HBox row = new HBox(12, check, spacer(), fixed(preset.hotkey(), 143), fixed(preset.validationStatus(), 150));
            row.getStyleClass().add("preset-row");
            row.setAlignment(Pos.CENTER_LEFT);
            row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), check.isSelected());
            check.selectedProperty().addListener((obs, old, selected) -> {
                if (selected) vm.selected.add(preset.id()); else vm.selected.remove(preset.id());
                row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), selected);
            });
            rows.getChildren().add(row);
        }
        updateCount();
    }
    private void updateCount() { count.setText(vm.selected.size() + " selected / " + vm.presets.size() + " presets"); }
    private void renderComposition(Composition data) {
        Label valid = styled("●  " + data.status(), "valid");
        HBox heading = new HBox(12, styled("Composition", "section-title"), spacer(), styled("Hotkeys: " + data.hotkeys(), "muted"), valid);
        heading.setAlignment(Pos.CENTER_LEFT);
        HBox meters = new HBox(28,
            new CapacityMeter("Runtime", data.runtimeUsed(), data.runtimeCapacity()),
            new CapacityMeter("SB1", data.sb1Used(), data.sb1Capacity()),
            new CapacityMeter("SB2", data.sb2Used(), data.sb2Capacity()));
        HBox.setHgrow(meters, Priority.ALWAYS);
        meters.setMinWidth(0);
        Button generate = new Button("Generate");
        generate.getStyleClass().add("primary");
        generate.setMinWidth(145);
        generate.setOnAction(event -> showGenerationPreview());
        HBox content = new HBox(24, meters, generate);
        content.setAlignment(Pos.CENTER_RIGHT);
        composition.getChildren().setAll(heading, content);
    }
    private static Label styled(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }
    private static Label fixed(String text, double width) {
        Label label = new Label(text);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        return label;
    }
    private static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.ALWAYS);
        return region;
    }
}
