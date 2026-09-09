package com.choppy.desktop.view.wondercard;

import com.google.gson.*;
import javafx.scene.image.Image;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

/** Cached, prepared resources only. Never reads a WC3 or accesses the network. */
public final class WonderCardAssets {
    public static final String ROOT="/com/choppy/desktop/frlg/wondercard/";
    private final Function<String,InputStream> resources;
    private final Map<String,Image> images=new HashMap<>();
    private JsonObject font;
    public WonderCardAssets() { this(path -> WonderCardAssets.class.getResourceAsStream(ROOT+path)); }
    public WonderCardAssets(Function<String,InputStream> resources) { this.resources=resources; }
    public Image image(String path) {
        return images.computeIfAbsent(path,key -> {
            try (InputStream input=resources.apply(key)) {
                if (input==null) throw new IllegalStateException("Preview asset missing: " + key);
                Image image=new Image(input);
                if (image.isError()) throw new IllegalStateException("Preview asset could not be read: " + key);
                return image;
            } catch (IOException e) { throw new IllegalStateException("Preview asset could not be read: " + key,e); }
        });
    }
    public JsonObject font() {
        if (font==null) {
            try (InputStream input=resources.apply("fonts/normal.json")) {
                if (input==null) throw new IllegalStateException("Preview font metadata is missing");
                font=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) { throw new IllegalStateException("Preview font metadata could not be read",e); }
        }
        return font;
    }
}
