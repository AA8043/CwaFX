package org.a8043.cwaFX.components.form;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class NumberRangeRequirement implements Requirement {
    private final DoubleProperty min = new SimpleDoubleProperty(this, "min", Double.NEGATIVE_INFINITY);
    private final DoubleProperty max = new SimpleDoubleProperty(this, "max", Double.POSITIVE_INFINITY);
    private final StringProperty messageKey = new SimpleStringProperty(this, "messageKey", "");

    @Override
    public ValidationResult validate(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("NumberRangeRequirement requires a Number value");
        }
        if (Double.isNaN(min.get()) || Double.isNaN(max.get()) || min.get() > max.get()) {
            throw new IllegalStateException("Invalid number range");
        }
        double numberValue = number.doubleValue();
        if (numberValue >= min.get() && numberValue <= max.get()) {
            return ValidationResult.success();
        }
        return ValidationResult.invalid(resolveMessageKey(), min.get(), max.get());
    }

    @Override
    public boolean supports(ItemType itemType) {
        return itemType == ItemType.NUMBER;
    }

    private String resolveMessageKey() {
        if (!messageKey.get().isBlank()) {
            return messageKey.get();
        }
        if (Double.isInfinite(min.get())) {
            return "form.validation.number-range.max";
        }
        if (Double.isInfinite(max.get())) {
            return "form.validation.number-range.min";
        }
        return "form.validation.number-range";
    }

    public DoubleProperty minProperty() {
        return min;
    }

    public double getMin() {
        return min.get();
    }

    public void setMin(double min) {
        this.min.set(min);
    }

    public DoubleProperty maxProperty() {
        return max;
    }

    public double getMax() {
        return max.get();
    }

    public void setMax(double max) {
        this.max.set(max);
    }

    public StringProperty messageKeyProperty() {
        return messageKey;
    }

    public String getMessageKey() {
        return messageKey.get();
    }

    public void setMessageKey(String messageKey) {
        this.messageKey.set(messageKey == null ? "" : messageKey);
    }
}
