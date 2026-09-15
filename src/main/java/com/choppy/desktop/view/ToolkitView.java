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
        this(injector, builder, null);
    }

    public ToolkitView(InjectorView injector, PresetBuilderView builder, BuilderView cardBuilder) {
        // Keep the application branding outside the individual tool tabs.
        Node header = builder.getTop();
        builder.setTop(null);
        javafx.scene.control.Button theme = new javafx.scene.control.Button();
        theme.getStyleClass().add("theme-toggle");
        Runnable updateThemeHint = () -> {
            String hint = "Switch to " + (com.choppy.desktop.AppTheme.isLight() ? "dark" : "light") + " mode";
            theme.setGraphic(themeIcon(com.choppy.desktop.AppTheme.isLight()));
            theme.setAccessibleText(hint);
            theme.setTooltip(new javafx.scene.control.Tooltip(hint));
        };
        updateThemeHint.run();
        theme.setOnAction(event -> { com.choppy.desktop.AppTheme.toggle(); updateThemeHint.run(); });
        if (header instanceof javafx.scene.layout.HBox bar) bar.getChildren().add(theme);
        setTop(header);
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("tool-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            new Tab("Transport", injector == null ? blank() : injector),
            new Tab("Builder", cardBuilder == null ? blank() : cardBuilder),
            new Tab("Preset Composition", builder));
        tabs.getSelectionModel().select(injector == null ? 2 : 0);
        setCenter(tabs);
    }

    /** Lucide moon/sun paths; license bundled in resources/icons/LUCIDE-LICENSE.txt. */
    private static Node themeIcon(boolean moon) {
        javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
        path.setContent(moon
            ? "M20.985 12.486a9 9 0 1 1-9.473-9.472c.405-.022.617.46.402.803a6 6 0 0 0 8.268 8.268c.344-.215.825-.004.803.401"
            : "M16 12a4 4 0 1 0-8 0a4 4 0 1 0 8 0 M12 2v2 M12 20v2 M4.93 4.93l1.41 1.41 M17.66 17.66l1.41 1.41 M2 12h2 M20 12h2 M6.34 17.66l-1.41 1.41 M19.07 4.93l-1.41 1.41");
        path.getStyleClass().add("theme-icon");
        javafx.scene.Group graphic = new javafx.scene.Group(path);
        graphic.getTransforms().add(new javafx.scene.transform.Scale(0.75, 0.75));
        return new javafx.scene.Group(graphic);
    }

    private static Node blank() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("mock-tool");
        return pane;
    }
}
