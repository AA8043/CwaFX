package org.a8043.cwaFX.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import lombok.Getter;

public class MaterialTextField extends Region {
    public enum Type {
        NORMAL,
        PASSWORD
    }

    private static final PseudoClass PSEUDO_FOCUSED = PseudoClass.getPseudoClass("mtf-focused");
    private static final PseudoClass PSEUDO_FILLED = PseudoClass.getPseudoClass("mtf-filled");
    private static final PseudoClass PSEUDO_ERROR = PseudoClass.getPseudoClass("mtf-error");

    private static final double LABEL_SCALE = 0.75;
    private static final double FIELD_PADDING = 4;
    private static final double LABEL_GAP = 4;
    private static final double HELPER_GAP = 4;
    private static final Duration ANIM_DURATION = Duration.millis(150);

    @Getter
    private TextField textField;
    private final Label floatingLabel = new Label();
    private final Label helperLabel = new Label();
    private final Region outline = new Region();
    private final Region focusOutline = new Region();

    private final Scale labelScale = new Scale(1, 1);
    private final DoubleProperty floatProgress = new SimpleDoubleProperty(0);
    private Timeline labelAnimation;
    private Timeline focusAnimation;

    @Getter
    private final BooleanProperty error = new SimpleBooleanProperty(false);
    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final ObjectProperty<Type> type = new SimpleObjectProperty<>(this, "type", Type.NORMAL);

    private double floatTranslateY = 0;
    private boolean floating = false;

    public MaterialTextField() {
        this("");
    }

    public MaterialTextField(String labelText) {
        getStyleClass().add("material-text-field");
        floatingLabel.getStyleClass().add("mtf-label");
        helperLabel.getStyleClass().add("mtf-helper");
        outline.getStyleClass().add("mtf-outline");
        focusOutline.getStyleClass().add("mtf-focus-outline");

        floatingLabel.setText(labelText);
        floatingLabel.setMouseTransparent(true);
        floatingLabel.getTransforms().add(labelScale);
        floatingLabel.boundsInParentProperty().addListener((o, oldBounds, newBounds) -> updateOutlineClips());
        floatProgress.addListener((o, oldValue, newValue) -> updateLabelPosition());

        outline.setMouseTransparent(true);
        focusOutline.setMouseTransparent(true);
        focusOutline.setOpacity(0);

        replaceTextField(new TextField());
        getChildren().addAll(outline, focusOutline, textField, floatingLabel, helperLabel);

        text.addListener((o, ol, ne) -> refreshFilled());
        type.addListener((o, oldType, newType) -> replaceTextField(createTextField(newType)));
        error.addListener((o, ol, ne) -> pseudoClassStateChanged(PSEUDO_ERROR, ne));

        setOnMousePressed(e -> textField.requestFocus());

        refreshFilled();
    }

    private TextField createTextField(Type inputType) {
        return inputType == Type.PASSWORD ? new PasswordField() : new TextField();
    }

    private void replaceTextField(TextField newTextField) {
        TextField oldTextField = textField;
        boolean wasFocused = oldTextField != null && oldTextField.isFocused();
        int anchor = oldTextField == null ? 0 : oldTextField.getAnchor();
        int caretPosition = oldTextField == null ? 0 : oldTextField.getCaretPosition();

        if (oldTextField != null) {
            oldTextField.textProperty().unbindBidirectional(text);
            getChildren().remove(oldTextField);
        }

        newTextField.getStyleClass().add("mtf-input");
        newTextField.textProperty().bindBidirectional(text);
        newTextField.focusedProperty().addListener((o, was, focused) -> onFocusChanged(focused));
        textField = newTextField;

        if (getChildren().size() >= 2) {
            getChildren().add(2, newTextField);
        }

        newTextField.selectRange(Math.min(anchor, newTextField.getLength()),
            Math.min(caretPosition, newTextField.getLength()));
        if (wasFocused) {
            newTextField.requestFocus();
        }
    }

    private void onFocusChanged(boolean focused) {
        pseudoClassStateChanged(PSEUDO_FOCUSED, focused);
        animateFocusOutline(focused);
        updateFloatingState();
    }

    private void refreshFilled() {
        boolean filled = text.get() != null && !text.get().isEmpty();
        pseudoClassStateChanged(PSEUDO_FILLED, filled);
        updateFloatingState();
    }

    private void updateFloatingState() {
        boolean shouldFloat = textField.isFocused()
                              || (text.get() != null && !text.get().isEmpty());
        if (shouldFloat != floating) {
            floating = shouldFloat;
            animateLabel(floating);
        }
    }

    private void animateLabel(boolean toFloat) {
        if (labelAnimation != null) {
            labelAnimation.stop();
        }
        labelAnimation = new Timeline(new KeyFrame(ANIM_DURATION,
            new KeyValue(floatProgress, toFloat ? 1 : 0, Interpolator.EASE_BOTH)
        ));
        labelAnimation.play();
    }

    private void updateLabelPosition() {
        double scale = 1 - (1 - LABEL_SCALE) * floatProgress.get();
        labelScale.setX(scale);
        labelScale.setY(scale);
        floatingLabel.setTranslateY(floatTranslateY * floatProgress.get());
        updateOutlineClips();
    }

    private void updateOutlineClips() {
        Bounds labelBounds = floatingLabel.getBoundsInParent();
        clipOutline(outline, labelBounds);
        clipOutline(focusOutline, labelBounds);
    }

