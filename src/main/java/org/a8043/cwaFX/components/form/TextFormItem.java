package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import org.a8043.cwaFX.components.MaterialTextField;

public class TextFormItem extends FormItem {
    @Override
    public Class<?> getValueType() {
        return String.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        MaterialTextField field = new MaterialTextField(label);
        field.setHelperText(helperText);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    @Override
    protected InputValue readInput(Node control) {
        String text = ((MaterialTextField) control).getText();
        return text == null || text.isBlank() ? emptyValue() : value(text);
    }

    @Override
    protected void writeValue(Node control, Object value) {
        ((MaterialTextField) control).setText(value == null ? "" : String.valueOf(value));
    }

    @Override
    protected Node createRow(Node control, String labelText, String helperText) {
        return control;
    }

    @Override
    protected void showError(Node row, Node control, String message) {
        MaterialTextField field = (MaterialTextField) control;
        field.setError(true);
        field.setHelperText(message);
    }

    @Override
    protected void clearError(Node row, Node control, String helperText) {
        MaterialTextField field = (MaterialTextField) control;
        field.setError(false);
        field.setHelperText(helperText);
    }
}
