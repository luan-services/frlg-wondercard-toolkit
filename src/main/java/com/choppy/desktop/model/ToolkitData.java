package com.choppy.desktop.model;

import java.util.List;
import java.util.Map;

/** Transport-independent data consumed by the desktop UI. */
public final class ToolkitData {
    private ToolkitData() {}
    public record Parameter(String id, String type, boolean required, String label, String example) {}
    public record Diagnostic(String code, String severity, String message, List<String> affectedPresetIds, String detail) {
        public String display() { return code + ": " + message; }
    }
    public record Memory(int used, int capacity, int free) {}
    public record Plan(boolean valid, Memory ramScript, Memory sb1, Memory sb2, int hotkeys,
                       boolean localOnly, List<String> bindings, List<Diagnostic> diagnostics) {}
    public record Request(Rom rom, List<String> presets, Map<String, String> parameters) {
        public Request { presets = List.copyOf(presets); parameters = Map.copyOf(parameters); }
    }
    public record Artifact(int order, String role, String path, long size) {}
    public record BuildResult(boolean success, List<Artifact> artifacts, List<String> instructions, List<Diagnostic> diagnostics) {}
}
