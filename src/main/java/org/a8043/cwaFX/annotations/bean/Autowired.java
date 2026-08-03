package org.a8043.cwaFX.annotations.bean;

import java.lang.annotation.*;

/**
 * Marks a field as an automatically injected Bean.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
    /**
     * Specifies the name of the Bean to inject.
     * If not specified, the Bean will be matched automatically based on the field type.
     *
     * @return the name of the Bean to inject
     */
    String name() default "";
}