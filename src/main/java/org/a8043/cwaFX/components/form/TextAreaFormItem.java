package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import javafx.scene.control.TextArea;

public class TextAreaFormItem extends FormItem {
    @Override
    public Class<?> getValueType() {
        return String.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(4);
        return textArea;
    }

    @Override
    protected InputValue readInput(Node control) {
        String text = ((TextArea) control).getText();
        return text == null || text.isBlank() ? emptyValue() : value(text);
    }

    @Override
    protected void writeValue(Node control, Object value) {
        ((TextArea) control).setText(value instanceof String text ? text : "");
    }

    @Override
    protected boolean growsVertically() {
        return true;
    }
}
