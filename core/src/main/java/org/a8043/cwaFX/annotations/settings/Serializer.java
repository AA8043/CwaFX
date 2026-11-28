package org.a8043.cwaFX.annotations.settings;

import cn.hutool.json.JSONObject;

import java.lang.annotation.*;

/**
 * Marks a method as a serializer for a settings class.
 * The annotated method must be parameterless and return a {@link JSONObject}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Serializer {
}
