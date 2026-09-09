package com.choppy.desktop.controller;

import com.choppy.desktop.model.BuilderData.*;
import com.choppy.desktop.service.*;
import com.choppy.desktop.view.wondercard.WonderCardAssets;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BuilderViewModelTest {
    private static final Catalog CATALOG=new Wc3BuilderService().catalog();
    private static class InlineExecutor extends AbstractExecutorService {
        boolean closed;
        public void shutdown() { closed=true; }
        public List<Runnable> shutdownNow() { closed=true; return List.of(); }
        public boolean isShutdown() { return closed; }
        public boolean isTerminated() { return closed; }
        public boolean awaitTermination(long t,TimeUnit unit) { return closed; }
        public void execute(Runnable r) { r.run(); }
    }
    private static class Fake implements BuilderService {
        Path lastSource,lastOutput;
        Card lastCard;
        Inspection inspection=new Inspection(CATALOG.defaultCard(),new Validation(true,false));
        boolean fail;
        public void verifyVersion() { if (fail) throw new IllegalStateException("Missing builder JAR"); }
        public Catalog catalog() { return CATALOG; }
        public Inspection inspect(Path p) { return inspection; }
        public Saved save(Path source,Path output,Card c) {
            if (fail) throw new IllegalStateException("Cannot save");
            lastSource=source; lastOutput=output; lastCard=c;
            return new Saved(c,new Validation(true,true),source!=null,source==null ? "DEFAULT_INFORMATIONAL_PLACEHOLDER" : "");
        }
    }
    private BuilderViewModel vm(Fake fake) { return new BuilderViewModel(fake,new InlineExecutor(),Runnable::run); }
    @Test void loadsDefaultsTracksEveryFieldAndResetsToBackendDefaults() {
        try (var vm=vm(new Fake())) {
            assertTrue(vm.ready.get()); assertFalse(vm.dirty.get()); assertNull(vm.source.get());
            assertEquals(CATALOG.defaultCard(),vm.snapshot());
            for (String field : List.of("title","subtitle","body1","body2","body3","body4","footer1","footer2","flag","icon","id","type","bg","send","stamps")) {
                vm.field(field).set(vm.field(field).get()+"1"); assertTrue(vm.dirty.get(),field);
                vm.newCard(); assertFalse(vm.dirty.get()); assertEquals(CATALOG.defaultCard(),vm.snapshot());
            }
        }
    }
    @Test void newSaveCreatesAndSubsequentSaveUsesTheWrittenSource() {
        Fake fake=new Fake();
        try (var vm=vm(fake)) {
            Path output=Path.of("first.wc3"); vm.field("title").set("NEW"); vm.save(output);
            assertNull(fake.lastSource); assertEquals(output.toAbsolutePath(),vm.source.get()); assertFalse(vm.dirty.get());
            vm.save(Path.of("copy.wc3")); assertEquals(output.toAbsolutePath(),fake.lastSource);
            vm.newCard(); assertNull(vm.source.get()); vm.save(Path.of("new.wc3")); assertNull(fake.lastSource);
        }
    }
    @Test void inspectMapsAllFieldsPreservesRawIconAndUsesEdit() {
        Fake fake=new Fake(); Card d=CATALOG.defaultCard();
        Card raw=new Card(d.flagId(),65000,123,2,7,1,5,"Title","Subtitle",List.of("one","two","three","four"),List.of("foot1","foot2"));
        fake.inspection=new Inspection(raw,new Validation(false,true));
        try (var vm=vm(fake)) {
            Path source=Path.of("open.wc3"); vm.open(source);
            assertEquals(raw,vm.snapshot()); assertFalse(vm.dirty.get()); assertFalse(vm.validation.get().cardCrcValid());
            vm.field("title").set("Changed"); vm.save(Path.of("copy.wc3"));
            assertEquals(source.toAbsolutePath(),fake.lastSource); assertEquals(65000,fake.lastCard.iconSpecies());
        }
    }
    @Test void failedSaveKeepsDirtyEditorAndOriginalSource() {
        Fake fake=new Fake();
        try (var vm=vm(fake)) {
            vm.open(Path.of("source.wc3")); vm.field("title").set("Unsaved"); fake.fail=true;
            vm.save(Path.of("failed.wc3")); assertTrue(vm.failed.get()); assertTrue(vm.dirty.get());
            assertEquals(Path.of("source.wc3").toAbsolutePath(),vm.source.get()); assertFalse(vm.busy.get());
        }
    }
    @Test void connectionFailureCanRetry() {
        Fake fake=new Fake(); fake.fail=true;
        try (var vm=vm(fake)) {
            assertFalse(vm.ready.get()); assertTrue(vm.failed.get());
            fake.fail=false; vm.reconnect(); assertTrue(vm.ready.get()); assertEquals(CATALOG.defaultCard(),vm.snapshot());
        }
    }
    @Test void missingAssetsHaveUsefulErrors() {
        var assets=new WonderCardAssets(p -> null);
        assertTrue(assertThrows(IllegalStateException.class,() -> assets.image("backgrounds/0.png")).getMessage().contains("Preview asset missing"));
        assertThrows(IllegalStateException.class,assets::font);
    }
}
