package com.choppy.desktop.view;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/** Application navigation. */
public final class ToolkitView extends BorderPane {
    public ToolkitView(PresetBuilderView builder) {
        this(null,builder);
    }

    public ToolkitView(InjectorView injector, PresetBuilderView builder) {
        // Keep the application branding outside the individual tool tabs.
        Node header = builder.getTop();
        builder.setTop(null);
        setTop(header);
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("tool-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Transport", injector == null ? blank() : injector),
            new Tab("Builder", blank()),
            new Tab("Preset Composition", builder));
        tabs.getSelectionModel().select(injector == null ? 2 : 0);
        setCenter(tabs);
    }

    private static Node blank() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("mock-tool");
        return pane;
    }
}
