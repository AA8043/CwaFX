package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a method to be executed after the object is created and all dependencies are injected.
 * The annotated method must be parameterless.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Initialize {
}
