package com.choppy.desktop.view;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** The same compact selector treatment used by Preset Composition's ROM picker. */
public final class AnimatedSelector extends StackPane {
    public AnimatedSelector(ComboBox<?> select) {
        getStyleClass().add("rom-selector"); setMinWidth(0); setMaxWidth(Double.MAX_VALUE);
        SVGPath chevron=new SVGPath(); chevron.setContent("M6 9 L12 15 L18 9"); chevron.getStyleClass().add("rom-chevron");
        StackPane icon=new StackPane(chevron); icon.setMaxWidth(32); icon.setMouseTransparent(true);
        StackPane.setAlignment(icon,Pos.CENTER_RIGHT);
        RotateTransition rotation=new RotateTransition(Duration.millis(180),chevron);
        rotation.setInterpolator(Interpolator.EASE_BOTH);
        select.showingProperty().addListener((o,a,showing) -> {
            rotation.stop(); rotation.setFromAngle(chevron.getRotate()); rotation.setToAngle(showing ? 180 : 0); rotation.playFromStart();
        });
        getChildren().addAll(select,icon);
    }
}
