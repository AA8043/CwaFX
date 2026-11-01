package org.a8043.cwaFX.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.css.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.List;

public final class MaterialSwitch extends Region {
    private static final double DEFAULT_WIDTH = 52;
    private static final double DEFAULT_HEIGHT = 32;
    private static final double LABEL_GAP = 12;
    private static final Paint DEFAULT_COLOR = Color.web("#0071ff");
    private static final Paint DEFAULT_TRACK_COLOR = Color.web("#e0e3e7");
    private static final Paint DEFAULT_THUMB_COLOR = Color.WHITE;
    private static final Paint DEFAULT_UNSELECTED_THUMB_COLOR = Color.web("#74777f");
    private static final Duration ANIMATION_DURATION = Duration.millis(200);
    private static final Interpolator MATERIAL_EASE = Interpolator.SPLINE(0.4, 0.0, 0.2, 1.0);
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    private static final StyleablePropertyFactory<MaterialSwitch> CSS_FACTORY =
        new StyleablePropertyFactory<>(Region.getClassCssMetaData());

    private static final CssMetaData<MaterialSwitch, Paint> COLOR_META = CSS_FACTORY.createPaintCssMetaData(
        "-fx-switch-color", control -> control.color, DEFAULT_COLOR, false);
    private static final CssMetaData<MaterialSwitch, Paint> TRACK_COLOR_META = CSS_FACTORY.createPaintCssMetaData(
        "-fx-switch-track-color", control -> control.trackColor, DEFAULT_TRACK_COLOR, false);
    private static final CssMetaData<MaterialSwitch, Paint> THUMB_COLOR_META = CSS_FACTORY.createPaintCssMetaData(
        "-fx-switch-thumb-color", control -> control.thumbColor, DEFAULT_THUMB_COLOR, false);
    private static final CssMetaData<MaterialSwitch, Paint> UNSELECTED_THUMB_COLOR_META = CSS_FACTORY.createPaintCssMetaData(
        "-fx-switch-unselected-thumb-color", control -> control.unselectedThumbColor,
        DEFAULT_UNSELECTED_THUMB_COLOR, false);

    private final StyleableObjectProperty<Paint> color = new SimpleStyleableObjectProperty<>(
        COLOR_META, this, "color", DEFAULT_COLOR);
    private final StyleableObjectProperty<Paint> trackColor = new SimpleStyleableObjectProperty<>(
        TRACK_COLOR_META, this, "trackColor", DEFAULT_TRACK_COLOR);
    private final StyleableObjectProperty<Paint> thumbColor = new SimpleStyleableObjectProperty<>(
        THUMB_COLOR_META, this, "thumbColor", DEFAULT_THUMB_COLOR);
    private final StyleableObjectProperty<Paint> unselectedThumbColor = new SimpleStyleableObjectProperty<>(
        UNSELECTED_THUMB_COLOR_META, this, "unselectedThumbColor", DEFAULT_UNSELECTED_THUMB_COLOR);

    private final Rectangle track = new Rectangle();
    private final Circle stateLayer = new Circle();
    private final Circle thumb = new Circle();
    private final Label label = new Label();
    private final Timeline animation = new Timeline();
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<Tooltip> tooltip = new SimpleObjectProperty<>(this, "tooltip");
    private final DoubleProperty progress = new SimpleDoubleProperty(this, "progress", 0);
    private final BooleanProperty selected = new SimpleBooleanProperty(this, "selected", false);
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", true);
    private final ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<>() {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return MaterialSwitch.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };

    private Window observedWindow;
    private final ChangeListener<Boolean> showingListener = (observable, oldValue, newValue) -> finishAnimation();
    private final ChangeListener<Window> windowListener = (observable, oldValue, newValue) -> observeWindow(newValue);
    private boolean mouseArmed;
    private KeyCode armedKey;
    private double preferredSwitchHeight = DEFAULT_HEIGHT;

    public MaterialSwitch() {
        this(false);
    }

    public MaterialSwitch(boolean selected) {
        getStyleClass().add("material-switch");
        setMinSize(0, 0);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setFocusTraversable(true);
        setCursor(Cursor.HAND);
        setPickOnBounds(true);
        setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);

        track.getStyleClass().add("switch-track");
        stateLayer.getStyleClass().add("switch-state-layer");
        thumb.getStyleClass().add("switch-thumb");
        label.getStyleClass().add("switch-label");
        label.setManaged(false);
        label.setMouseTransparent(true);
        label.setVisible(false);
        label.textProperty().bind(text);
        label.fontProperty().addListener(observable -> requestLayout());
        track.setStrokeType(StrokeType.INSIDE);
        for (var shape : List.of(track, stateLayer, thumb)) {
            shape.setManaged(false);
            shape.setMouseTransparent(true);
        }
        getChildren().addAll(track, stateLayer, thumb, label);

