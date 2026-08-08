package org.a8043.cwaFX.components.form;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TextLengthRequirement implements Requirement {
    private final IntegerProperty minLength = new SimpleIntegerProperty(this, "minLength", 0);
    private final IntegerProperty maxLength = new SimpleIntegerProperty(this, "maxLength", Integer.MAX_VALUE);
    private final StringProperty messageKey = new SimpleStringProperty(this, "messageKey", "");

    @Override
    public ValidationResult validate(Object value) {
        if (!(value instanceof String text)) {
            throw new IllegalArgumentException("TextLengthRequirement requires a String value");
        }
        if (minLength.get() < 0 || maxLength.get() < minLength.get()) {
            throw new IllegalStateException("Invalid text length range");
        }
        int length = text.length();
        if (length >= minLength.get() && length <= maxLength.get()) {
            return ValidationResult.success();
        }
        return ValidationResult.invalid(resolveMessageKey(), minLength.get(), maxLength.get());
    }

    @Override
    public boolean supports(ItemType itemType) {
        return itemType == ItemType.TEXT || itemType == ItemType.PASSWORD || itemType == ItemType.TEXT_AREA;
    }

    private String resolveMessageKey() {
        if (!messageKey.get().isBlank()) {
            return messageKey.get();
        }
        if (minLength.get() == 0) {
            return "form.validation.text-length.max";
        }
        if (maxLength.get() == Integer.MAX_VALUE) {
            return "form.validation.text-length.min";
        }
        return "form.validation.text-length";
    }

    public IntegerProperty minLengthProperty() {
        return minLength;
    }

    public int getMinLength() {
        return minLength.get();
    }

    public void setMinLength(int minLength) {
        this.minLength.set(minLength);
    }

    public IntegerProperty maxLengthProperty() {
        return maxLength;
    }

    public int getMaxLength() {
        return maxLength.get();
    }

    public void setMaxLength(int maxLength) {
        this.maxLength.set(maxLength);
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