    private void clipOutline(Region border, Bounds labelBounds) {
        double labelY = labelBounds.getMinY() - border.getLayoutY();
        if (floatingLabel.getText() == null || floatingLabel.getText().isEmpty()
            || labelY > 2 || labelY + labelBounds.getHeight() < 0) {
            border.setClip(null);
            return;
        }

        double gapStart = Math.max(4, labelBounds.getMinX() - border.getLayoutX() - LABEL_GAP);
        double gapEnd = Math.min(border.getWidth() - 4,
            labelBounds.getMaxX() - border.getLayoutX() + LABEL_GAP);
        // Cut out the border itself so the label works on any background.
        border.setClip(Shape.subtract(new Rectangle(border.getWidth(), border.getHeight()),
            new Rectangle(gapStart, 0, Math.max(0, gapEnd - gapStart),
                Math.max(0, labelY + labelBounds.getHeight() + 2))));
    }

    private void animateFocusOutline(boolean focused) {
        if (focusAnimation != null) {
            focusAnimation.stop();
        }
        focusAnimation = new Timeline(new KeyFrame(ANIM_DURATION,
            new KeyValue(focusOutline.opacityProperty(), focused ? 1 : 0, Interpolator.EASE_BOTH)
        ));
        focusAnimation.play();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        Insets pad = getInsets();

        double left = pad.getLeft();
        double labelW = floatingLabel.prefWidth(-1);
        double labelH = floatingLabel.prefHeight(labelW);
        double top = pad.getTop() + labelH * LABEL_SCALE / 2;
        double bottom = pad.getBottom();
        double contentW = Math.max(0, w - left - pad.getRight());

        boolean hasHelper = helperLabel.getText() != null && !helperLabel.getText().isEmpty();
        double helperH = hasHelper ? helperLabel.prefHeight(contentW) : 0;
        double fieldH = textField.prefHeight(contentW);

        double helperY = h - bottom - helperH;
        double outlineH = Math.max(0, helperY - (hasHelper ? HELPER_GAP : 0) - top);
        double fieldY = top + (outlineH - fieldH) / 2;

        textField.resizeRelocate(left, fieldY, contentW, fieldH);
        outline.resizeRelocate(left, top, contentW, outlineH);
        focusOutline.resizeRelocate(left, top, contentW, outlineH);
        helperLabel.resizeRelocate(left, helperY, contentW, helperH);

        double labelBaseY = top + (outlineH - labelH) / 2;
        double labelX = left + textField.getInsets().getLeft();
        floatingLabel.resizeRelocate(labelX, labelBaseY,
            Math.max(0, Math.min(labelW, contentW - 2 * textField.getInsets().getLeft())), labelH);

        floatTranslateY = top - labelH * LABEL_SCALE / 2 - labelBaseY;
        updateLabelPosition();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets pad = getInsets();
        double contentW = width < 0 ? -1 : Math.max(0, width - pad.getLeft() - pad.getRight());
        double fieldH = textField.prefHeight(contentW);
        boolean hasHelper = helperLabel.getText() != null && !helperLabel.getText().isEmpty();
        double helperH = hasHelper ? HELPER_GAP + helperLabel.prefHeight(contentW) : 0;
        double floatArea = floatingLabel.prefHeight(-1) * LABEL_SCALE / 2;
        return pad.getTop() + FIELD_PADDING * 2 + floatArea + fieldH + helperH + pad.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets pad = getInsets();
        return pad.getLeft() + Math.max(200, textField.prefWidth(-1)) + pad.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        return computePrefHeight(width);
    }

    public StringProperty textProperty() {
        return text;
    }

    public String getText() {
        return text.get();
    }

    public void setText(String text) {
        this.text.set(text);
    }

    public int getCaretPosition() {
        return textField == null ? 0 : textField.getCaretPosition();
    }

    public int getAnchor() {
        return textField == null ? 0 : textField.getAnchor();
    }

    public boolean isInputFocused() {
        return textField != null && textField.isFocused();
    }

    public void selectInputRange(int anchor, int caretPosition) {
        if (textField != null) {
            textField.selectRange(Math.min(anchor, textField.getLength()),
                Math.min(caretPosition, textField.getLength()));
        }
    }

    public void restoreInputSelection(int anchor, int caretPosition, boolean focused) {
        if (textField == null) return;
        textField.selectRange(Math.min(anchor, textField.getLength()),
            Math.min(caretPosition, textField.getLength()));
        if (focused) textField.requestFocus();
    }

    public ObjectProperty<Type> typeProperty() {
        return type;
    }

    public Type getType() {
        return type.get();
    }

    public void setType(Type type) {
        this.type.set(type == null ? Type.NORMAL : type);
    }

    public StringProperty labelTextProperty() {
        return floatingLabel.textProperty();
    }

    public String getLabelText() {
        return floatingLabel.getText();
    }

    public void setLabelText(String text) {
        floatingLabel.setText(text);
    }

    public StringProperty helperTextProperty() {
        return helperLabel.textProperty();
    }

    public String getHelperText() {
        return helperLabel.getText();
    }

    public void setHelperText(String text) {
        helperLabel.setText(text);
    }

    public boolean isError() {
        return error.get();
    }

    public void setError(boolean value) {
        error.set(value);
    }
}