        text.addListener(observable -> {
            requestLayout();
            notifyAccessibleAttributeChanged(AccessibleAttribute.TEXT);
        });
        tooltip.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) Tooltip.uninstall(this, oldValue);
            if (newValue != null) Tooltip.install(this, newValue);
        });

        progress.addListener(observable -> requestLayout());
        for (var property : List.of(color, trackColor, thumbColor, unselectedThumbColor)) {
            property.addListener(observable -> requestLayout());
        }
        this.selected.addListener((observable, oldValue, newValue) -> {
            pseudoClassStateChanged(SELECTED, newValue);
            notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
            updateSelection();
        });
        animated.addListener((observable, oldValue, newValue) -> {
            if (!newValue) finishAnimation();
        });
        visibleProperty().addListener((observable, oldValue, newValue) -> finishAnimation());
        disabledProperty().addListener((observable, oldValue, newValue) -> {
            mouseArmed = false;
            armedKey = null;
            finishAnimation();
            track.setOpacity(newValue ? 0.38 : 1);
            thumb.setOpacity(newValue ? 0.38 : 1);
        });
        hoverProperty().addListener(observable -> requestLayout());
        pressedProperty().addListener(observable -> requestLayout());
        focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) armedKey = null;
            requestLayout();
        });
        sceneProperty().addListener((observable, oldScene, newScene) -> observeScene(oldScene, newScene));

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && !isDisabled()) {
                mouseArmed = true;
                requestFocus();
                event.consume();
            }
        });
        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && mouseArmed) {
                mouseArmed = false;
                if (contains(event.getX(), event.getY())) fire();
                event.consume();
            }
        });
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (isActivationKey(event.getCode()) && !isDisabled()) {
                if (armedKey == null) {
                    armedKey = event.getCode();
                    requestLayout();
                }
                event.consume();
            }
        });
        addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (isActivationKey(event.getCode()) && armedKey == event.getCode()) {
                armedKey = null;
                requestLayout();
                fire();
                event.consume();
            }
        });

        setSelected(selected);
    }

    public MaterialSwitch(double size) {
        this();
        preferredSwitchHeight = size;
    }

    public MaterialSwitch(String text) {
        this();
        setText(text);
    }

    private static boolean isActivationKey(KeyCode code) {
        return code == KeyCode.SPACE || code == KeyCode.ENTER;
    }

    private void updateSelection() {
        double start = progress.get();
        animation.stop();
        double target = isSelected() ? 1 : 0;
        if (isAnimated() && !isDisabled() && isVisible() && observedWindow != null && observedWindow.isShowing()) {
            animation.getKeyFrames().setAll(
                new KeyFrame(Duration.ZERO, new KeyValue(progress, start)),
                new KeyFrame(ANIMATION_DURATION, new KeyValue(progress, target, MATERIAL_EASE)));
            animation.playFromStart();
        } else {
            progress.set(target);
        }
    }

    private void finishAnimation() {
        animation.stop();
        progress.set(isSelected() ? 1 : 0);
        requestLayout();
    }

    private void observeScene(Scene oldScene, Scene newScene) {
        if (oldScene != null) oldScene.windowProperty().removeListener(windowListener);
        if (newScene != null) newScene.windowProperty().addListener(windowListener);
        observeWindow(newScene == null ? null : newScene.getWindow());
    }

    private void observeWindow(Window window) {
        if (observedWindow != null) observedWindow.showingProperty().removeListener(showingListener);
        observedWindow = window;
        if (observedWindow != null) observedWindow.showingProperty().addListener(showingListener);
        finishAnimation();
    }

    @Override
    protected void layoutChildren() {
        double left = getInsets().getLeft();
        double top = getInsets().getTop();
        double width = Math.max(0, getWidth() - left - getInsets().getRight());
        double height = Math.max(0, getHeight() - top - getInsets().getBottom());
        double labelWidth = hasText() ? Math.min(label.prefWidth(-1), Math.max(0,
            width - preferredSwitchHeight * DEFAULT_WIDTH / DEFAULT_HEIGHT - LABEL_GAP)) : 0;
        double gap = labelWidth > 0 ? LABEL_GAP : 0;
        double scale = Math.min(Math.max(0, width - labelWidth - gap) / DEFAULT_WIDTH, height / DEFAULT_HEIGHT);
        double trackWidth = DEFAULT_WIDTH * scale;
        double trackHeight = DEFAULT_HEIGHT * scale;
        double x = left + (width - trackWidth - gap - labelWidth) / 2;
        double y = top + (height - trackHeight) / 2;
        double fraction = progress.get();

        double labelHeight = Math.min(height, label.prefHeight(labelWidth));
        label.setVisible(labelWidth > 0);
        label.resizeRelocate(x + trackWidth + gap, top + (height - labelHeight) / 2, labelWidth, labelHeight);

        track.setX(x);
        track.setY(y);
        track.setWidth(trackWidth);
        track.setHeight(trackHeight);
        track.setArcWidth(trackHeight);
        track.setArcHeight(trackHeight);
        track.setStrokeWidth(2 * scale);
        track.setFill(interpolate(getTrackColor(), getColor(), fraction));
        track.setStroke(interpolate(getUnselectedThumbColor(), getColor(), fraction));

        thumb.setCenterX(x + (16 + 20 * fraction) * scale);
        thumb.setCenterY(y + trackHeight / 2);
        thumb.setRadius((8 + 4 * fraction) * scale);
        thumb.setFill(interpolate(getUnselectedThumbColor(), getThumbColor(), fraction));

        stateLayer.setCenterX(thumb.getCenterX());
        stateLayer.setCenterY(thumb.getCenterY());
        stateLayer.setRadius(thumb.getRadius() + 4 * scale);
        stateLayer.setFill(thumb.getFill());
        stateLayer.setOpacity(isDisabled() ? 0 : isFocused() || isPressed() || armedKey != null ? 0.12 : isHover() ? 0.08 : 0);
    }

    private static Paint interpolate(Paint from, Paint to, double fraction) {
        if (from instanceof Color start && to instanceof Color end) {
            return start.interpolate(end, fraction);
        }
        return fraction < 0.5 ? from : to;
    }

    @Override
    protected double computePrefWidth(double height) {
        double labelWidth = hasText() ? LABEL_GAP + label.prefWidth(-1) : 0;
        return getInsets().getLeft() + preferredSwitchHeight * DEFAULT_WIDTH / DEFAULT_HEIGHT
            + labelWidth + getInsets().getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        double contentHeight = hasText() ? Math.max(preferredSwitchHeight, label.prefHeight(-1))
            : preferredSwitchHeight;
        return getInsets().getTop() + contentHeight + getInsets().getBottom();
    }

    private boolean hasText() {
        return getText() != null && !getText().isEmpty();
    }

    public void fire() {
        if (!isDisabled() && !selected.isBound()) {
            setSelected(!isSelected());
            fireEvent(new ActionEvent(this, this));
        }
    }

    @Override
    public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
        if (attribute == AccessibleAttribute.TEXT && getAccessibleText() == null && hasText()) {
            return getText();
        }
        return attribute == AccessibleAttribute.SELECTED ? isSelected()
            : super.queryAccessibleAttribute(attribute, parameters);
    }

    @Override
    public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
        if (action == AccessibleAction.FIRE) fire();
        else super.executeAccessibleAction(action, parameters);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_FACTORY.getCssMetaData();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public StringProperty textProperty() {
        return text;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String value) {
        text.set(value == null ? "" : value);
    }

    public ObjectProperty<Tooltip> tooltipProperty() {
        return tooltip;
    }

    public Tooltip getTooltip() {
        return tooltip.get();
    }

    public void setTooltip(Tooltip value) {
        tooltip.set(value);
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public BooleanProperty animatedProperty() {
        return animated;
    }

    public boolean isAnimated() {
        return animated.get();
    }

    public void setAnimated(boolean value) {
        animated.set(value);
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

    public ObjectProperty<Paint> trackColorProperty() {
        return trackColor;
    }

    public Paint getTrackColor() {
        return trackColor.get();
    }

    public void setTrackColor(Paint value) {
        trackColor.set(value);
    }

    public ObjectProperty<Paint> thumbColorProperty() {
        return thumbColor;
    }

    public Paint getThumbColor() {
        return thumbColor.get();
    }

    public void setThumbColor(Paint value) {
        thumbColor.set(value);
    }

    public ObjectProperty<Paint> unselectedThumbColorProperty() {
        return unselectedThumbColor;
    }

    public Paint getUnselectedThumbColor() {
        return unselectedThumbColor.get();
    }

    public void setUnselectedThumbColor(Paint value) {
        unselectedThumbColor.set(value);
    }

    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    public EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    public void setOnAction(EventHandler<ActionEvent> value) {
        onAction.set(value);
    }
}
