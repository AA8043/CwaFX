package org.a8043.cwaFX.annotations.settings;

import cn.hutool.json.JSONObject;

import java.lang.annotation.*;

/**
 * Marks a method as a deserializer for a settings class.
 * The annotated method must accept a {@link JSONObject} as a parameter and return void.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Deserializer {
}
