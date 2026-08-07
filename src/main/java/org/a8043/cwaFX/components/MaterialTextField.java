package org.a8043.cwaFX.components;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;
import javafx.util.Duration;
import lombok.Getter;

public class MaterialTextField extends Region {
    private static final PseudoClass PSEUDO_FOCUSED = PseudoClass.getPseudoClass("mtf-focused");
    private static final PseudoClass PSEUDO_FILLED = PseudoClass.getPseudoClass("mtf-filled");
    private static final PseudoClass PSEUDO_ERROR = PseudoClass.getPseudoClass("mtf-error");

    private static final double LABEL_SCALE = 0.75;
    private static final double LINE_HEIGHT = 2;
    private static final double LABEL_GAP = 4;
    private static final double HELPER_GAP = 4;
    private static final Duration ANIM_DURATION = Duration.millis(150);

    @Getter
    private final TextField textField = new TextField();
    private final Label floatingLabel = new Label();
    private final Label helperLabel = new Label();
    private final Region baseLine = new Region();
    private final Region focusLine = new Region();

    private final Scale labelScale = new Scale(1, 1);

    @Getter
    private final BooleanProperty error = new SimpleBooleanProperty(false);

    private double floatTranslateY = 0;
    private boolean floating = false;
    private boolean animating = false;

    public MaterialTextField() {
        this("");
    }

    public MaterialTextField(String labelText) {
        getStyleClass().add("material-text-field");
        textField.getStyleClass().add("mtf-input");
        floatingLabel.getStyleClass().add("mtf-label");
        helperLabel.getStyleClass().add("mtf-helper");
        baseLine.getStyleClass().add("mtf-base-line");
        focusLine.getStyleClass().add("mtf-focus-line");

        floatingLabel.setText(labelText);
        floatingLabel.setMouseTransparent(true);
        floatingLabel.getTransforms().add(labelScale);

        focusLine.setScaleX(0);

        getChildren().addAll(baseLine, focusLine, textField, floatingLabel, helperLabel);

        textField.focusedProperty().addListener((o, was, is) -> onFocusChanged(is));
        textField.textProperty().addListener((o, ol, ne) -> refreshFilled());
        error.addListener((o, ol, ne) -> pseudoClassStateChanged(PSEUDO_ERROR, ne));

        setOnMousePressed(e -> textField.requestFocus());

        refreshFilled();
    }

    private void onFocusChanged(boolean focused) {
        pseudoClassStateChanged(PSEUDO_FOCUSED, focused);
        animateFocusLine(focused);
        updateFloatingState();
    }

    private void refreshFilled() {
        boolean filled = textField.getText() != null && !textField.getText().isEmpty();
        pseudoClassStateChanged(PSEUDO_FILLED, filled);
        updateFloatingState();
    }

    private void updateFloatingState() {
        boolean shouldFloat = textField.isFocused()
                              || (textField.getText() != null && !textField.getText().isEmpty());
        if (shouldFloat != floating) {
            floating = shouldFloat;
            animateLabel(floating);
        }
    }

    private void animateLabel(boolean toFloat) {
        animating = true;
        double targetScale = toFloat ? LABEL_SCALE : 1.0;
        double targetY = toFloat ? floatTranslateY : 0;

        Timeline tl = new Timeline(new KeyFrame(ANIM_DURATION,
            new KeyValue(labelScale.xProperty(), targetScale, Interpolator.EASE_BOTH),
            new KeyValue(labelScale.yProperty(), targetScale, Interpolator.EASE_BOTH),
            new KeyValue(floatingLabel.translateYProperty(), targetY, Interpolator.EASE_BOTH)
        ));
        tl.setOnFinished(e -> animating = false);
        tl.play();
    }

    private void animateFocusLine(boolean focused) {
        Timeline tl = new Timeline(new KeyFrame(ANIM_DURATION,
            new KeyValue(focusLine.scaleXProperty(), focused ? 1 : 0, Interpolator.EASE_BOTH)
        ));
        tl.play();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        Insets pad = getInsets();

        double left = pad.getLeft();
        double top = pad.getTop();
        double bottom = pad.getBottom();
        double contentW = w - left - pad.getRight();

        double helperH = helperLabel.prefHeight(contentW);
        double fieldH = textField.prefHeight(contentW);

        double helperY = h - bottom - helperH;
        double lineY = helperY - LINE_HEIGHT - HELPER_GAP;
        double fieldY = lineY - fieldH;

        textField.resizeRelocate(left, fieldY, contentW, fieldH);
        baseLine.resizeRelocate(left, lineY, contentW, LINE_HEIGHT);
        focusLine.resizeRelocate(left, lineY, contentW, LINE_HEIGHT);
        helperLabel.resizeRelocate(left, helperY, contentW, helperH);

        double labelW = floatingLabel.prefWidth(-1);
        double labelH = floatingLabel.prefHeight(labelW);
        double labelBaseY = fieldY + (fieldH - labelH) / 2;
        double labelX = left + textField.getInsets().getLeft();
        floatingLabel.resizeRelocate(labelX, labelBaseY, labelW, labelH);

        floatTranslateY = top - labelBaseY;

        if (!animating) {
            double s = floating ? LABEL_SCALE : 1.0;
            labelScale.setX(s);
            labelScale.setY(s);
            floatingLabel.setTranslateY(floating ? floatTranslateY : 0);
        }
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets pad = getInsets();
        double fieldH = textField.prefHeight(width);
        double helperH = helperLabel.prefHeight(width);
        double floatArea = floatingLabel.prefHeight(-1) * LABEL_SCALE + LABEL_GAP;
        return pad.getTop() + floatArea + fieldH + LINE_HEIGHT + HELPER_GAP + helperH + pad.getBottom();
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
        return textField.textProperty();
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
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
