package com.choppy.desktop;

import com.choppy.desktop.model.Rom;
import com.choppy.desktop.model.ToolkitData.Request;
import com.choppy.desktop.service.*;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.stage.Stage;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Invoked by package-windows.ps1 with the packaged runtime, never included in the ZIP. */
public final class PackagedAppSmokeTest {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        var builder = new Wc3BuilderService();
        var injector = new Wc3InjectorService();
        var presets = new RamscriptToolkitService();
        builder.verifyVersion(); injector.verifyVersion(); presets.verifyVersion();
        var catalog = builder.catalog();
        Path card = output.resolve("portable-test.wc3");
        builder.save(null, card, catalog.defaultCard());
        var inspected = builder.inspect(card);
        if (!builder.save(card, card, inspected.card()).ramScriptPreserved())
            throw new AssertionError("Edit did not preserve the script");
        if (!injector.verifyWonderCard(card).cardCrcValid()) throw new AssertionError("Invalid card");
        if (presets.listPresets(Rom.LEAF_GREEN_10).isEmpty()) throw new AssertionError("Empty preset catalog");
        if (!presets.plan(new Request(Rom.LEAF_GREEN_10, List.of("repel"), Map.of())).valid())
            throw new AssertionError("Preset planning failed");
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                ToolkitApplication app = new ToolkitApplication();
                Stage stage = new Stage();
                app.start(stage);
                PauseTransition wait = new PauseTransition(Duration.seconds(3));
                wait.setOnFinished(event -> {
                    try {
                        var tabs = (javafx.scene.control.TabPane)stage.getScene().getRoot().lookup(".tool-tabs");
                        tabs.getSelectionModel().select(1);
                        stage.getScene().getRoot().applyCss();
                        stage.getScene().getRoot().layout();
                        var image = stage.getScene().snapshot(null);
                        BufferedImage png = new BufferedImage((int)image.getWidth(), (int)image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                        for (int y=0; y<png.getHeight(); y++) for (int x=0; x<png.getWidth(); x++)
                            png.setRGB(x,y,image.getPixelReader().getArgb(x,y));
                        ImageIO.write(png,"png",output.resolve("portable-ui.png").toFile());
                        stage.hide();
                    } catch (Throwable e) { failure.set(e); }
                    finally { done.countDown(); Platform.exit(); }
                });
                wait.play();
            } catch (Throwable e) { failure.set(e); done.countDown(); Platform.exit(); }
        });
        if (!done.await(25, TimeUnit.SECONDS)) throw new AssertionError("UI startup timed out");
        if (failure.get()!=null) throw new AssertionError("Packaged UI failed",failure.get());
        System.out.println("Portable smoke test passed: all three APIs, create/edit, planning and JavaFX.");
    }
}
