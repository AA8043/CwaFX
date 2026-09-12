package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import javafx.scene.control.DatePicker;

import java.time.LocalDate;

public class DateFormItem extends FormItem {
    @Override
    public Class<?> getValueType() {
        return LocalDate.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        return datePicker;
    }

    @Override
    protected InputValue readInput(Node control) {
        return value(((DatePicker) control).getValue());
    }

    @Override
    protected void writeValue(Node control, Object value) {
        ((DatePicker) control).setValue(value instanceof LocalDate date ? date : null);
    }
}
