package com.choppy.desktop;

import com.choppy.desktop.controller.PresetBuilderViewModel;
import com.choppy.desktop.service.MockToolkitService;
import com.choppy.desktop.view.PresetBuilderView;
import com.choppy.desktop.view.ToolkitView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;

public final class ToolkitApplication extends Application {
    @Override public void start(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(1080, bounds.getWidth() * 0.92);
        double height = Math.min(820, bounds.getHeight() * 0.90);
        Scene scene = new Scene(new ToolkitView(new PresetBuilderView(new PresetBuilderViewModel(new MockToolkitService()))));
        scene.getStylesheets().add(ToolkitApplication.class.getResource("/styles/app.css").toExternalForm());
        stage.setTitle("Choppy's FRLG Wondercard Toolkit");
        stage.setMinWidth(Math.min(700, width));
        stage.setMinHeight(Math.min(480, height));
        stage.setScene(scene);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(bounds.getMinX() + (bounds.getWidth() - width) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - height) / 2);
        stage.show();
        System.out.println("Preset Builder started successfully.");
    }
    public static void main(String[] args) { launch(args); }
}
