package com.choppy.desktop.service;

import com.choppy.desktop.model.InjectorData.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Process/JSON adapter. Save-format rules remain exclusively in wc3-injector. */
public final class Wc3InjectorService implements InjectorService {
    private final Path jar;
    private final String java;

    public Wc3InjectorService() { this(BackendPaths.resolve("wc3-injector-api-v1.jar")); }
    public Wc3InjectorService(Path jar) {
        this(jar, Path.of(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString());
    }
    public Wc3InjectorService(Path jar, String java) { this.jar = jar.toAbsolutePath(); this.java = java; }

    @Override public void verifyVersion() {
        JsonObject result = call("version");
        if (!"wc3-injector-json".equals(text(result,"protocol")) || number(result,"apiVersion") != 1)
            throw new IllegalStateException("Unsupported wc3-injector API");
    }

    @Override public SaveInspection inspectSave(Path input) {
        JsonObject result = call("inspect-save", "--input", absolute(input));
        List<Slot> slots = new ArrayList<>();
        for (JsonElement element : array(result,"slots")) slots.add(slot(element.getAsJsonObject()));
        Slot active = result.has("activeSlot") && !result.get("activeSlot").isJsonNull()
            ? slot(result.getAsJsonObject("activeSlot")) : null;
        return new SaveInspection(Path.of(text(result,"input")), List.copyOf(slots), active);
    }

    @Override public Wc3Verification verifyWonderCard(Path input) {
        JsonObject result = call("verify-wc3", "--input", absolute(input));
        return verification(result, Path.of(text(result,"input")));
    }

    @Override public TransferResult inject(Path inputSave, Path wc3, Path output) {
        protectOutput(inputSave, output);
        JsonObject result = call("inject", "--input-save", absolute(inputSave), "--wc3", absolute(wc3), "--output", absolute(output));
        return new TransferResult(true, artifact(result), number(result,"slotIndex"), result.get("saveCounter").getAsLong(),
            number(result,"physicalSector"), number(result,"wonderCardFlagId"), text(result,"sectorChecksumHex"),
            null, null, warnings(result));
    }

    @Override public TransferResult extract(Path inputSave, Path output) {
        protectOutput(inputSave, output);
        JsonObject result = call("extract", "--input-save", absolute(inputSave), "--output", absolute(output));
        JsonObject wc3 = result.getAsJsonObject("wonderCard");
        return new TransferResult(false, artifact(result), number(result,"slotIndex"), result.get("saveCounter").getAsLong(),
            number(result,"physicalSector"), number(wc3,"flagId"), null,
            wc3.get("cardCrcValid").getAsBoolean(), wc3.get("ramScriptChecksumValid").getAsBoolean(), warnings(result));
    }

    private void protectOutput(Path input, Path output) {
        if (input.toAbsolutePath().normalize().equals(output.toAbsolutePath().normalize()))
            throw new IllegalArgumentException("Output must be different from the input save");
        Path parent = output.toAbsolutePath().getParent();
        if (parent == null || !Files.isDirectory(parent)) throw new IllegalArgumentException("Output directory does not exist");
    }

    private JsonObject call(String... args) {
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("Injector JAR missing: " + jar);
        List<String> command = new ArrayList<>(List.of(java,"-jar",jar.toString(),"api"));
        command.addAll(List.of(args));
        Process process = null;
        ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
        try {
            process = new ProcessBuilder(command).start();
            Process running = process;
            Future<String> stdout = readers.submit(() -> new String(running.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            Future<String> stderr = readers.submit(() -> new String(running.getErrorStream().readAllBytes(), StandardCharsets.UTF_8));
            if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new IllegalStateException("Injector timed out"); }
            String body = stdout.get(5,TimeUnit.SECONDS);
            stderr.get(5,TimeUnit.SECONDS);
            return decodeResponse(body, process.exitValue());
        } catch (InterruptedException error) {
            if (process != null) process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Injector request interrupted",error);
        } catch (Exception error) {
            if (error instanceof InjectorException injector) throw injector;
            throw new IllegalStateException("Injector request failed: " + error.getMessage(),error);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            readers.shutdownNow();
        }
    }

    static JsonObject decodeResponse(String body, int exitCode) {
        JsonElement parsed = JsonParser.parseString(body);
        if (!parsed.isJsonObject()) throw new IllegalStateException("Malformed injector response");
        JsonObject envelope = parsed.getAsJsonObject();
        if (!"wc3-injector-json".equals(optionalText(envelope,"protocol"))
                || !envelope.has("apiVersion") || !envelope.get("apiVersion").isJsonPrimitive()
                || !envelope.getAsJsonPrimitive("apiVersion").isNumber()
                || !"1".equals(envelope.get("apiVersion").getAsString()))
            throw new IllegalStateException("Unsupported injector protocol; expected API v1");
        if (!envelope.has("ok") || !envelope.get("ok").isJsonPrimitive())
            throw new IllegalStateException("Malformed injector response");
        if (!envelope.get("ok").getAsBoolean()) {
            JsonObject error = envelope.getAsJsonObject("error");
            throw new InjectorException(optionalText(error,"code"), optionalText(error,"message"));
        }
        if (exitCode != 0 || !envelope.has("result") || !envelope.get("result").isJsonObject())
            throw new IllegalStateException("Unexpected injector process result");
        return envelope.getAsJsonObject("result");
    }

    public static final class InjectorException extends IllegalStateException {
        private final String code;
        InjectorException(String code, String message) { super(message); this.code = code; }
        public String code() { return code; }
    }
    private static Wc3Verification verification(JsonObject o, Path input) {
        return new Wc3Verification(input,number(o,"flagId"),number(o,"iconSpecies"),text(o,"storedCardCrcHex"),
            text(o,"calculatedCardCrcHex"),o.get("cardCrcValid").getAsBoolean(),text(o,"storedRamScriptChecksumHex"),
            text(o,"calculatedRamScriptChecksumHex"),o.get("ramScriptChecksumValid").getAsBoolean(),warnings(o));
    }
    private static Artifact artifact(JsonObject result) {
        JsonObject o = result.getAsJsonObject("output");
        return new Artifact(Path.of(text(o,"path")),o.get("sizeBytes").getAsLong());
    }
    private static Slot slot(JsonObject o) {
        Long counter = o.has("counter") && !o.get("counter").isJsonNull() ? o.get("counter").getAsLong() : null;
        return new Slot(number(o,"slotIndex"),o.get("valid").getAsBoolean(),counter,text(o,"status"));
    }
    private static List<Warning> warnings(JsonObject o) {
        List<Warning> result = new ArrayList<>();
        for (JsonElement element : array(o,"warnings")) {
            JsonObject warning = element.getAsJsonObject();
            result.add(new Warning(text(warning,"severity"),text(warning,"code"),text(warning,"message")));
        }
        return List.copyOf(result);
    }
    private static JsonArray array(JsonObject o,String key) { return o.has(key) ? o.getAsJsonArray(key) : new JsonArray(); }
    private static int number(JsonObject o,String key) { return o.get(key).getAsInt(); }
    private static String text(JsonObject o,String key) { return o.get(key).getAsString(); }
    private static String optionalText(JsonObject o,String key) { return o != null && o.has(key) ? o.get(key).getAsString() : ""; }
    private static String absolute(Path path) { return path.toAbsolutePath().normalize().toString(); }
}
