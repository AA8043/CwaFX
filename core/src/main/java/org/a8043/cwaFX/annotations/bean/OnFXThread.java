package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a method to be executed on the JavaFX Application Thread.
 * This annotation can be applied to framework-invoked method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnFXThread {
}
