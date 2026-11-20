package org.a8043.cwaFX.components;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MaterialTextFieldTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
        }
        onFx(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
    }

    @Test
    void floatsLabelIntoBorderGapAndRestoresBorderWhenEmpty() throws Exception {
        for (String theme : new String[]{"light", "dark"}) {
            MaterialTextField field = onFx(() -> {
                MaterialTextField control = new MaterialTextField("Account");
                if (theme.equals("dark")) control.setType(MaterialTextField.Type.PASSWORD);
                Pane root = new Pane(control);
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/defaultStyles/" + theme + ".css").toExternalForm());
                root.applyCss();
                control.resize(220, control.prefHeight(220));
                control.layout();
                assertNull(control.lookup(".mtf-outline").getClip());
                control.setText("sample");
                return control;
            });
            awaitAnimation();
            onFx(() -> {
                field.layout();
                Label label = (Label) field.lookup(".mtf-label");
                Bounds bounds = label.getBoundsInParent();
                for (String selector : new String[]{".mtf-outline", ".mtf-focus-outline"}) {
                    Region border = (Region) field.lookup(selector);
                    assertEquals(border.getLayoutY(), bounds.getCenterY(), 0.01);
                    assertTrue(bounds.getMinY() >= 0);
                    assertNotNull(border.getClip());
                    double gapX = bounds.getCenterX() - border.getLayoutX();
                    assertFalse(border.getClip().contains(gapX, 1));
                    assertTrue(border.getClip().contains(border.getWidth() - 10, 1));
                    assertTrue(border.getClip().contains(1, border.getHeight() / 2));
                    assertTrue(border.getClip().contains(gapX, border.getHeight() - 1));
                }
                Region border = (Region) field.lookup(".mtf-outline");
                assertEquals(border.getBoundsInParent().getCenterY(),
                    field.getTextField().getBoundsInParent().getCenterY(), 0.01);
                field.setText("");
                return null;
            });
            awaitAnimation();
            onFx(() -> {
                assertNull(field.lookup(".mtf-outline").getClip());
                assertNull(field.lookup(".mtf-focus-outline").getClip());
                return null;
            });
        }
    }

    private static void awaitAnimation() throws Exception {
        CompletableFuture<Void> finished = new CompletableFuture<>();
        onFx(() -> {
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(event -> finished.complete(null));
            pause.play();
            return null;
        });
        finished.get(10, TimeUnit.SECONDS);
    }

    private static <T> T onFx(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
