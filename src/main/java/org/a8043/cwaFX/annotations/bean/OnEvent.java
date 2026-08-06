package org.a8043.cwaFX.annotations.bean;

import org.a8043.cwaFX.userEvent.Event;

import java.lang.annotation.*;

/**
 * Marks the method to be executed when the specified event occurs.
 * The method should have a single parameter of the event type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnEvent {
    /**
     * Specifies the event class that this method should listen to.
     * @return the event class
     */
    Class<? extends Event> value();
}
