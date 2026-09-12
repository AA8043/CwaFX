package org.a8043.cwaFX.components.form;

/**
 * A validation rule applied to a non-empty form value.
 */
public interface Requirement {
    ValidationResult validate(Object value);

    default boolean supports(Class<?> valueType) {
        return true;
    }
}
