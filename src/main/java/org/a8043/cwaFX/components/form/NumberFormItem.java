package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import org.a8043.cwaFX.components.MaterialTextField;

public class NumberFormItem extends TextFormItem {
    @Override
    public Class<?> getValueType() {
        return Double.class;
    }

    @Override
    protected InputValue readInput(Node control) {
        String text = ((MaterialTextField) control).getText();
        if (text == null || text.isBlank()) {
            return emptyValue();
        }
        try {
            double parsedValue = Double.parseDouble(text);
            return Double.isFinite(parsedValue) ? value(parsedValue) : invalidValue("form.validation.number");
        } catch (NumberFormatException exception) {
            return invalidValue("form.validation.number");
        }
    }
}
