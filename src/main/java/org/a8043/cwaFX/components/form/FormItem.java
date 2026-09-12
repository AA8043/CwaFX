package org.a8043.cwaFX.components.form;

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;

/**
 * Base configuration and rendering contract for an item displayed by a {@link Form}.
 */
@DefaultProperty("requirements")
@EqualsAndHashCode
public abstract class FormItem {
    public static final String DEFAULT_REQUIRED_MESSAGE_KEY = "form.validation.required";

    private static final PseudoClass PSEUDO_ERROR = PseudoClass.getPseudoClass("form-error");

    private final StringProperty labelKey = new SimpleStringProperty(this, "labelKey", "");
    private final StringProperty helperTextKey = new SimpleStringProperty(this, "helperTextKey", "");
    private final BooleanProperty required = new SimpleBooleanProperty(this, "required", false);
    private final StringProperty requiredMessageKey = new SimpleStringProperty(this, "requiredMessageKey",
        DEFAULT_REQUIRED_MESSAGE_KEY);
    private final ObservableList<Requirement> requirements = FXCollections.observableArrayList();
    @EqualsAndHashCode.Exclude
    private final ReadOnlyLongWrapper configurationVersion =
        new ReadOnlyLongWrapper(this, "configurationVersion", 0);
    private Node control;

    protected FormItem() {
        InvalidationListener listener = observable -> requestRebuild();
        labelKey.addListener(listener);
        helperTextKey.addListener(listener);
        required.addListener(listener);
        requiredMessageKey.addListener(listener);
        requirements.addListener(listener);
    }

    /**
     * Returns the Java type produced by this item when the form is submitted.
     */
    public abstract Class<?> getValueType();

    /**
     * Creates the interactive control. The supplied label and helper text are already localized.
     */
    protected abstract Node createControl(String label, String helperText);

    /**
     * Reads and converts the current control value.
     */
    protected abstract InputValue readInput(Node control);

    /**
     * Restores a previously read value after the item is rebuilt.
     */
    protected abstract void writeValue(Node control, Object value);

    /**
     * Creates the node inserted into the form for this item.
     */
    protected Node createRow(Node control, String labelText, String helperText) {
        VBox row = new VBox(4);
        row.getStyleClass().add("form-item");
        Label label = new Label(labelText);
        label.getStyleClass().add("form-item-label");
        Label helper = new Label(helperText);
        helper.getStyleClass().add("form-item-helper");
        control.getStyleClass().add("form-input");
        row.getChildren().addAll(label, control, helper);
        VBox.setVgrow(control, growsVertically() ? Priority.ALWAYS : Priority.NEVER);
        return row;
    }

    protected boolean growsVertically() {
        return false;
    }

    protected void showError(Node row, Node control, String message) {
        row.pseudoClassStateChanged(PSEUDO_ERROR, true);
        control.pseudoClassStateChanged(PSEUDO_ERROR, true);
        helperLabel(row).setText(message);
    }

    protected void clearError(Node row, Node control, String helperText) {
        row.pseudoClassStateChanged(PSEUDO_ERROR, false);
        control.pseudoClassStateChanged(PSEUDO_ERROR, false);
        helperLabel(row).setText(helperText);
    }

    protected boolean isMissing(Object value) {
        return value == null;
    }

    /**
     * Notifies a containing form that subclass-specific configuration has changed.
     */
    protected final void requestRebuild() {
        configurationVersion.set(configurationVersion.get() + 1);
    }

    protected static InputValue value(Object value) {
        return new InputValue(value, null);
    }

    protected static InputValue emptyValue() {
        return value(null);
    }

    protected static InputValue invalidValue(String messageKey) {
        return new InputValue(null, ValidationResult.invalid(messageKey));
    }

    public StringProperty labelKeyProperty() {
        return labelKey;
    }

    public String getLabelKey() {
        return labelKey.get();
    }

    public void setLabelKey(String labelKey) {
        this.labelKey.set(labelKey == null ? "" : labelKey);
    }

    public StringProperty helperTextKeyProperty() {
        return helperTextKey;
    }

    public String getHelperTextKey() {
        return helperTextKey.get();
    }

    public void setHelperTextKey(String helperTextKey) {
        this.helperTextKey.set(helperTextKey == null ? "" : helperTextKey);
    }

    public BooleanProperty requiredProperty() {
        return required;
    }

    public boolean isRequired() {
        return required.get();
    }

    public void setRequired(boolean required) {
        this.required.set(required);
    }

    public StringProperty requiredMessageKeyProperty() {
        return requiredMessageKey;
    }

    public String getRequiredMessageKey() {
        return requiredMessageKey.get();
    }

    public void setRequiredMessageKey(String requiredMessageKey) {
        this.requiredMessageKey.set(requiredMessageKey == null || requiredMessageKey.isBlank()
            ? DEFAULT_REQUIRED_MESSAGE_KEY : requiredMessageKey);
    }

    public ObservableList<Requirement> getRequirements() {
        return requirements;
    }

    public Node getControl() {
        return control;
    }

    ReadOnlyLongProperty configurationVersionProperty() {
        return configurationVersion.getReadOnlyProperty();
    }

    public void setControl(Node control) {
        this.control = control;
    }

    protected record InputValue(Object value, ValidationResult error) {
    }

    private static Label helperLabel(Node row) {
        if (row instanceof VBox box && box.getChildren().size() >= 3
            && box.getChildren().get(2) instanceof Label helper) {
            return helper;
        }
        throw new IllegalStateException("The default error handling requires the default form item row");
    }
}
