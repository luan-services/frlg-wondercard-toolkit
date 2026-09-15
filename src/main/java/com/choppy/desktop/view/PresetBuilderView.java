package com.choppy.desktop.view;

import com.choppy.desktop.controller.PresetBuilderViewModel;
import com.choppy.desktop.model.*;
import com.choppy.desktop.model.ToolkitData.*;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;

public final class PresetBuilderView extends BorderPane {
    private final PresetBuilderViewModel vm;
    private File lastOutputDirectory;
    private final VBox rows = new VBox();
    private final VBox composition = new VBox(16);

    private final Label count = new Label();
    private final VBox parameterFields = new VBox(10);


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
        vm.planning.addListener((o,a,b) -> renderComposition(vm.composition.get()));
        vm.building.addListener((o,a,b) -> renderComposition(vm.composition.get()));
        vm.message.addListener((o,a,b) -> renderComposition(vm.composition.get()));
        vm.wc3File.addListener((o,a,b) -> renderComposition(vm.composition.get()));
        vm.result.addListener((o,a,b) -> { if (b != null) showBuildResult(b); });



        body.getChildren().addAll(heading, presetCard, composition);
        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setCenter(scroll);
        Label footer = styled("POKÉMON FIRERED / LEAFGREEN WONDERCARD TOOLKIT . Local workspace . v1.0.1", "footer");
        ProgressBar activity = new ProgressBar();
        activity.setPrefWidth(110);
        activity.setMaxWidth(110);
        activity.visibleProperty().bind(vm.planning.or(vm.building));
        // Keep the footer footprint fixed even when idle.
        HBox footerRow = new HBox(12, footer, spacer(), activity);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(0, 24, 0, 0));
        setBottom(footerRow);
    }

    // Header and tool introduction
    private void createHeader() {
        Label brand = styled("FRLG Wondercard Toolkit", "brand");
        Label demo = styled("TOOLKIT API v1", "badge");
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
        heading.disableProperty().bind(vm.building);

        return heading;
    }

    // Bounded preset list with a fixed column header
    private VBox createPresetCard() {
        VBox presetCard = new VBox();
        presetCard.setMinHeight(Region.USE_PREF_SIZE);
        presetCard.getStyleClass().add("card");
        HBox section = new HBox(12, styled("Available presets", "section-title"), spacer(), count);
        section.getStyleClass().add("section-header");
        section.setAlignment(Pos.CENTER_LEFT);
        HBox columns = new HBox(styled("PRESET", "eyebrow"), spacer(), fixed("HOTKEY", 155), fixed("VALIDATION", 190));
        columns.getStyleClass().add("columns");
        ScrollPane presetScroll = new ScrollPane(rows);
        presetScroll.getStyleClass().add("preset-scroll");
        presetScroll.setFitToWidth(true);
        presetScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        presetCard.getChildren().addAll(section, columns, presetScroll);
        vm.presets.addListener((ListChangeListener<Preset>) change -> renderRows());
        vm.selected.addListener((SetChangeListener<String>) change -> renderRows());
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

    private void chooseOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose output prefix — files will be placed in a new subfolder");
        chooser.setInitialFileName("composition.wc3");
        if (lastOutputDirectory != null && lastOutputDirectory.isDirectory()) chooser.setInitialDirectory(lastOutputDirectory);
        File output = chooser.showSaveDialog(getScene().getWindow());
        if (output != null) { lastOutputDirectory = output.getAbsoluteFile().getParentFile(); vm.build(output.toPath()); }
    }

    private void showBuildResult(BuildResult result) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Preset Composition");
        dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        dialog.getDialogPane().getStyleClass().add("success-dialog");
        VBox content = new VBox(12, styled("Preset Composition", "success-title"),
            wrapped(result.success() ? "Wonder card generated successfully!" : "Wonder card generation failed."));
        for (Artifact artifact : result.artifacts()) {
            Label file = new Label(artifact.order() + ". " + artifact.role() + " · " + artifact.size() + " B\n" + artifact.path());
            file.setWrapText(true);
            file.getStyleClass().add("artifact-code-text");
            VBox code = new VBox(file);
            code.getStyleClass().add("artifact-code");
            content.getChildren().add(code);
        }
        for (String instruction : result.instructions()) content.getChildren().add(wrapped(instruction));
        for (Diagnostic diagnostic : result.diagnostics()) content.getChildren().add(wrapped(diagnostic.display()));
        configureDialog(dialog, content);
        dialog.showAndWait();
    }

    private void renderParameters() {
        parameterFields.getChildren().clear();
        java.util.Set<String> displayed = new java.util.HashSet<>();
        for (Preset preset : vm.presets) if (vm.selected.contains(preset.id())) {
            for (Parameter parameter : preset.parameters()) if (displayed.add(parameter.id())) {
                TextField input = new TextField(vm.parameters.getOrDefault(parameter.id(), ""));
                input.setPromptText("HEX_U32".equals(parameter.type()) ? "00001234" : parameter.example().isEmpty() ? parameter.type() : parameter.example());
                input.setMaxWidth(240);
                input.textProperty().addListener((o,a,b) -> vm.parameters.put(parameter.id(),b));
                input.disableProperty().bind(vm.building);
                parameterFields.getChildren().add(new VBox(6,
                    new Label(parameter.label() + (parameter.required() ? " *" : "")), input));
            }
        }
        if (parameterFields.getChildren().isEmpty()) parameterFields.getChildren().add(styled("None required", "muted"));
        parameterFields.setManaged(true);
        parameterFields.setVisible(parameterFields.isManaged());
    }
    private void renderRows() {
        rows.getChildren().clear();
        for (Preset preset : vm.presets) {
            CheckBox check = new CheckBox(preset.name());
            check.setSelected(vm.selected.contains(preset.id()));
            check.disableProperty().bind(vm.building.or(vm.ready.not()));
            check.setTooltip(new Tooltip(preset.description()));
            HBox row = new HBox(12, check, spacer(), fixed(preset.hotkey(), 143), validationLabel(preset));
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
        renderParameters();
    }
    private void updateCount() { count.setText(vm.selected.size() + " selected / " + vm.presets.size() + " presets"); }
    private void renderComposition(Plan data) {


        boolean idle = vm.selected.isEmpty();
        Label status = styled(idle ? "Idle..." : vm.message.get(), idle || vm.planning.get() || vm.building.get() ? "muted"
            : data != null ? (data.valid() ? "valid" : "invalid-badge") : "muted");
        status.setWrapText(true);
        Button params = compactButton("Requested params", () -> {
            renderParameters();
            showDetails("Requested parameters", parameterFields);
        });
        var requested = vm.presets.stream().filter(p -> vm.selected.contains(p.id()))
            .flatMap(p -> p.parameters().stream()).toList();
        boolean missing = requested.stream().anyMatch(p -> p.required()
            && vm.parameters.getOrDefault(p.id(), "").isBlank());
        params.getStyleClass().add("params-action");
        params.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("missing"), missing);
        params.setDisable(requested.isEmpty() || vm.building.get() || !vm.ready.get());
        Button bindings = compactButton("Effective bindings", () -> {
            VBox content = new VBox(10);
            if (data == null || data.bindings().isEmpty()) content.getChildren().add(wrapped("Available after a valid plan."));
            else data.bindings().forEach(binding -> content.getChildren().add(wrapped(binding)));
            showDetails("Effective bindings", content);
        });
        Button technical = compactButton("Technical details", () -> {
            VBox content = new VBox(10);
            if (data == null || data.diagnostics().isEmpty()) content.getChildren().add(wrapped("No technical notes for this composition."));
            else data.diagnostics().forEach(d -> content.getChildren().add(wrapped(d.display())));
            showDetails("Technical details", content);
        });
        technical.getStyleClass().add("params-action");
        HBox heading = new HBox(10, styled("Composition", "section-title"), params, bindings, spacer(), status, technical);
        heading.setAlignment(Pos.CENTER_LEFT);
        composition.getChildren().setAll(heading);
        HBox meters = new HBox(20);
        if (data != null && data.valid()) {
            meters.getChildren().addAll(meter("RamScript",data.ramScript()),meter("SB1",data.sb1()),meter("SB2",data.sb2()));

        } else {
            meters.getChildren().addAll(new CapacityMeter("RamScript"), new CapacityMeter("SB1"), new CapacityMeter("SB2"));
            Tooltip.install(meters, new Tooltip("No valid plan available. Empty bars are placeholders."));
        }
        heading.getChildren().add(4, styled("Hotkeys: " + (data != null && data.valid() ? data.hotkeys() : "—"), "muted"));
        HBox.setHgrow(meters,Priority.ALWAYS);
        Button generate = new Button(vm.building.get() ? "Generating…" : "Generate");
        generate.getStyleClass().add("primary");
        generate.setMinWidth(145);
        generate.setDisable(vm.planning.get() || vm.building.get() || !vm.ready.get() || data == null || !data.valid() || vm.wc3File.get() == null);
        generate.setOnAction(e -> chooseOutput());
        HBox content = new HBox(24,meters,generate);
        content.setAlignment(Pos.CENTER_RIGHT);
        composition.getChildren().add(content);
        if (!vm.planning.get() && data == null) {
            Button retry = new Button("Reconnect toolkit");
            retry.setOnAction(e -> vm.reload());
            composition.getChildren().add(retry);
        }


    }
    private Button compactButton(String title, Runnable action) {
        Button button = new Button(title);
        button.getStyleClass().add("composition-action");
        button.setOnAction(event -> action.run());
        return button;
    }

    private void showDetails(String title, VBox content) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(getScene().getWindow());
        dialog.setTitle(title);
        dialog.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        ScrollPane scroll = configureDialog(dialog, content);
        dialog.showAndWait();
        ((VBox) scroll.getContent()).getChildren().clear();
        scroll.setContent(null);
    }

    private ScrollPane configureDialog(Dialog<Void> dialog, VBox content) {
        dialog.getDialogPane().getStyleClass().add("toolkit-dialog");
        if (!content.getStyleClass().contains("dialog-card")) content.getStyleClass().add("dialog-card");
        VBox inset = new VBox(content);
        inset.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(inset);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dialog.getDialogPane().setContent(scroll);
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(ok);
        dialog.getDialogPane().lookupButton(ok).getStyleClass().add("primary");
        dialog.setOnShowing(event -> {
            var owner = getScene().getWindow();
            var screens = javafx.stage.Screen.getScreensForRectangle(owner.getX(), owner.getY(), owner.getWidth(), owner.getHeight());
            var bounds = (screens.isEmpty() ? javafx.stage.Screen.getPrimary() : screens.getFirst()).getVisualBounds();
            var pane = dialog.getDialogPane();
            pane.applyCss();
            // Measure wrapped text at the selected width, allowing for window chrome and buttons.
            double width = Math.min(Math.max(460, inset.prefWidth(-1)), Math.min(760, bounds.getWidth() - 80));
            double height = Math.ceil(inset.prefHeight(width - 20)) + 8;
            scroll.setPrefViewportWidth(width);
            scroll.setPrefViewportHeight(Math.min(height, Math.max(120, bounds.getHeight() - 180)));
            pane.setPrefWidth(width + 24);
        });
        return scroll;
    }
    private static CapacityMeter meter(String name, Memory memory) {
        CapacityMeter meter = new CapacityMeter(name,memory.used(),memory.capacity());
        Tooltip.install(meter,new Tooltip(memory.free() + " B free"));
        return meter;
    }
    private static Label validationLabel(Preset preset) {
        Label label = fixed(preset.validationStatus(),190);
        label.setStyle("-fx-font-size: 10px;");
        label.setTooltip(new Tooltip(preset.description()));
        return label;
    }
    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }    private static Label styled(String text, String style) {
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
