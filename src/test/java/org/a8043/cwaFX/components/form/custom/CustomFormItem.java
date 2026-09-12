package org.a8043.cwaFX.components.form.custom;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.a8043.cwaFX.components.form.FormItem;

public class CustomFormItem extends FormItem {
    private String promptText = "";

    public void setPromptText(String promptText) {
        this.promptText = promptText == null ? "" : promptText;
        requestRebuild();
    }

    @Override
    public Class<?> getValueType() {
        return String.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        return field;
    }

    @Override
    protected InputValue readInput(Node control) {
        String text = ((TextField) control).getText();
        return text == null || text.isBlank() ? emptyValue() : value(text);
    }

    @Override
    protected void writeValue(Node control, Object value) {
        ((TextField) control).setText(value instanceof String text ? text : "");
    }
}
