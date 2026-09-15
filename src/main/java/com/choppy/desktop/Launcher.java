package com.choppy.desktop;

import javafx.application.Application;

/** Non-Application entry point for the portable native launcher. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        Application.launch(ToolkitApplication.class, args);
    }
}
