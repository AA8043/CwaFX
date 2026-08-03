package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a method to be executed before the object is destroyed.
 * The annotated method must be parameterless.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}