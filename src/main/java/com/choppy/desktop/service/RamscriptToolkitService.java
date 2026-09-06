package com.choppy.desktop.service;

import com.choppy.desktop.model.*;
import com.choppy.desktop.model.ToolkitData.*;
import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Process/JSON boundary only: all domain decisions are delegated to the toolkit. */
public final class RamscriptToolkitService implements ToolkitService {
    private final Path jar;
    private final String java;
    public RamscriptToolkitService() { this(Path.of("lib/ramscript-tools-api-v1.jar")); }
    public RamscriptToolkitService(Path jar) {
        this(jar, Path.of(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString());
    }
    public RamscriptToolkitService(Path jar, String java) { this.jar = jar.toAbsolutePath(); this.java = java; }

    @Override public void verifyVersion() {
        JsonObject result = call(List.of("version"));
        if (!"ramscript-tools-json".equals(string(result, "protocol")))
            throw new IllegalStateException("Unsupported toolkit protocol");
    }

    @Override public List<Preset> listPresets(Rom rom) {
        JsonObject result = call(List.of("list-content", "--rom", romId(rom), "--context", "hotkey-composition"));
        List<Preset> presets = new ArrayList<>();
        for (JsonElement element : array(result, "content")) {
            JsonObject item = element.getAsJsonObject();
            List<Parameter> parameters = new ArrayList<>();
            for (JsonElement p : array(item, "parameters")) {
                JsonObject o = p.getAsJsonObject();
                parameters.add(new Parameter(string(o,"id"), string(o,"type"), o.get("required").getAsBoolean(),
                    string(o,"label"), optional(o,"example")));
            }
            JsonObject validation = item.getAsJsonObject("validation");
            String status = "Single: " + string(validation.getAsJsonObject("singleHotkey"), "status")
                + "\nShared: " + string(validation.getAsJsonObject("sharedHotkey"), "status");
            presets.add(new Preset(string(item,"id"), string(item,"name"),
                string(item.getAsJsonObject("defaultHotkey"),"displayName"), status,
                string(item,"description") + "\n" + validation, List.copyOf(parameters)));
        }
        return List.copyOf(presets);
    }

    @Override public Plan plan(Request request) {
        JsonObject result = call(arguments("plan-composition", request));
        List<Diagnostic> diagnostics = diagnostics(result);
        if (!result.has("valid") && diagnostics.isEmpty()) throw new IllegalStateException("Missing plan validity");
        boolean valid = result.has("valid") && result.get("valid").getAsBoolean();
        if (!valid) return new Plan(false, null, null, null, 0, false, List.of(), diagnostics);
        JsonObject capacity = result.getAsJsonObject("capacity");
        List<String> bindings = new ArrayList<>();
        for (JsonElement selection : array(result,"selections")) {
            JsonObject s = selection.getAsJsonObject();
            bindings.add(string(s,"name") + " — " + string(s.getAsJsonObject("hotkey"),"displayName")
                + " · " + string(s,"deployment"));
        }
        return new Plan(true, memory(capacity,"ramscript"), memory(capacity,"sb1"), memory(capacity,"sb2"),
            result.get("hotkeyBindings").getAsInt(), result.getAsJsonObject("installation").get("localOnly").getAsBoolean(),
            List.copyOf(bindings), diagnostics);
    }

    @Override public BuildResult build(Request request, Path input, Path output) {
        // Require a fresh output directory. This protects input and existing outputs without guessing artifact names.
        try {
            Path parent = output.toAbsolutePath().getParent();
            try (var entries = Files.list(parent)) {
                if (entries.findAny().isPresent()) throw new IllegalArgumentException("Output directory must be empty");
            }
            if (input.toRealPath().startsWith(parent.toRealPath())) throw new IllegalArgumentException("Input must be outside output directory");
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot access input/output: " + error.getMessage(), error); }
        List<String> args = arguments("build-composition", request);
        args.addAll(List.of("--input", input.toAbsolutePath().toString(), "--output", output.toAbsolutePath().toString()));
        JsonObject result = call(args);
        List<Diagnostic> diagnostics = diagnostics(result);
        if (!result.has("artifacts")) return new BuildResult(false,List.of(),List.of(),diagnostics);
        List<Artifact> artifacts = new ArrayList<>();
        for (JsonElement a : array(result,"artifacts")) {
            JsonObject o = a.getAsJsonObject();
            artifacts.add(new Artifact(o.get("order").getAsInt(), string(o,"role"), string(o,"path"), o.get("sizeBytes").getAsLong()));
        }
        List<String> instructions = new ArrayList<>();
        for (JsonElement i : array(result,"instructions")) instructions.add(i.getAsString());
        return new BuildResult(!artifacts.isEmpty(),List.copyOf(artifacts),List.copyOf(instructions),diagnostics);
    }

    private List<String> arguments(String command, Request request) {
        List<String> args = new ArrayList<>(List.of(command,"--rom",romId(request.rom())));
        request.presets().forEach(id -> args.addAll(List.of("--preset",id)));
        request.parameters().forEach((key,value) -> args.addAll(List.of("--param",key + "=" + value)));
        return args;
    }
    private JsonObject call(List<String> args) {
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("Toolkit JAR missing: " + jar);
        List<String> command = new ArrayList<>(List.of(java,"-jar",jar.toString(),"api"));
        command.addAll(args);
        Process process = null;
        ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
        try {
            process = new ProcessBuilder(command).start();
            Process running = process;
            Future<String> stdout = readers.submit(() -> new String(running.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            Future<String> stderr = readers.submit(() -> new String(running.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            if (!process.waitFor(60,TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Toolkit timed out"); }
            String body = stdout.get(5,TimeUnit.SECONDS);
            stderr.get(5,TimeUnit.SECONDS); // Drain separately; never interpret human output as domain data.
            return decodeResponse(body,process.exitValue());
        } catch (InterruptedException error) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Toolkit request interrupted",error);
        } catch (Exception error) {
            throw new IllegalStateException("Toolkit request failed: " + error.getMessage(),error);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            readers.shutdownNow();
        }
    }
    static JsonObject decodeResponse(String body, int exitCode) {
        JsonObject result = JsonParser.parseString(body).getAsJsonObject();
        if (!result.has("apiVersion") || !result.get("apiVersion").isJsonPrimitive()
                || !result.getAsJsonPrimitive("apiVersion").isNumber()
                || !result.get("apiVersion").getAsString().equals("1"))
            throw new IllegalStateException("Unsupported API version; expected 1");
        if (exitCode != 0 && (!result.has("diagnostics") || result.getAsJsonArray("diagnostics").isEmpty()))
            throw new IllegalStateException("Toolkit failed without structured diagnostics");
        return result;
    }
    private static Memory memory(JsonObject root,String key) {
        JsonObject m = root.getAsJsonObject(key);
        return new Memory(m.get("used").getAsInt(),m.get("capacity").getAsInt(),m.get("free").getAsInt());
    }
    private static List<Diagnostic> diagnostics(JsonObject root) {
        List<Diagnostic> result = new ArrayList<>();
        for (JsonElement element : array(root,"diagnostics")) {
            if (element.isJsonPrimitive()) result.add(new Diagnostic("TOOLKIT_NOTE","WARNING",element.getAsString(),List.of(),""));
            else {
                JsonObject o = element.getAsJsonObject();
                List<String> ids = new ArrayList<>();
                for (JsonElement id : array(o,"affectedPresetIds")) ids.add(id.getAsString());
                result.add(new Diagnostic(string(o,"code"),string(o,"severity"),string(o,"message"),List.copyOf(ids),optional(o,"detail")));
            }
        }
        return List.copyOf(result);
    }
    private static JsonArray array(JsonObject o,String key) { return o.getAsJsonArray(key); }
    private static String string(JsonObject o,String key) { return o.get(key).getAsString(); }
    private static String optional(JsonObject o,String key) { return o.has(key) ? o.get(key).toString().replaceAll("^\"|\"$", "") : ""; }
    public static String romId(Rom rom) {
        return switch(rom) { case FIRE_RED_10 -> "fr10"; case LEAF_GREEN_10 -> "lg10"; case FIRE_RED_11 -> "fr11"; case LEAF_GREEN_11 -> "lg11"; };
    }
}
