package org.a8043.cwaFX.components.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FormSubmitEventTest {
    @Test
    void copiesValuesAndSupportsIndexedAccess() {
        Object[] values = {"Ada", 42};
        FormSubmitEvent event = new FormSubmitEvent(this, null, values);
        values[0] = "changed";

        assertArrayEquals(new Object[]{"Ada", 42}, event.getValues());
        assertEquals("Ada", event.getValue(0));
        assertEquals(42, event.getValue(1));
    }

    @Test
    void nullValuesBecomeAnEmptyArray() {
        FormSubmitEvent event = new FormSubmitEvent(this, null, null);

        assertArrayEquals(new Object[0], event.getValues());
    }
}
