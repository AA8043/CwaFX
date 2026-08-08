package org.a8043.cwaFX.components.form;

import javafx.beans.DefaultProperty;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@DefaultProperty("requirements")
@EqualsAndHashCode
public class FormItem {
    public static final String DEFAULT_REQUIRED_MESSAGE_KEY = "form.validation.required";

    private final StringProperty labelKey = new SimpleStringProperty(this, "labelKey", "");
    private final StringProperty helperTextKey = new SimpleStringProperty(this, "helperTextKey", "");
    private final ObjectProperty<ItemType> type = new SimpleObjectProperty<>(this, "type", ItemType.TEXT);
    private final BooleanProperty required = new SimpleBooleanProperty(this, "required", false);
    private final StringProperty requiredMessageKey = new SimpleStringProperty(this, "requiredMessageKey",
        DEFAULT_REQUIRED_MESSAGE_KEY);
    @Getter
    private final ObservableList<String> options = FXCollections.observableArrayList();
    @Getter
    private final ObservableList<Requirement> requirements = FXCollections.observableArrayList();

    @Getter
    @Setter
    private Node control;

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

    public ObjectProperty<ItemType> typeProperty() {
        return type;
    }

    public ItemType getType() {
        return type.get();
    }

    public void setType(ItemType type) {
        this.type.set(type == null ? ItemType.TEXT : type);
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
}
