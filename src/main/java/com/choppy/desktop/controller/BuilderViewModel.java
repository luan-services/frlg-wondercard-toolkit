package com.choppy.desktop.controller;

import com.choppy.desktop.model.BuilderData.*;
import com.choppy.desktop.service.BuilderService;
import javafx.application.Platform;
import javafx.beans.property.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Observable editor state; all backend work runs on an owned worker. */
public final class BuilderViewModel implements AutoCloseable {
    private final BuilderService service;
    private final ExecutorService worker;
    private final Consumer<Runnable> dispatch;
    private volatile boolean closed;
    private boolean loading;
    public final ObjectProperty<Catalog> catalog = new SimpleObjectProperty<>();
    public final ObjectProperty<Path> source = new SimpleObjectProperty<>();
    public final ObjectProperty<Validation> validation = new SimpleObjectProperty<>();
    public final ObjectProperty<Saved> result = new SimpleObjectProperty<>();
    public final BooleanProperty dirty = new SimpleBooleanProperty();
    public final BooleanProperty busy = new SimpleBooleanProperty();
    public final BooleanProperty ready = new SimpleBooleanProperty();
    public final BooleanProperty failed = new SimpleBooleanProperty();
    public final StringProperty status = new SimpleStringProperty("Connecting to builder…");
    public final LongProperty revision = new SimpleLongProperty();
    private final Map<String,StringProperty> fields = new LinkedHashMap<>();

    public BuilderViewModel(BuilderService service) {
        this(service, Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r,"wc3-builder-worker"); t.setDaemon(true); return t;
        }), Platform::runLater);
    }
    public BuilderViewModel(BuilderService service, ExecutorService worker, Consumer<Runnable> dispatch) {
        this.service=service; this.worker=worker; this.dispatch=dispatch;
        for (String name : List.of("title","subtitle","body1","body2","body3","body4","footer1","footer2",
                "flag","icon","id","type","bg","send","stamps")) {
            StringProperty p = new SimpleStringProperty(""); fields.put(name,p);
            p.addListener((o,a,b) -> { if (!loading) { dirty.set(true); revision.set(revision.get()+1); } });
        }
        reconnect();
    }
    public StringProperty field(String name) { return Objects.requireNonNull(fields.get(name)); }
    public boolean isLoading() { return loading; }
    public void reconnect() {
        if (busy.get() || closed) return;
        ready.set(false);
        run("Connecting to builder…", () -> { service.verifyVersion(); return service.catalog(); }, c -> {
            catalog.set(c); ready.set(true); load(c.defaultCard(),null,null);
            status.set("wc3-builder-json / v1 ready");
        });
    }
    public void newCard() {
        if (!ready.get() || busy.get()) return;
        load(catalog.get().defaultCard(),null,null); failed.set(false); status.set("New Wonder Card · ready to build");
    }
    public void open(Path path) {
        if (!ready.get() || busy.get()) return;
        run("Opening Wonder Card…", () -> service.inspect(path), result -> {
            load(result.card(),path.toAbsolutePath().normalize(),result.validation()); status.set("Wonder Card opened");
        });
    }
    public void save(Path output) {
        if (!ready.get() || busy.get()) return;
        final Card card;
        try { card = snapshot(); } catch (RuntimeException e) { failed.set(true); status.set(e.getMessage()); return; }
        Path input = source.get();
        result.set(null);
        run("Building Wonder Card…", () -> service.save(input,output,card), saved -> {
            load(saved.card(),output.toAbsolutePath().normalize(),saved.validation());
            status.set(input == null ? "Built · informational deliveryman event included" : "Built · existing RamScript preserved");
            result.set(saved);
        });
    }
    public Card snapshot() {
        try {
            return new Card(integer("flag"),integer("icon"),Long.parseLong(field("id").get()),integer("type"),integer("bg"),
                integer("send"),integer("stamps"),field("title").get(),field("subtitle").get(),
                List.of(field("body1").get(),field("body2").get(),field("body3").get(),field("body4").get()),
                List.of(field("footer1").get(),field("footer2").get()));
        } catch (NumberFormatException e) { throw new IllegalArgumentException("Enter whole numbers for ID number and max stamps."); }
    }
    private int integer(String name) { return Integer.parseInt(field(name).get()); }
    private void load(Card c, Path path, Validation checks) {
        loading=true;
        try {
            set("title",c.title()); set("subtitle",c.subtitle()); set("flag",c.flagId()); set("icon",c.iconSpecies());
            set("id",c.idNumber()); set("type",c.type()); set("bg",c.background()); set("send",c.sendType()); set("stamps",c.maxStamps());
            for (int i=0;i<4;i++) set("body"+(i+1),c.body().get(i));
            for (int i=0;i<2;i++) set("footer"+(i+1),c.footer().get(i));
            source.set(path); validation.set(checks); dirty.set(false);
        } finally { loading=false; revision.set(revision.get()+1); }
    }
    private void set(String name,Object value) { field(name).set(value.toString()); }
    private <T> void run(String message, Callable<T> task, Consumer<T> success) {
        busy.set(true); failed.set(false); status.set(message);
        worker.submit(() -> {
            try {
                T result=task.call();
                dispatch.accept(() -> { if (!closed) { busy.set(false); success.accept(result); } });
            } catch (Exception e) {
                dispatch.accept(() -> { if (!closed) { busy.set(false); failed.set(true); status.set(e.getMessage() == null ? "Builder request failed" : e.getMessage()); } });
            }
        });
    }
    @Override public void close() { closed=true; worker.shutdownNow(); }
}
