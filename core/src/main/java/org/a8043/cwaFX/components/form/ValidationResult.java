package org.a8043.cwaFX.components.form;

import lombok.Value;

import java.util.Objects;

@Value
public class ValidationResult {
    boolean valid;
    String messageKey;
    Object[] arguments;

    public ValidationResult(boolean valid, String messageKey, Object[] arguments) {
        if (!valid) {
            Objects.requireNonNull(messageKey, "messageKey must not be null for a failed validation");
        }
        arguments = arguments == null ? new Object[0] : arguments.clone();
        this.valid = valid;
        this.messageKey = messageKey;
        this.arguments = arguments;
    }

    public static ValidationResult success() {
        return new ValidationResult(true, null, new Object[0]);
    }

    public static ValidationResult invalid(String messageKey, Object... arguments) {
        return new ValidationResult(false, messageKey, arguments);
    }
}
