package com.choppy.desktop.model;

public record Preset(String id, String name, String hotkey, String validationStatus,
                     String description, java.util.List<ToolkitData.Parameter> parameters) {
    public Preset(String id, String name, String hotkey, String validationStatus) {
        this(id, name, hotkey, validationStatus, "", java.util.List.of());
    }
}
