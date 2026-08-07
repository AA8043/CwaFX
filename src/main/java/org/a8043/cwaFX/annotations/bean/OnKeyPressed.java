package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks the method to be executed when the specified key is pressed.
 * The annotated method must be parameterless.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnKeyPressed {
    /**
     * Specifies the key mapping name that triggers the annotated method.
     *
     * @return the key mapping name
     */
    String value();
}
