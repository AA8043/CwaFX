package org.a8043.cwaFX.components.form;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;

public class ComboBoxFormItem extends FormItem {
    private final ObservableList<String> options = FXCollections.observableArrayList();

    public ComboBoxFormItem() {
        InvalidationListener listener = observable -> requestRebuild();
        options.addListener(listener);
    }

    public ObservableList<String> getOptions() {
        return options;
    }

    @Override
    public Class<?> getValueType() {
        return String.class;
    }

    @Override
    protected Node createControl(String label, String helperText) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(options);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    @Override
    protected InputValue readInput(Node control) {
        return value(((ComboBox<?>) control).getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void writeValue(Node control, Object value) {
        ((ComboBox<String>) control).setValue(value instanceof String text ? text : null);
    }
}
