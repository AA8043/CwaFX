package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a Bean implementation class to be collected into service lists for its interfaces.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Service {
}
