package com.choppy.desktop.view;

import javafx.scene.control.*;
import javafx.scene.layout.*;

public final class CapacityMeter extends VBox {
    // Presentation thresholds only; composition validation remains in the service.
    private static final double WARNING_USAGE = 0.70;
    private static final double CRITICAL_USAGE = 0.90;

    public CapacityMeter(String name, int used, int capacity) {
        this(name, used, capacity, false);
    }

    /** Empty visual state, not a toolkit measurement or a claimed capacity. */
    public CapacityMeter(String name) {
        this(name, 0, 0, true);
    }

    private CapacityMeter(String name, int used, int capacity, boolean placeholder) {
        setSpacing(9);
        Label title = new Label(name);
        title.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label value = new Label(placeholder ? "0 / — B" : used + " / " + capacity + " B");
        HBox line = new HBox(title, spacer, value);
        double usage = placeholder ? 0 : capacity > 0 ? (double) used / capacity : 1.0;
        ProgressBar bar = new ProgressBar(Math.clamp(usage, 0.0, 1.0));
        bar.setMaxWidth(Double.MAX_VALUE);
        String capacityStyle = usage >= CRITICAL_USAGE ? "capacity-critical"
            : usage >= WARNING_USAGE ? "capacity-warning" : "capacity-available";
        bar.getStyleClass().add(capacityStyle);
        getChildren().addAll(line, bar);
        setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(this, Priority.ALWAYS);
    }
}
