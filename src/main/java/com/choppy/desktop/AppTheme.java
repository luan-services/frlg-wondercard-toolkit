package com.choppy.desktop;

import java.util.prefs.Preferences;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;

/** Shared theme for the main window, dialogs and popup controls. */
public final class AppTheme {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AppTheme.class);
    private static boolean light = PREFS.getBoolean("lightTheme", false);
    private AppTheme() {}

    public static void initialize(Scene scene) {
        apply(scene);
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                for (Window window : change.getAddedSubList()) apply(window.getScene());
            }
        });
    }

    public static boolean isLight() { return light; }

    public static void toggle() {
        light = !light;
        PREFS.putBoolean("lightTheme", light);
        for (Window window : Window.getWindows()) apply(window.getScene());
    }

    private static void apply(Scene scene) {
        if (scene == null) return;
        String css = AppTheme.class.getResource("/styles/app.css").toExternalForm();
        if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
        scene.getRoot().getStyleClass().remove("light-theme");
        if (light) scene.getRoot().getStyleClass().add("light-theme");
    }
}
