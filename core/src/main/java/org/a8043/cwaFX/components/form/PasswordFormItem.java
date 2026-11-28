package org.a8043.cwaFX.components.form;

import javafx.scene.Node;
import org.a8043.cwaFX.components.MaterialTextField;

public class PasswordFormItem extends TextFormItem {
    @Override
    protected Node createControl(String label, String helperText) {
        MaterialTextField field = (MaterialTextField) super.createControl(label, helperText);
        field.setType(MaterialTextField.Type.PASSWORD);
        return field;
    }
}
