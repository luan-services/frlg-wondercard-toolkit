package com.choppy.desktop.controller;

import com.choppy.desktop.model.Rom;
import com.choppy.desktop.service.RamscriptToolkitService;
import com.choppy.desktop.view.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

class PresetBuilderViewModelTest {
    @BeforeAll static void startFx() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @AfterAll static void stopFx() { Platform.exit(); }
    private static <T> T fx(Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(10,TimeUnit.SECONDS);
    }
    private static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < deadline) {
            if (fx(condition::getAsBoolean)) return;
            Thread.sleep(30);
        }
        fail("UI state did not settle");
    }
    @Test void realUiLoadsParametersAndDiscardsOldPlans() throws Exception {
        PresetBuilderViewModel vm = fx(() -> new PresetBuilderViewModel(new RamscriptToolkitService()));
        Stage stage = fx(() -> {
            Stage window = new Stage();
            Scene scene = new Scene(new ToolkitView(new PresetBuilderView(vm)),1080,780);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            window.setScene(scene); window.show(); return window;
        });
        try {
            await(() -> vm.ready.get() && !vm.planning.get());
            assertFalse(fx(() -> vm.composition.get().valid()));
            fx(() -> { vm.selected.add("seed-modifier"); return null; });
            await(() -> !vm.planning.get());
            assertTrue(fx(() -> vm.composition.get().diagnostics().stream().anyMatch(d -> d.code().equals("MISSING_PARAMETER"))));
            fx(() -> {
                vm.parameters.put("seed","B5B1E7AD");
                vm.rom.set(Rom.LEAF_GREEN_10);
                vm.rom.set(Rom.FIRE_RED_11);
                vm.rom.set(Rom.LEAF_GREEN_10);
                return null;
            });
            await(() -> vm.ready.get() && !vm.planning.get());
            assertTrue(fx(() -> vm.composition.get().valid()));
            assertEquals(449,fx(() -> vm.composition.get().ramScript().used()));
            assertTrue(fx(() -> stage.getScene().getRoot().lookupAll(".button").stream()
                .filter(n -> n instanceof Button b && b.getText().equals("Generate"))
                .allMatch(n -> n.isDisabled()))); // No base file yet.
            fx(() -> {
                vm.selected.clear();
                vm.selected.addAll(java.util.List.of("seed-modifier-box14","repel","party-iv-viewer","run-bike-anywhere"));
                return null;
            });
            await(() -> !vm.planning.get());
            assertTrue(fx(() -> vm.composition.get().valid()));
            fx(() -> {
                WritableImage image = stage.getScene().snapshot(null);
                BufferedImage png = new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
                for (int y=0;y<png.getHeight();y++) for(int x=0;x<png.getWidth();x++) png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                ImageIO.write(png,"png",new File("target/integration-ui.png"));
                return null;
            });
        } finally { fx(() -> { stage.close(); vm.close(); return null; }); }
    }
    @Test void missingJarIsRecoverableUiState() throws Exception {
        PresetBuilderViewModel vm = fx(() -> new PresetBuilderViewModel(new RamscriptToolkitService(java.nio.file.Path.of("missing-toolkit.jar"))));
        try {
            await(() -> !vm.planning.get());
            assertFalse(fx(() -> vm.ready.get()));
            assertTrue(fx(() -> vm.message.get().contains("missing")));
        } finally { fx(() -> { vm.close(); return null; }); }
    }
    @Test void builderLoadsRealDefaultsAndRendersAtDesktopAndCompactSizes() throws Exception {
        BuilderViewModel vm=fx(() -> new BuilderViewModel(new com.choppy.desktop.service.Wc3BuilderService()));
        Stage stage=fx(() -> {
            Stage window=new Stage();
            Scene scene=new Scene(new BuilderView(vm),1080,760);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            window.setScene(scene); window.show(); return window;
        });
        try {
            await(() -> vm.ready.get() && !vm.busy.get());
            assertEquals(65535,fx(() -> vm.snapshot().iconSpecies()));
            assertFalse(fx(() -> vm.dirty.get()));
            fx(() -> {
                var renderer=new com.choppy.desktop.view.wondercard.WonderCardRenderer(new com.choppy.desktop.view.wondercard.WonderCardAssets());
                BufferedImage sheet=new BufferedImage(240*8,160*3,BufferedImage.TYPE_INT_ARGB);
                for (var bg : vm.catalog.get().backgrounds()) {
                    vm.field("bg").set(Integer.toString(bg.value()));
                    for (var type : vm.catalog.get().cardTypes()) {
                        vm.field("type").set(Integer.toString(type.value())); vm.field("stamps").set("7");
                        var rendered=renderer.render(vm.snapshot(),vm.catalog.get());
                        assertEquals(240,rendered.getWidth());
                        assertFalse(renderer.hasUnsupportedText());
                        for (int y=0;y<160;y++) for (int x=0;x<240;x++) sheet.setRGB(bg.value()*240+x,type.value()*160+y,rendered.getPixelReader().getArgb(x,y));
                    }
                }
                ImageIO.write(sheet,"png",new File("target/builder-backgrounds.png"));
                vm.newCard();
                snapshot(stage,"target/builder-ui.png");
                return null;
            });
            fx(() -> {
                javafx.scene.control.TextField search=(javafx.scene.control.TextField)stage.getScene().getRoot().lookupAll(".text-field").stream()
                    .filter(n -> n instanceof javafx.scene.control.TextField f && f.getPromptText().startsWith("Search Pokémon")).findFirst().orElseThrow();
                search.setText("Treecko");
                assertEquals(65535,vm.snapshot().iconSpecies()); assertFalse(vm.dirty.get());
                @SuppressWarnings("unchecked") javafx.scene.control.ComboBox<com.choppy.desktop.model.BuilderData.Choice> combo=
                    (javafx.scene.control.ComboBox<com.choppy.desktop.model.BuilderData.Choice>)stage.getScene().getRoot().lookupAll(".combo-box").stream()
                        .filter(n -> n instanceof javafx.scene.control.ComboBox<?> b && b.getItems().stream().anyMatch(v -> v.toString().startsWith("Treecko"))).findFirst().orElseThrow();
                combo.getSelectionModel().select(combo.getItems().stream().filter(c -> c.id().equals("TREECKO")).findFirst().orElseThrow());
                assertEquals(277,vm.snapshot().iconSpecies());
                search.setText("Pikachu"); assertEquals(277,vm.snapshot().iconSpecies());
                search.clear(); vm.newCard();
                stage.setWidth(700); stage.setHeight(620); return null;
            });
            fx(() -> { snapshot(stage,"target/builder-ui-compact.png"); return null; });
        } finally { fx(() -> { stage.close(); vm.close(); return null; }); }
    }
    @Test void builderValidatesInputsAndShowsBuildDetails() throws Exception {
        BuilderViewModel vm=fx(() -> new BuilderViewModel(new com.choppy.desktop.service.Wc3BuilderService()));
        Stage stage=fx(() -> {
            Stage window=new Stage(); Scene scene=new Scene(new BuilderView(vm),1080,760);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            window.setScene(scene); window.show(); return window;
        });
        try {
            await(() -> vm.ready.get() && !vm.busy.get());
            fx(() -> {
                var id=builderInput(stage,"ID number");
                id.replaceText(0,id.getLength(),"-1");
                assertTrue(id.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("invalid")));
                Button build=(Button)stage.getScene().getRoot().lookupAll(".button").stream()
                    .filter(n -> n instanceof Button b && b.getText().equals("Build")).findFirst().orElseThrow();
                assertTrue(build.isDisabled()); snapshot(stage,"target/builder-invalid.png");
                id.replaceText(0,id.getLength(),"123"); assertFalse(build.isDisabled());
                var title=builderInput(stage,"Title");
                title.replaceText(0,title.getLength(),"Hello @😀");
                assertEquals("Hello ??",title.getText()); assertEquals(title.getText(),vm.field("title").get());
                title.replaceText(0,title.getLength(),"W".repeat(40)); assertEquals("Hello ??",title.getText());
                var subtitle=builderInput(stage,"Subtitle"); String previous=subtitle.getText();
                subtitle.replaceText(0,subtitle.getLength(),"W".repeat(30)); assertEquals(previous,subtitle.getText());
                return null;
            });
            java.nio.file.Path output=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("target"),"builder-ui-test-").resolve("card.wc3");
            fx(() -> { vm.save(output); return null; });
            await(() -> javafx.stage.Window.getWindows().stream().anyMatch(w -> w instanceof Stage s && "Wonder Card Builder".equals(s.getTitle())));
            fx(() -> {
                Stage dialog=(Stage)javafx.stage.Window.getWindows().stream()
                    .filter(w -> w instanceof Stage s && "Wonder Card Builder".equals(s.getTitle())).findFirst().orElseThrow();
                snapshot(dialog,"target/builder-success.png");
                javafx.scene.control.DialogPane pane=(javafx.scene.control.DialogPane)dialog.getScene().getRoot();
                assertTrue(pane.lookupAll(".label").stream().anyMatch(n -> n instanceof javafx.scene.control.Label l && l.getText().contains("card.wc3")));
                ((Button)pane.lookupButton(javafx.scene.control.ButtonType.CLOSE)).fire(); return null;
            });
            assertTrue(java.nio.file.Files.isRegularFile(output)); assertNotNull(fx(() -> vm.result.get()));
        } finally { fx(() -> { stage.close(); vm.close(); return null; }); }
    }
    private static javafx.scene.control.TextField builderInput(Stage stage,String name) {
        return (javafx.scene.control.TextField)stage.getScene().getRoot().lookupAll(".text-field").stream()
            .filter(n -> name.equals(n.getAccessibleText())).findFirst().orElseThrow();
    }
    private static void snapshot(Stage stage,String path) throws Exception {
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
        WritableImage image=stage.getScene().snapshot(null);
        BufferedImage png=new BufferedImage((int)image.getWidth(),(int)image.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<png.getHeight();y++) for (int x=0;x<png.getWidth();x++) png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
        ImageIO.write(png,"png",new File(path));
    }
}
