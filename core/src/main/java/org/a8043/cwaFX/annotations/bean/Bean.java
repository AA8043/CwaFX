package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a class as a Bean.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Bean {
    /**
     * Whether this Bean is a singleton. Defaults to {@code true}.
     * If set to {@code false}, different instances will be created when different names are used.
     *
     * @return {@code true} if the Bean is a singleton, {@code false} otherwise
     */
    boolean single() default true;
}