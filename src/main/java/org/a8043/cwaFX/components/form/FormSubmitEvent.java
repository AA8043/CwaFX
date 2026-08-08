package org.a8043.cwaFX.components.form;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import lombok.Getter;

public class FormSubmitEvent extends Event {
    public static final EventType<FormSubmitEvent> FORM_SUBMIT = new EventType<>(Event.ANY, "FORM_SUBMIT");
    @Getter
    private final Object[] values;

    public FormSubmitEvent(Object source, EventTarget target, Object[] values) {
        super(source, target, FORM_SUBMIT);
        this.values = values == null ? new Object[0] : values.clone();
    }

    public Object getValue(int index) {
        return values[index];
    }
}
