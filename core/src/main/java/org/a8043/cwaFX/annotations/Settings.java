package org.a8043.cwaFX.annotations;

import java.lang.annotation.*;

/**
 * Marks a class as a settings class.
 * The bean must be singleton.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Settings {
    /**
     * Specifies the path to the settings file.
     *
     * @return the path to the settings file
     */
    String value();
}
