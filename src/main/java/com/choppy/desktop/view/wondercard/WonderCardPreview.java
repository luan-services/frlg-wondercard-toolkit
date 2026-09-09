package com.choppy.desktop.view.wondercard;

import com.choppy.desktop.controller.BuilderViewModel;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/** Responsive integer-scaled preview; faults stay inside this panel. */
public final class WonderCardPreview extends VBox {
    private final WonderCardRenderer renderer=new WonderCardRenderer(new WonderCardAssets());
    private final ImageView image=new ImageView();
    private final Label note=new Label("Loading preview…");
    public WonderCardPreview(BuilderViewModel vm) {
        super(14); getStyleClass().addAll("card","builder-preview"); setMinWidth(0); setMinHeight(0);
        Label heading=new Label("Wonder Card preview"); heading.getStyleClass().add("section-title");
        Label resolution=new Label("FR/LG · 240 × 160"); resolution.getStyleClass().add("eyebrow");
        image.setSmooth(false); image.setPreserveRatio(true); image.setFitWidth(240); image.setFitHeight(160);
        StackPane viewport=new StackPane(image); viewport.setMinSize(0,160); viewport.setAlignment(Pos.TOP_CENTER);
        setMinHeight(300);
        VBox.setVgrow(viewport,Priority.ALWAYS);
        note.getStyleClass().add("muted"); note.setWrapText(true);
        getChildren().addAll(heading,resolution,viewport,note);
        Runnable resize=() -> {
            int scale=Math.max(1,Math.min(3,(int)Math.min(viewport.getWidth()/240,viewport.getHeight()/160)));
            image.setFitWidth(240*scale); image.setFitHeight(160*scale);
        };
        viewport.widthProperty().addListener((o,a,b) -> resize.run()); viewport.heightProperty().addListener((o,a,b) -> resize.run());
        Runnable render=() -> {
            if (!vm.ready.get()) return;
            try {
                var card=vm.snapshot();
                image.setImage(renderer.render(card,vm.catalog.get())); image.setVisible(true);
                boolean stats=vm.catalog.get().cardTypes().stream().anyMatch(c -> c.value()==card.type() && c.id().equals("LINK_STAT"));
                note.setText(renderer.hasUnsupportedText() ? "Some characters have no preview glyph and appear as ?. Saving uses the backend's encoding."
                    : stats ? "Static card preview. Player battle/trade statistics are not included in a WC3."
                    : "Live preview · stock graphics and Latin font. Text is clipped to the in-game windows. Player stamps are not included.");
            } catch (RuntimeException e) {
                image.setVisible(false); note.setText(e.getMessage()+". Card editing and saving remain available.");
            }
        };
        vm.revision.addListener((o,a,b) -> render.run()); vm.ready.addListener((o,a,b) -> render.run()); render.run();
    }
}
