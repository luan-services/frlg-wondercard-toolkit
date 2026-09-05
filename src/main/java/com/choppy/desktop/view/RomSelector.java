package com.choppy.desktop.view;

import com.choppy.desktop.model.Rom;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/** Native keyboard/popup behavior with the supplied outline chevron. */
public final class RomSelector extends StackPane {
    private static final double ANIMATION_MILLIS = 180;

    public RomSelector(ObjectProperty<Rom> selectedRom) {
        getStyleClass().add("rom-selector");
        setPrefWidth(195);

        ComboBox<Rom> select = new ComboBox<>();
        select.getItems().setAll(Rom.values());
        select.valueProperty().bindBidirectional(selectedRom);
        select.setMaxWidth(Double.MAX_VALUE);
        select.setAccessibleText("Target ROM");

        // ChevronDownIcon.tsx: polyline points="6 9 12 15 18 9".
        SVGPath chevron = new SVGPath();
        chevron.setContent("M6 9 L12 15 L18 9");
        chevron.getStyleClass().add("rom-chevron");
        StackPane icon = new StackPane(chevron);
        icon.getStyleClass().add("rom-chevron-container");
        icon.setMaxWidth(32);
        icon.setMouseTransparent(true);
        StackPane.setAlignment(icon, Pos.CENTER_RIGHT);

        RotateTransition rotation = new RotateTransition(Duration.millis(ANIMATION_MILLIS), chevron);
        rotation.setInterpolator(Interpolator.EASE_BOTH);
        select.showingProperty().addListener((observable, oldValue, showing) -> {
            rotation.stop();
            rotation.setFromAngle(chevron.getRotate());
            rotation.setToAngle(showing ? 180 : 0);
            rotation.playFromStart();
        });

        getChildren().addAll(select, icon);
    }
}
