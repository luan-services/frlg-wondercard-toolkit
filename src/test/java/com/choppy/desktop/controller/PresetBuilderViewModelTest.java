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
}
