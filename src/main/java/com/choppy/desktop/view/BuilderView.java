package com.choppy.desktop.view;

import com.choppy.desktop.controller.BuilderViewModel;
import com.choppy.desktop.model.BuilderData.*;
import com.choppy.desktop.view.wondercard.WonderCardPreview;
import com.choppy.desktop.view.wondercard.WonderCardTextLayout;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

/** Wonder Card editor, using the shared desktop visual vocabulary. */
public final class BuilderView extends BorderPane {
    private final BuilderViewModel vm;
    private final VBox form = new VBox(12);
    private Path lastBuildDestination;
    private final WonderCardTextLayout textLayout=new WonderCardTextLayout();
    private final BooleanProperty invalid=new SimpleBooleanProperty();
    private final Map<String,Control> controls=new LinkedHashMap<>();
    private final Map<String,Label> hints=new LinkedHashMap<>();
    public BuilderView(BuilderViewModel vm) {
        this.vm=vm;
        getStyleClass().addAll("app","builder-view");
        Label title = label("Wonder Card Builder","title");
        Label subtitle = label("Create a card or edit an existing Wonder Card.","muted");
        Button retry = new Button("Reconnect"); retry.setOnAction(e -> vm.reconnect());
        retry.visibleProperty().bind(vm.ready.not()); retry.managedProperty().bind(retry.visibleProperty());
        retry.disableProperty().bind(vm.busy);
        HBox heading = new HBox(12,new VBox(5,title,subtitle),spacer(),retry);
        heading.setAlignment(Pos.CENTER_LEFT);
        Button fresh = button("New", () -> { if (confirmDiscard()) vm.newCard(); });
        Button open = button("Open",this::open);
        Button build = button("Build",this::build); build.getStyleClass().add("primary");
        build.disableProperty().bind(vm.busy.or(vm.ready.not()).or(invalid));
        Label file = label("","muted");
        file.textProperty().bind(Bindings.createStringBinding(() ->
            (vm.source.get() == null ? "Untitled.wc3" : vm.source.get().getFileName().toString())
                + (vm.dirty.get() ? " · Unsaved changes" : ""),vm.source,vm.dirty));
        file.setTooltip(new Tooltip());
        file.setMinWidth(0);
        file.getTooltip().textProperty().bind(Bindings.createStringBinding(() -> vm.source.get() == null ? "New Wonder Card" : vm.source.get().toString(),vm.source));
        HBox actions = new HBox(8,fresh,open,build); actions.setAlignment(Pos.CENTER_LEFT);
        for (Button action : List.of(fresh,open,build)) action.setMinWidth(Region.USE_PREF_SIZE);
        VBox toolbar = new VBox(6,actions,file);
        toolbar.getStyleClass().addAll("card","builder-toolbar");
        VBox top = new VBox(heading); top.setPadding(new Insets(18,32,16,32)); setTop(top);

        form.getStyleClass().add("builder-fields"); form.setMinWidth(0);
        form.disableProperty().bind(vm.busy.or(vm.ready.not()));
        ScrollPane scroll = new ScrollPane(form); scroll.setFitToWidth(true); scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("card"); scroll.setMinWidth(0);
        WonderCardPreview preview = new WonderCardPreview(vm);
        ScrollPane previewScroll = new ScrollPane(preview); previewScroll.setFitToWidth(true); previewScroll.setFitToHeight(true);
        previewScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        previewScroll.setMinWidth(0);
        Label status = label("","muted"); status.setWrapText(true); status.textProperty().bind(vm.status);
        vm.failed.addListener((o,a,b) -> status.getStyleClass().setAll(b ? "connection-error" : "muted"));
        Label checks = label("","muted");
        checks.textProperty().bind(Bindings.createStringBinding(() -> {
            Validation v=vm.validation.get();
            return v == null ? "" : "Last saved file · Card CRC: " + (v.cardCrcValid() ? "valid" : "invalid")
                + " · RamScript: " + (v.ramScriptChecksumValid() ? "valid" : "invalid");
        },vm.validation));
        checks.setWrapText(true);
        VBox messages = new VBox(3,status,checks);
        messages.setAlignment(Pos.CENTER_RIGHT); messages.setMinWidth(0);
        status.setAlignment(Pos.CENTER_RIGHT); checks.setAlignment(Pos.CENTER_RIGHT);
        status.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        checks.setTextAlignment(javafx.scene.text.TextAlignment.RIGHT);
        checks.visibleProperty().bind(checks.textProperty().isNotEmpty());
        checks.managedProperty().bind(checks.visibleProperty());
        GridPane editor = new GridPane(); editor.setHgap(16); editor.setVgap(16);
        ColumnConstraints settingsColumn = new ColumnConstraints(); settingsColumn.setPercentWidth(55);
        ColumnConstraints previewColumn = new ColumnConstraints(); previewColumn.setPercentWidth(45);
        editor.getColumnConstraints().addAll(settingsColumn,previewColumn);
        VBox rightColumn=new VBox(14,previewScroll,toolbar,messages);
        rightColumn.setMinWidth(0); rightColumn.setMinHeight(0); VBox.setVgrow(previewScroll,Priority.ALWAYS);
        RowConstraints cardsRow = new RowConstraints(); cardsRow.setVgrow(Priority.ALWAYS); cardsRow.setMinHeight(0);
        editor.getRowConstraints().add(cardsRow);
        editor.add(scroll,0,0); editor.add(rightColumn,1,0);
        setCenter(editor); BorderPane.setMargin(editor,new Insets(0,32,0,32));
        Label footer = label("POKÉMON FIRERED / LEAFGREEN WONDERCARD TOOLKIT . Local workspace . v0.4.0", "footer");
        ProgressBar activity = new ProgressBar();
        activity.setPrefWidth(110); activity.setMaxWidth(110);
        activity.visibleProperty().bind(vm.busy);
        HBox footerRow = new HBox(12,footer,spacer(),activity);
        footerRow.setAlignment(Pos.CENTER_LEFT); footerRow.setPadding(new Insets(0,24,0,0));
        setBottom(footerRow);
        vm.catalog.addListener((o,a,b) -> buildForm(b));
        vm.revision.addListener((o,a,b) -> validateFields());
        vm.result.addListener((o,a,b) -> { if (b!=null) showBuildResult(b); });
        if (vm.catalog.get()!=null) buildForm(vm.catalog.get());
    }
    private Button button(String title,Runnable action) {
        Button b = new Button(title); b.disableProperty().bind(vm.busy.or(vm.ready.not())); b.setOnAction(e -> action.run()); return b;
    }
    private void buildForm(Catalog c) {
        form.getChildren().clear();
        controls.clear(); hints.clear();
        form.getChildren().add(label("Card settings","section-title"));
        form.getChildren().add(pair(select("Receive slot","flag",c.receiveSlots()),input("ID number","id")));
        VBox species = select("Pokémon icon","icon",c.iconSpecies());
        @SuppressWarnings("unchecked") ComboBox<Choice> choices = (ComboBox<Choice>)controls.get("icon");
        TextField search = new TextField(); search.setPromptText("Search Pokémon by name or Pokédex number");
        search.textProperty().addListener((o,a,b) -> {
            Choice selected = choices.getValue();
            String query = b.strip().toLowerCase(Locale.ROOT);
            List<Choice> matches = c.iconSpecies().stream().filter(s -> "SPECIAL".equals(s.kind()) || s.toString().toLowerCase(Locale.ROOT).contains(query)).toList();
            // Keep the selected value in the model even while it is outside the filtered list.
            choices.getItems().setAll(matches); choices.setValue(selected);
        });
        choices.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Choice item,boolean empty) {
                super.updateItem(item,empty);
                setText(empty || item==null ? null : item + ("SPECIAL".equals(item.kind()) ? " — Official default" : ""));
                setStyle(!empty && item != null && "SPECIAL".equals(item.kind()) ? "-fx-font-weight: bold;" : "");
            }
        });
        species.getChildren().add(1,search); form.getChildren().add(species);
        form.getChildren().add(pair(select("Card type","type",c.cardTypes()),select("Background","bg",c.backgrounds())));
        VBox stamps=input("Max stamps","stamps");
        stamps.disableProperty().bind(Bindings.createBooleanBinding(() -> c.cardTypes().stream()
            .noneMatch(t -> t.id().equals("STAMP") && Integer.toString(t.value()).equals(vm.field("type").get())),vm.field("type")));
        form.getChildren().add(pair(select("Send type","send",c.sendTypes()),stamps));
        form.getChildren().add(new Separator());
        form.getChildren().add(label("Card text","section-title"));
        Label guidance=label("Each line must fit the game’s text area and " + c.limits().get("textFieldBytes") + "-byte field. Unsupported input becomes ?.","muted"); guidance.setWrapText(true);
        form.getChildren().add(guidance);
        form.getChildren().addAll(input("Title","title"),input("Subtitle","subtitle"));
        for (int i=1;i<=4;i++) form.getChildren().add(input("Body "+i,"body"+i));
        for (int i=1;i<=2;i++) form.getChildren().add(input("Footer "+i,"footer"+i));
        validateFields();
    }
    private VBox select(String title,String key,List<Choice> values) {
        ComboBox<Choice> combo = new ComboBox<>(); combo.getItems().setAll(values); combo.setMaxWidth(Double.MAX_VALUE);
        combo.setMinWidth(0); combo.setAccessibleText(title); controls.put(key,combo);
        Runnable update = () -> {
            if (vm.field(key).get().isBlank()) return;
            int value=Integer.parseInt(vm.field(key).get());
            Choice selected = values.stream().filter(c -> c.value()==value).findFirst()
                .orElse(new Choice(value,"RAW","Unknown ("+value+")",null,"RAW"));
            if (!combo.getItems().contains(selected)) combo.getItems().add(selected);
            combo.setValue(selected); combo.setTooltip(new Tooltip(selected.label()+"\nInternal value: " + value));
        };
        combo.setOnAction(e -> { if (combo.getValue()!=null) vm.field(key).set(Integer.toString(combo.getValue().value())); });
        vm.field(key).addListener((o,a,b) -> update.run()); update.run();
        Label hint=label("","field-error"); hint.setWrapText(true); hints.put(key,hint);
        hint.setVisible(false); hint.setManaged(false);
        return new VBox(5,label(title.toUpperCase(Locale.ROOT),"eyebrow"),new AnimatedSelector(combo),hint);
    }
    private VBox input(String title,String key) {
        TextField field = new TextField(); field.setAccessibleText(title); controls.put(key,field);
        if (isText(key)) field.setTextFormatter(new TextFormatter<String>(change -> {
            if (!change.isContentChange() || vm.isLoading() || !vm.ready.get()) return change;
            try {
                StringBuilder normalized=new StringBuilder();
                change.getText().codePoints().forEach(cp -> {
                    String ch=new String(Character.toChars(cp)); normalized.append(textLayout.supported(ch) ? ch : "?");
                });
                if (!normalized.toString().equals(change.getText())) {
                    change.setText(normalized.toString());
                    change.setCaretPosition(change.getRangeStart()+normalized.length()); change.setAnchor(change.getCaretPosition());
                }
                String next=change.getControlNewText();
                int limit=vm.catalog.get().limits().get("textFieldBytes");
                // Always allow deletion to repair a pre-existing overlong field.
                if (!textLayout.fits(key,next,limit) && next.length()>=change.getControlText().length()) return null;
            } catch (IllegalStateException missingAssets) { /* Backend remains available without preview resources. */ }
            return change;
        }));
        field.textProperty().bindBidirectional(vm.field(key));
        Label hint=label("","muted"); hint.setWrapText(true); hints.put(key,hint);
        field.setMaxWidth(Double.MAX_VALUE); field.setMinWidth(0);
        return new VBox(5,label(title.toUpperCase(Locale.ROOT),"eyebrow"),field,hint);
    }
    private static boolean isText(String key) { return !List.of("flag","icon","id","type","bg","send","stamps").contains(key); }
    private void validateFields() {
        if (!vm.ready.get() || vm.isLoading() || vm.catalog.get()==null) return;
        boolean any=false;
        for (var entry : controls.entrySet()) {
            String key=entry.getKey(),value=vm.field(key).get(),error="",guidance="";
            var c=vm.catalog.get();
            if (isText(key)) {
                int limit=c.limits().get("textFieldBytes");
                int count=value.codePointCount(0,value.length());
                guidance=count+" / "+limit+" characters";
                if (count>limit) error="This line exceeds the "+limit+"-character limit.";
                try {
                    int width=textLayout.width(value),max=textLayout.maxWidth(key);
                    guidance+=" · "+width+" / "+max+" px";
                    if (!textLayout.supported(value)) error="Contains unsupported characters. Replace them with ?.";
                    if (width>max) error="This line is too wide for the card ("+width+" / "+max+" px).";
                } catch (IllegalStateException missingAssets) { /* Only backend validation is available. */ }
            } else {
                try {
                    long number=Long.parseLong(value);
                    boolean valid=switch (key) {
                        case "id" -> number>=0 && number<=0xffff_ffffL;
                        case "stamps" -> number>=c.limits().get("maxStampsMin") && number<=c.limits().get("maxStampsMax");
                        case "icon" -> number>=c.limits().get("iconSpeciesMin") && number<=c.limits().get("iconSpeciesMax");
                        default -> (switch (key) {
                            case "flag" -> c.receiveSlots(); case "type" -> c.cardTypes();
                            case "bg" -> c.backgrounds(); default -> c.sendTypes();
                        }).stream().anyMatch(choice -> choice.value()==number);
                    };
                    if (!valid) error=key.equals("id") ? "Use a number from 0 to 4294967295." : key.equals("stamps")
                        ? "Use "+c.limits().get("maxStampsMin")+"–"+c.limits().get("maxStampsMax")+" stamps." : "Choose a valid value.";
                } catch (NumberFormatException e) { error="Enter a whole number."; }
            }
            boolean bad=!error.isEmpty(); any|=bad;
            entry.getValue().pseudoClassStateChanged(PseudoClass.getPseudoClass("invalid"),bad);
            Label hint=hints.get(key);
            hint.setText(bad ? error : guidance); hint.getStyleClass().setAll(bad ? "field-error" : "muted");
            hint.setVisible(!hint.getText().isEmpty()); hint.setManaged(hint.isVisible());
        }
        invalid.set(any);
    }
    private void showBuildResult(Saved saved) {
        Dialog<Void> dialog=new Dialog<>(); dialog.initOwner(getScene().getWindow());
        dialog.setTitle("Wonder Card Builder");
        DialogPane pane=dialog.getDialogPane(); pane.getStylesheets().addAll(getScene().getStylesheets());
        pane.getStyleClass().addAll("success-dialog","toolkit-dialog");
        Label path=label(vm.source.get().toString(),"artifact-code-text"); path.setWrapText(true);
        VBox artifact=new VBox(6,label("WONDER CARD · "+vm.catalog.get().limits().get("fileSize")+" B","eyebrow"),path);
        artifact.getStyleClass().add("artifact-code");
        Label summary=label("Card CRC: "+(saved.validation().cardCrcValid() ? "valid" : "invalid")
            +"\nRamScript checksum: "+(saved.validation().ramScriptChecksumValid() ? "valid" : "invalid")
            +"\n"+(saved.ramScriptPreserved() ? "Existing RamScript preserved." : "Informational deliveryman event included."),"muted");
        summary.setWrapText(true);
        VBox content=new VBox(14,label("Wonder Card Builder","success-title"),label("Wonder Card built successfully.","muted"),artifact,
            label(saved.card().title(),"section-title"),summary);
        content.getStyleClass().add("dialog-card");
        VBox inset=new VBox(content); inset.setPadding(new Insets(12));
        ScrollPane body=new ScrollPane(inset); body.setFitToWidth(true); body.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        body.setPrefViewportWidth(480); body.setPrefViewportHeight(340);
        pane.setContent(body); pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.lookupButton(ButtonType.CLOSE).getStyleClass().add("primary");
        dialog.showAndWait();
    }
    private static HBox pair(Node a,Node b) { HBox row=new HBox(12,a,b); HBox.setHgrow(a,Priority.ALWAYS); HBox.setHgrow(b,Priority.ALWAYS); return row; }
    private FileChooser chooser() {
        FileChooser chooser=new FileChooser(); chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Wonder Card (*.wc3)","*.wc3","*.WC3"));
        if (vm.source.get()!=null && java.nio.file.Files.isDirectory(vm.source.get().getParent())) chooser.setInitialDirectory(vm.source.get().getParent().toFile());
        return chooser;
    }
    private void open() {
        if (!confirmDiscard()) return;
        FileChooser chooser=chooser(); chooser.setTitle("Open Wonder Card"); File file=chooser.showOpenDialog(getScene().getWindow());
        if (file!=null) vm.open(file.toPath());
    }
    private void build() {
        Path source=lastBuildDestination == null ? vm.source.get() : lastBuildDestination;
        FileChooser chooser=chooser(); chooser.setTitle("Build Wonder Card");
        if (source!=null && java.nio.file.Files.isDirectory(source.toAbsolutePath().getParent()))
            chooser.setInitialDirectory(source.toAbsolutePath().getParent().toFile());
        chooser.setInitialFileName(source==null ? "custom.wc3" : source.getFileName().toString());
        File file=chooser.showSaveDialog(getScene().getWindow()); if (file==null) return;
        Path output=file.toPath();
        if (!output.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".wc3")) {
            output=output.resolveSibling(output.getFileName()+".wc3");
            if (java.nio.file.Files.exists(output) && !confirm("Replace Wonder Card?","Replace " + output.getFileName() + "?")) return;
        }
        lastBuildDestination=output.toAbsolutePath();
        vm.save(output);
    }
    public boolean confirmDiscard() {
        if (vm.busy.get()) return confirm("Builder is working","Close while the Builder is working? The current operation may not finish.");
        return !vm.dirty.get() || confirm("Unsaved Wonder Card","Discard the unsaved changes to this Wonder Card?");
    }
    private boolean confirm(String title,String message) {
        Alert alert=new Alert(Alert.AlertType.CONFIRMATION,message,ButtonType.CANCEL,ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(null); alert.initOwner(getScene().getWindow());
        alert.getDialogPane().getStylesheets().addAll(getScene().getStylesheets());
        return alert.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;
    }
    private static Label label(String text,String style) { Label l=new Label(text); l.getStyleClass().add(style); return l; }
    private static Region spacer() { Region r=new Region(); HBox.setHgrow(r,Priority.ALWAYS); return r; }
}
