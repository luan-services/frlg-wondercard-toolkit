package com.choppy.desktop.service;

import com.choppy.desktop.model.BuilderData.*;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** JSON/process boundary. Writes are staged next to the destination before replacement. */
public final class Wc3BuilderService implements BuilderService {
    private final Path jar;
    private final String java;
    public Wc3BuilderService() { this(Path.of("lib/wc3-builder-api-v1.jar")); }
    public Wc3BuilderService(Path jar) {
        this(jar, Path.of(System.getProperty("java.home"), "bin",
            System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java").toString());
    }
    public Wc3BuilderService(Path jar, String java) { this.jar = jar.toAbsolutePath(); this.java = java; }
    @Override public void verifyVersion() { call(List.of("version")); }
    @Override public Catalog catalog() {
        JsonObject o = call(List.of("catalog"));
        Map<String,Integer> limits = new HashMap<>();
        o.getAsJsonObject("limits").entrySet().forEach(e -> limits.put(e.getKey(), e.getValue().getAsInt()));
        return new Catalog(choices(o,"cardTypes"), choices(o,"sendTypes"), choices(o,"backgrounds"),
            choices(o,"receiveSlots"), choices(o,"iconSpecies"), Map.copyOf(limits),
            card(o.getAsJsonObject("defaultCard")), str(o,"defaultRamScriptBehavior"));
    }
    @Override public Inspection inspect(Path input) {
        JsonObject o = call(List.of("inspect", "--input", input.toAbsolutePath().toString()));
        return new Inspection(card(o.getAsJsonObject("card")), validation(o));
    }
    @Override public Saved save(Path source, Path output, Card card) {
        Path target = output.toAbsolutePath().normalize();
        Path staging = null;
        try {
            if (!Files.isDirectory(target.getParent())) throw new IllegalArgumentException("Output directory does not exist");
            staging = Files.createTempFile(target.getParent(), ".wc3-builder-", ".wc3");
            List<String> args = new ArrayList<>();
            args.add(source == null ? "create" : "edit");
            if (source != null) Collections.addAll(args, "--input", source.toAbsolutePath().toString());
            Collections.addAll(args, "--output", staging.toString());
            add(args,"flag",card.flagId()); add(args,"icon",card.iconSpecies()); add(args,"id",card.idNumber());
            add(args,"type",card.type()); add(args,"bg",card.background()); add(args,"send",card.sendType());
            add(args,"stamps",card.maxStamps()); add(args,"title",card.title()); add(args,"subtitle",card.subtitle());
            for (int i=0; i<card.body().size(); i++) add(args,"body"+(i+1),card.body().get(i));
            for (int i=0; i<card.footer().size(); i++) add(args,"footer"+(i+1),card.footer().get(i));
            JsonObject o = call(args);
            boolean preserved = o.has("ramScriptPreserved") && o.get("ramScriptPreserved").getAsBoolean();
            if (source != null && !preserved) throw new IllegalStateException("Builder did not confirm RamScript preservation; original file was not replaced");
            Saved saved = new Saved(card(o.getAsJsonObject("card")), validation(o), preserved,
                o.has("ramScriptBehavior") ? str(o,"ramScriptBehavior") : "");
            // Require atomic replacement: failure leaves the user's destination intact.
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return saved;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Could not safely save Wonder Card: " + e.getMessage(), e);
        } finally {
            if (staging != null) try { Files.deleteIfExists(staging); } catch (java.io.IOException ignored) { }
        }
    }
    private static void add(List<String> args, String key, Object value) { args.add("--"+key); args.add(value.toString()); }
    private JsonObject call(List<String> args) {
        if (!Files.isRegularFile(jar)) throw new IllegalStateException("Builder JAR missing: " + jar);
        List<String> command = new ArrayList<>(List.of(java,"-jar",jar.toString(),"api")); command.addAll(args);
        Process process = null;
        ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
        try {
            process = new ProcessBuilder(command).start();
            Process running = process;
            Future<String> stdout = readers.submit(() -> new String(running.getInputStream().readAllBytes(),StandardCharsets.UTF_8));
            Future<String> stderr = readers.submit(() -> new String(running.getErrorStream().readAllBytes(),StandardCharsets.UTF_8));
            if (!process.waitFor(60,TimeUnit.SECONDS)) throw new IllegalStateException("Builder timed out");
            String body = stdout.get(5,TimeUnit.SECONDS), errors = stderr.get(5,TimeUnit.SECONDS);
            if (body.isBlank()) throw new IllegalStateException("Builder returned no JSON" + (errors.isBlank() ? "" : ": " + errors.strip()));
            return decodeResponse(body,process.exitValue());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new IllegalStateException("Builder request interrupted",e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Builder request failed: " + e.getMessage(),e);
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            readers.shutdownNow();
        }
    }
    static JsonObject decodeResponse(String text, int exit) {
        try {
            JsonObject o = JsonParser.parseString(text).getAsJsonObject();
            if (!"wc3-builder-json".equals(str(o,"protocol")) || !o.get("apiVersion").isJsonPrimitive()
                    || !o.getAsJsonPrimitive("apiVersion").isNumber() || !"1".equals(o.get("apiVersion").getAsString()))
                throw new IllegalStateException("Unsupported builder protocol; expected wc3-builder-json / v1");
            if (!o.getAsJsonPrimitive("ok").isBoolean()) throw new IllegalArgumentException("Invalid ok field");
            if (!o.get("ok").getAsBoolean()) {
                JsonObject error = o.getAsJsonObject("error");
                throw new BuilderException(str(error,"code"),str(error,"message"));
            }
            if (exit != 0) throw new IllegalStateException("Builder exited with code " + exit);
            return o;
        } catch (BuilderException e) { throw e;
        } catch (IllegalStateException e) { throw e;
        } catch (RuntimeException e) { throw new IllegalStateException("Malformed builder JSON response",e); }
    }
    public static final class BuilderException extends IllegalStateException {
        private final String code;
        BuilderException(String code,String message) { super(code + ": " + message); this.code=code; }
        public String code() { return code; }
    }
    private static List<Choice> choices(JsonObject o, String key) {
        List<Choice> list = new ArrayList<>();
        for (JsonElement element : o.getAsJsonArray(key)) {
            JsonObject c = element.getAsJsonObject();
            int value = c.get(c.has("receiveId") ? "receiveId" : "value").getAsInt();
            String id = str(c,"id");
            String label = c.has("label") ? str(c,"label") : c.has("description") ? str(c,"description") : id.replace('_',' ').toLowerCase(Locale.ROOT);
            if (!label.isEmpty()) label=Character.toUpperCase(label.charAt(0))+label.substring(1);
            if (c.has("receiveId")) label += " ("+value+")";
            list.add(new Choice(value,id,label,c.has("nationalDex") && !c.get("nationalDex").isJsonNull() ? c.get("nationalDex").getAsInt() : null,
                c.has("kind") ? str(c,"kind") : ""));
        }
        return List.copyOf(list);
    }
    static Card card(JsonObject o) {
        return new Card(o.get("flagId").getAsInt(),o.get("iconSpecies").getAsInt(),o.get("idNumber").getAsLong(),
            o.getAsJsonObject("type").get("value").getAsInt(),o.get("background").getAsInt(),
            o.getAsJsonObject("sendType").get("value").getAsInt(),o.get("maxStamps").getAsInt(),
            str(o,"title"),str(o,"subtitle"),strings(o,"body"),strings(o,"footer"));
    }
    private static List<String> strings(JsonObject o,String key) {
        List<String> list = new ArrayList<>(); o.getAsJsonArray(key).forEach(e -> list.add(e.getAsString())); return list;
    }
    private static Validation validation(JsonObject o) {
        JsonObject v = o.getAsJsonObject("validation");
        return new Validation(v.get("cardCrcValid").getAsBoolean(),v.get("ramScriptChecksumValid").getAsBoolean());
    }
    private static String str(JsonObject o,String key) { return o.get(key).getAsString(); }
}
