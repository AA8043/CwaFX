package org.a8043.cwaFX.components;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.css.PseudoClass;
import javafx.event.EventType;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MaterialSwitchTest {
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
    void exposesDefaultsAndSizesLikeSpinner() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            assertFalse(control.isSelected());
            assertEquals("", control.getText());
            assertNull(control.getTooltip());
            assertTrue(control.isAnimated());
            assertTrue(control.isFocusTraversable());
            assertEquals(AccessibleRole.TOGGLE_BUTTON, control.getAccessibleRole());
            assertTrue(control.getStyleClass().contains("material-switch"));
            assertEquals(Color.web("#0071ff"), control.getColor());
            assertEquals(52, control.prefWidth(-1));
            assertEquals(32, control.prefHeight(-1));
            assertEquals(0, control.minWidth(-1));
            assertEquals(52, control.maxWidth(-1));

            MaterialSwitch large = new MaterialSwitch(64);
            assertEquals(104, large.prefWidth(-1));
            assertEquals(64, large.prefHeight(-1));
            assertTrue(new MaterialSwitch(true).isSelected());
            return null;
        });
    }

    @Test
    void laysOutBoundTextAndActivatesWhenClickingTheLabel() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch("Enable notifications");
            Pane root = new Pane(control);
            new Scene(root);
            root.applyCss();
            control.resize(control.prefWidth(-1), control.prefHeight(-1));
            control.layout();
            Label label = (Label) control.lookup(".switch-label");
            Rectangle track = (Rectangle) control.lookup(".switch-track");
            assertEquals("Enable notifications", label.getText());
            assertTrue(label.isVisible());
            assertEquals(track.getX() + track.getWidth() + 12, label.getLayoutX(), 0.001);
            assertEquals(52, track.getWidth(), 0.001);
            assertEquals(control.getText(), control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
            control.setAccessibleText("Custom accessible name");
            assertEquals("Custom accessible name", control.queryAccessibleAttribute(AccessibleAttribute.TEXT));
            double x = label.getLayoutX() + label.getWidth() / 2;
            mouse(control, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, x, 16);
            mouse(control, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, x, 16);
            assertTrue(control.isSelected());

            SimpleStringProperty text = new SimpleStringProperty("Auto save");
            control.textProperty().bind(text);
            assertEquals("Auto save", label.getText());
            text.set(null);
            control.resize(control.prefWidth(-1), control.prefHeight(-1));
            control.layout();
            assertFalse(label.isVisible());
            assertEquals(52, control.prefWidth(-1), 0.001);
            control.textProperty().unbind();
            control.setText(null);
            assertEquals("", control.getText());

            MaterialSwitch large = new MaterialSwitch(64);
            large.setText("Large switch");
            assertTrue(large.prefWidth(-1) > 104);
            assertEquals(64, large.prefHeight(-1));
            return null;
        });
    }

    @Test
    void installsReplacesAndBindsHoverTooltips() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            Tooltip first = new Tooltip("Enable notifications");
            control.setTooltip(first);
            assertSame(first, control.getTooltip());
            assertTrue(control.getProperties().containsValue(first));
            Tooltip second = new Tooltip("Auto save");
            SimpleObjectProperty<Tooltip> tooltip = new SimpleObjectProperty<>(second);
            control.tooltipProperty().bind(tooltip);
            assertSame(second, control.getTooltip());
            assertFalse(control.getProperties().containsValue(first));
            assertTrue(control.getProperties().containsValue(second));
            tooltip.set(null);
            assertNull(control.getTooltip());
            assertFalse(control.getProperties().containsValue(second));
            return null;
        });
    }

    @Test
    void switchesDefaultThemeColorsAndStylesLabelAndDisabledState() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch("Notifications");
            Pane root = new Pane(control);
            new Scene(root);
            String light = MaterialSwitchTest.class.getResource("/defaultStyles/light.css").toExternalForm();
            String dark = MaterialSwitchTest.class.getResource("/defaultStyles/dark.css").toExternalForm();
            root.getStylesheets().setAll(light);
            root.applyCss();
            control.resize(control.prefWidth(-1), control.prefHeight(-1));
            control.layout();
            Label label = (Label) control.lookup(".switch-label");
            Rectangle track = (Rectangle) control.lookup(".switch-track");
            Circle thumb = (Circle) control.lookup(".switch-thumb");
            assertEquals(Color.web("#4a90d9"), control.getColor());
            assertEquals(Color.web("#e8ecf1"), track.getFill());
            assertEquals(Color.web("#333333"), label.getTextFill());
            control.setSelected(true);
            control.layout();
            assertEquals(Color.web("#4a90d9"), track.getFill());
            assertEquals(Color.WHITE, thumb.getFill());

            root.getStylesheets().setAll(dark);
            root.applyCss();
            control.layout();
            assertEquals(Color.web("#5a9fe6"), track.getFill());
            assertEquals(Color.web("#1e2129"), thumb.getFill());
            assertEquals(Color.web("#e4e6eb"), label.getTextFill());
            control.setSelected(false);
            control.layout();
            assertEquals(Color.web("#3d434f"), track.getFill());
            assertEquals(Color.web("#9aa2b0"), thumb.getFill());
            control.setDisable(true);
            root.applyCss();
            assertEquals(Cursor.DEFAULT, control.getCursor());
            assertEquals(0.38, label.getOpacity(), 0.001);
            control.setDisable(false);
            root.getStylesheets().setAll(light);
            root.applyCss();
            control.layout();
            assertEquals(Color.web("#e8ecf1"), track.getFill());
            assertEquals(Color.web("#333333"), label.getTextFill());
            assertEquals(Cursor.HAND, control.getCursor());
            assertEquals(1, label.getOpacity(), 0.001);
            return null;
        });
    }

    @Test
    void firesActionsOnlyForActivationAndRespectsDisableAndBindings() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            AtomicInteger actions = new AtomicInteger();
            control.setOnAction(event -> {
                assertSame(control, event.getSource());
                actions.incrementAndGet();
            });
            control.setSelected(true);
            assertEquals(0, actions.get());
            assertTrue(control.getPseudoClassStates().contains(PseudoClass.getPseudoClass("selected")));
            control.fire();
            assertFalse(control.isSelected());
            assertEquals(1, actions.get());

            Pane parent = new Pane(control);
            parent.setDisable(true);
            control.fire();
            assertFalse(control.isSelected());
            assertEquals(1, actions.get());
            parent.setDisable(false);

            SimpleBooleanProperty model = new SimpleBooleanProperty(true);
            control.selectedProperty().bind(model);
            control.fire();
            assertTrue(control.isSelected());
            assertEquals(1, actions.get());
            model.set(false);
            assertFalse(control.isSelected());
            control.selectedProperty().unbind();
            control.selectedProperty().bindBidirectional(model);
            control.fire();
            assertTrue(model.get());
            assertEquals(2, actions.get());
            control.setOnAction(null);
            control.fire();
            assertEquals(2, actions.get());
            return null;
        });
    }

    @Test
    void handlesPrimaryMouseReleaseInsideAndCancelsOutside() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            control.resize(52, 32);
            mouse(control, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, 20, 16);
            assertFalse(control.isSelected());
            mouse(control, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, 20, 16);
            assertTrue(control.isSelected());
            mouse(control, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, 20, 16);
            mouse(control, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, 100, 16);
            assertTrue(control.isSelected());
            mouse(control, MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY, 20, 16);
            mouse(control, MouseEvent.MOUSE_RELEASED, MouseButton.SECONDARY, 20, 16);
            assertTrue(control.isSelected());
            mouse(control, MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, 20, 16);
            control.setDisable(true);
            control.setDisable(false);
            mouse(control, MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, 20, 16);
            assertTrue(control.isSelected());
            return null;
        });
    }

    @Test
    void handlesSpaceAndEnterWithoutRepeatingHeldKeys() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            AtomicInteger actions = new AtomicInteger();
            control.setOnAction(event -> actions.incrementAndGet());
            key(control, KeyEvent.KEY_PRESSED, KeyCode.SPACE);
            key(control, KeyEvent.KEY_PRESSED, KeyCode.SPACE);
            assertFalse(control.isSelected());
            key(control, KeyEvent.KEY_RELEASED, KeyCode.SPACE);
            assertTrue(control.isSelected());
            assertEquals(1, actions.get());
            key(control, KeyEvent.KEY_RELEASED, KeyCode.SPACE);
            assertEquals(1, actions.get());
            key(control, KeyEvent.KEY_PRESSED, KeyCode.ENTER);
            key(control, KeyEvent.KEY_RELEASED, KeyCode.ENTER);
            assertFalse(control.isSelected());
            key(control, KeyEvent.KEY_PRESSED, KeyCode.A);
            key(control, KeyEvent.KEY_RELEASED, KeyCode.A);
            assertEquals(2, actions.get());
            control.setDisable(true);
            key(control, KeyEvent.KEY_PRESSED, KeyCode.SPACE);
            key(control, KeyEvent.KEY_RELEASED, KeyCode.SPACE);
            assertEquals(2, actions.get());
            return null;
        });
    }

    @Test
    void appliesCssAndBoundColorsToRenderedShapes() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            Pane root = new Pane(control);
            new Scene(root);
            control.setStyle("-fx-switch-color: #ff0000; -fx-switch-track-color: #0000ff; "
                + "-fx-switch-thumb-color: #ffff00; -fx-switch-unselected-thumb-color: #008000;");
            root.applyCss();
            control.resize(52, 32);
            control.layout();
            Rectangle track = (Rectangle) control.lookup(".switch-track");
            Circle thumb = (Circle) control.lookup(".switch-thumb");
            assertEquals(Color.BLUE, track.getFill());
            assertEquals(Color.GREEN, thumb.getFill());
            control.setSelected(true);
            control.layout();
            assertEquals(Color.RED, track.getFill());
            assertEquals(Color.YELLOW, thumb.getFill());

            SimpleObjectProperty<Paint> color = new SimpleObjectProperty<>(Color.PURPLE);
            control.colorProperty().bind(color);
            root.applyCss();
            control.layout();
            assertEquals(Color.PURPLE, track.getFill());
            color.set(Color.ORANGE);
            control.layout();
            assertEquals(Color.ORANGE, track.getFill());
            assertTrue(control.getCssMetaData().containsAll(MaterialSwitch.getClassCssMetaData()));
            return null;
        });
    }

    @Test
    void scalesAndCentersGeometryWithinInsetsIncludingZeroSize() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            control.setPadding(new Insets(4));
            assertEquals(60, control.prefWidth(-1));
            assertEquals(40, control.prefHeight(-1));
            control.resize(112, 72);
            control.layout();
            Rectangle track = (Rectangle) control.lookup(".switch-track");
            Circle thumb = (Circle) control.lookup(".switch-thumb");
            assertEquals(4, track.getX());
            assertEquals(4, track.getY());
            assertEquals(104, track.getWidth());
            assertEquals(64, track.getHeight());
            assertEquals(36, thumb.getCenterX());
            assertEquals(16, thumb.getRadius());
            control.setSelected(true);
            control.layout();
            assertEquals(76, thumb.getCenterX());
            assertEquals(24, thumb.getRadius());

            control.resize(200, 40);
            control.layout();
            assertEquals(74, track.getX());
            assertEquals(52, track.getWidth());
            for (double size : new double[]{9, 1, 0}) {
                control.resize(size, size);
                control.layout();
                assertTrue(Double.isFinite(thumb.getCenterX()));
                assertTrue(Double.isFinite(thumb.getCenterY()));
                assertTrue(thumb.getRadius() >= 0);
                assertTrue(track.getWidth() >= 0);
                assertTrue(track.getHeight() >= 0);
            }
            return null;
        });
    }

    @Test
    void mirrorsThumbPositionInRightToLeftLayouts() throws Exception {
        onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            Pane root = new Pane(control);
            root.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            new Scene(root);
            control.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            control.resize(52, 32);
            control.layout();
            Circle thumb = (Circle) control.lookup(".switch-thumb");
            assertEquals(36, thumb.localToScene(thumb.getCenterX(), thumb.getCenterY()).getX(), 0.001);
            control.setSelected(true);
            control.layout();
            assertEquals(16, thumb.localToScene(thumb.getCenterX(), thumb.getCenterY()).getX(), 0.001);
            return null;
        });
    }

    @Test
    void loadsFromFxmlAndExposesAccessibleSelection() throws Exception {
        onFx(() -> {
            String fxml = """
                <?import org.a8043.cwaFX.components.MaterialSwitch?>
                <?import javafx.scene.control.Tooltip?>
                <MaterialSwitch xmlns:fx="http://javafx.com/fxml/1" text="Auto save" selected="true" animated="false">
                    <tooltip>
                        <Tooltip text="Save changes automatically"/>
                    </tooltip>
                </MaterialSwitch>
                """;
            MaterialSwitch control = new FXMLLoader().load(
                new ByteArrayInputStream(fxml.getBytes(StandardCharsets.UTF_8)));
            assertTrue(control.isSelected());
            assertEquals("Auto save", control.getText());
            assertEquals("Save changes automatically", control.getTooltip().getText());
            assertFalse(control.isAnimated());
            assertEquals(true, control.queryAccessibleAttribute(AccessibleAttribute.SELECTED));
            control.executeAccessibleAction(AccessibleAction.FIRE);
            assertEquals(false, control.queryAccessibleAttribute(AccessibleAttribute.SELECTED));
            control.setDisable(true);
            control.executeAccessibleAction(AccessibleAction.FIRE);
            assertFalse(control.isSelected());
            return null;
        });
    }

    @Test
    void completesAnimationsAndSettlesWhenHiddenDetachedOrDisabled() throws Exception {
        Stage stage = onFx(() -> {
            MaterialSwitch control = new MaterialSwitch();
            control.setManaged(false);
            control.resize(52, 32);
            Stage window = new Stage();
            window.setScene(new Scene(new Pane(control), 100, 60));
            window.setX(-10000);
            window.setY(-10000);
            window.show();
            control.layout();
            control.setSelected(true);
            return window;
        });
        try {
            CompletableFuture<Void> elapsed = new CompletableFuture<>();
            onFx(() -> {
                PauseTransition delay = new PauseTransition(Duration.millis(350));
                delay.setOnFinished(event -> elapsed.complete(null));
                delay.play();
                return null;
            });
            elapsed.get(10, TimeUnit.SECONDS);
            onFx(() -> {
                Pane root = (Pane) stage.getScene().getRoot();
                MaterialSwitch control = (MaterialSwitch) root.getChildren().getFirst();
                Circle thumb = (Circle) control.lookup(".switch-thumb");
                control.layout();
                assertEquals(36, thumb.getCenterX(), 0.001);
                control.setSelected(false);
                control.setVisible(false);
                control.layout();
                assertEquals(16, thumb.getCenterX(), 0.001);
                control.setVisible(true);
                control.setSelected(true);
                control.setAnimated(false);
                control.layout();
                assertEquals(36, thumb.getCenterX(), 0.001);
                control.setAnimated(true);
                control.setSelected(false);
                control.setDisable(true);
                control.layout();
                assertEquals(16, thumb.getCenterX(), 0.001);
                assertEquals(0.38, thumb.getOpacity(), 0.001);
                control.setDisable(false);
                control.setSelected(true);
                root.getChildren().clear();
                control.layout();
                assertEquals(36, thumb.getCenterX(), 0.001);
                return null;
            });
        } finally {
            onFx(() -> {
                stage.close();
                return null;
            });
        }
    }

    private static void mouse(MaterialSwitch control, EventType<MouseEvent> type,
                              MouseButton button, double x, double y) {
        control.fireEvent(new MouseEvent(type, x, y, x, y, button, 1,
            false, false, false, false, type == MouseEvent.MOUSE_PRESSED && button == MouseButton.PRIMARY,
            false, false, false, false, true, new PickResult(control, x, y)));
    }

    private static void key(MaterialSwitch control, EventType<KeyEvent> type, KeyCode code) {
        control.fireEvent(new KeyEvent(type, "", "", code, false, false, false, false));
    }

    private static <T> T onFx(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) return callable.call();
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
