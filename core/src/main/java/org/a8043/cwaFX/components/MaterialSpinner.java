package org.a8043.cwaFX.components;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.css.*;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

public final class MaterialSpinner extends Region {
    private static final double MIN_SWEEP = 10;
    private static final double SWEEP_DELTA = 260;
    private static final Interpolator MATERIAL_EASE = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
    private static final Paint DEFAULT_COLOR = Color.web("#0071ff");
    private static final double DEFAULT_STROKE_WIDTH = 3.0;

    private final Arc arc = new Arc();
    private final Transition animation;
    private final BooleanProperty running = new SimpleBooleanProperty(this, "running", true);
    private Window observedWindow;
    private final ChangeListener<Boolean> showingListener = (observable, oldValue, newValue) -> updateAnimation();
    private final ChangeListener<Window> windowListener = (observable, oldValue, newValue) -> observeWindow(newValue);

    private static final StyleablePropertyFactory<MaterialSpinner> CSS_FACTORY =
        new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    private static final CssMetaData<MaterialSpinner, Paint> COLOR_META = CSS_FACTORY.createPaintCssMetaData(
        "-fx-spinner-color", spinner -> spinner.color, DEFAULT_COLOR, false);

    private static final CssMetaData<MaterialSpinner, Number> STROKE_WIDTH_META = CSS_FACTORY.createSizeCssMetaData(
        "-fx-spinner-stroke-width", spinner -> spinner.strokeWidth, DEFAULT_STROKE_WIDTH, false);

    private final StyleableObjectProperty<Paint> color = new SimpleStyleableObjectProperty<>(
        COLOR_META, this, "color", DEFAULT_COLOR);

    private final StyleableDoubleProperty strokeWidth = new SimpleStyleableDoubleProperty(
        STROKE_WIDTH_META, this, "strokeWidth", DEFAULT_STROKE_WIDTH) {
        @Override
        protected void invalidated() {
            requestLayout();
        }
    };

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_FACTORY.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }


    public MaterialSpinner() {
        this(40);
    }

    public MaterialSpinner(double size) {
        getStyleClass().add("material-spinner");

        setMinSize(0, 0);
        setPrefSize(size, size);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMouseTransparent(true);

        arc.setManaged(false);
        arc.setType(ArcType.OPEN);
        arc.setFill(null);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setStrokeWidth(getStrokeWidth());
        arc.strokeProperty().bind(color);

        getChildren().add(arc);

        animation = new Transition() {
            {
                setCycleDuration(Duration.millis(5332));
                setCycleCount(Animation.INDEFINITE);
                setInterpolator(Interpolator.LINEAR);
            }

            @Override
            protected void interpolate(double fraction) {
                renderFrame(fraction);
            }
        };

        renderFrame(0);

        running.addListener((observable, oldValue, newValue) -> updateAnimation());
        visibleProperty().addListener((observable, oldValue, newValue) -> updateAnimation());
        sceneProperty().addListener((observable, oldScene, newScene) -> observeScene(oldScene, newScene));
    }

    private void renderFrame(double fraction) {
        double phase = fraction * 4;
        int cycle = Math.min(3, (int) phase);
        double local = phase - cycle;

        double sweep;
        double tail;

        if (local < 0.5) {
            double eased = ease(local * 2);
            sweep = MIN_SWEEP + SWEEP_DELTA * eased;
            tail = 0;
        } else {
            double eased = ease((local - 0.5) * 2);
            sweep = MIN_SWEEP + SWEEP_DELTA * (1 - eased);
            tail = SWEEP_DELTA * eased;
        }

        double rotation = 400 * fraction + SWEEP_DELTA * cycle + tail;

        arc.setStartAngle(90 - rotation);
        arc.setLength(-sweep);
    }

    private static double ease(double fraction) {
        return MATERIAL_EASE.interpolate(0.0, 1.0, fraction);
    }

    @Override
    protected void layoutChildren() {
        double left = getInsets().getLeft();
        double top = getInsets().getTop();

        double width = Math.max(
            0, getWidth() - left - getInsets().getRight());

        double height = Math.max(
            0, getHeight() - top - getInsets().getBottom());

        double diameter = Math.min(width, height);

        double requestedStroke = getStrokeWidth();

        if (!Double.isFinite(requestedStroke)) {
            requestedStroke = DEFAULT_STROKE_WIDTH;
        }

        requestedStroke = Math.max(0, requestedStroke);

        double actualStroke = Math.min(requestedStroke, diameter / 2);
        double radius = Math.max(0, (diameter - actualStroke) / 2);

        arc.setStrokeWidth(actualStroke);
        arc.setCenterX(left + width / 2);
        arc.setCenterY(top + height / 2);
        arc.setRadiusX(radius);
        arc.setRadiusY(radius);
    }


    private void observeScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) {
            oldScene.windowProperty().removeListener(windowListener);
        }

        if (newScene != null) {
            newScene.windowProperty().addListener(windowListener);
        }

        observeWindow(newScene == null ? null : newScene.getWindow());
    }

    private void observeWindow(Window window) {
        if (observedWindow != null) {
            observedWindow.showingProperty()
                .removeListener(showingListener);
        }

        observedWindow = window;

        if (observedWindow != null) {
            observedWindow.showingProperty()
                .addListener(showingListener);
        }

        updateAnimation();
    }

    private void updateAnimation() {
        boolean shouldPlay =
            running.get()
            && isVisible()
            && getScene() != null
            && observedWindow != null
            && observedWindow.isShowing();

        if (shouldPlay) {
            animation.play();
        } else {
            animation.pause();
        }
    }

    public ObjectProperty<Paint> colorProperty() {
        return color;
    }

    public Paint getColor() {
        return color.get();
    }

    public void setColor(Paint value) {
        color.set(value);
    }

    public DoubleProperty strokeWidthProperty() {
        return strokeWidth;
    }

    public double getStrokeWidth() {
        return strokeWidth.get();
    }

    public void setStrokeWidth(double value) {
        strokeWidth.set(value);
    }

    public void setRunning(boolean value) {
        running.set(value);
    }
}
