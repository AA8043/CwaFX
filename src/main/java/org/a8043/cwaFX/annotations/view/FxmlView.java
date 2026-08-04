package org.a8043.cwaFX.annotations.view;

import java.lang.annotation.*;

/**
 * Marks a class as an FXML view.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FxmlView {
    /**
     * Specifies the name of the view bean (a {@link javafx.scene.Node}).
     *
     * @return the name of the view bean
     */
    String value() default "";

    /**
     * Specifies the path to the FXML file for the view.
     * @return the path to the FXML file
     */
    String fxml() default "";
}
