package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;

public class CheckBoxFormItem extends FormItem {
    @Override
    public Class<?> getValueType() {
        return Boolean.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        return new CheckBox();
    }

    @Override
    protected InputValue readInput(Node control) {
        return value(((CheckBox) control).isSelected());
    }

    @Override
    protected void writeValue(Node control, Object value) {
        ((CheckBox) control).setSelected(Boolean.TRUE.equals(value));
    }

    @Override
    protected boolean isMissing(Object value) {
        return !Boolean.TRUE.equals(value);
    }
}
