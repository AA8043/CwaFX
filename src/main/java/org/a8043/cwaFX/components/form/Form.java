package org.a8043.cwaFX.components.form;

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.window.Window;
import org.a8043.cwaFX.window.WindowCreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@DefaultProperty("items")
public class Form extends VBox {
    public static final String DEFAULT_SUBMIT_TEXT_KEY = "form.submit";

    @Getter
    private final ObservableList<FormItem> items = FXCollections.observableArrayList();
    private final Map<FormItem, RenderedItem> renderedItems = new IdentityHashMap<>();
    @Getter
    private final Button submitButton = new Button();
    private final ReadOnlyBooleanWrapper submitting = new ReadOnlyBooleanWrapper(this, "submitting", false);
    private final StringProperty submitTextKey = new SimpleStringProperty(this, "submitTextKey", DEFAULT_SUBMIT_TEXT_KEY);
    private final ObjectProperty<EventHandler<FormSubmitEvent>> onSubmit =
        new SimpleObjectProperty<>(this, "onSubmit") {
            @Override
            protected void invalidated() {
                Form.this.setEventHandler(FormSubmitEvent.FORM_SUBMIT, get());
            }
        };
    private final InvalidationListener itemConfigurationListener = observable -> rebuild();

    public Form() {
        getStyleClass().add("form");
        setSpacing(12);
        setFillWidth(true);

        submitButton.getStyleClass().add("form-submit-button");
        submitButton.disableProperty().bind(submitting);
        submitButton.setOnAction(event -> submit());
        submitTextKey.addListener((observable, oldValue, newValue) -> updateSubmitButtonText());
        items.addListener((ListChangeListener<FormItem>) change -> {
            while (change.next()) {
                if (change.wasRemoved()) {
                    change.getRemoved().forEach(this::detachItem);
                }
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(this::attachItem);
                }
            }
            rebuild();
        });
        updateSubmitButtonText();
        rebuild();
    }

    public boolean submit() {
        if (submitting.get()) {
            return false;
        }
        submitting.set(true);
        try {
            Object[] values = new Object[items.size()];
            boolean valid = true;

            for (int index = 0; index < items.size(); index++) {
                FormItem item = items.get(index);
                RenderedItem renderedItem = renderedItems.get(item);
                item.clearError(renderedItem.row(), renderedItem.control(), resolve(item.getHelperTextKey()));

                FormItem.InputValue inputValue = item.readInput(renderedItem.control());
                ValidationResult failure = inputValue.error();
                Object value = inputValue.value();

                if (failure == null && item.isMissing(value) && item.isRequired()) {
                    failure = ValidationResult.invalid(item.getRequiredMessageKey());
                }
                if (failure == null && value != null) {
                    for (Requirement requirement : item.getRequirements()) {
                        ValidationResult result = requirement.validate(value);
                        if (!result.isValid()) {
                            failure = result;
                            break;
                        }
                    }
                }

                if (failure != null) {
                    item.showError(renderedItem.row(), renderedItem.control(), resolve(failure));
                    valid = false;
                } else {
                    values[index] = value;
                }
            }

            if (!valid) {
                submitting.set(false);
                return false;
            }
            fireEvent(new FormSubmitEvent(this, this, values));
            submitting.set(false);
            return true;
        } catch (RuntimeException | Error exception) {
            log.error("Form submit failed", exception);
            submitting.set(false);
            Window window = WindowCreator.findByScene(getScene());
            if (window != null) {
                window.showTipModal(I18n.get("form.error", exception.getMessage()));
            }
            return false;
        }
    }

    public ReadOnlyBooleanProperty submittingProperty() {
        return submitting.getReadOnlyProperty();
    }

    public boolean isSubmitting() {
        return submitting.get();
    }

    public StringProperty submitTextKeyProperty() {
        return submitTextKey;
    }

    public String getSubmitTextKey() {
        return submitTextKey.get();
    }

    public void setSubmitTextKey(String submitTextKey) {
        this.submitTextKey.set(submitTextKey == null || submitTextKey.isBlank()
            ? DEFAULT_SUBMIT_TEXT_KEY : submitTextKey);
    }

    public ObjectProperty<EventHandler<FormSubmitEvent>> onSubmitProperty() {
        return onSubmit;
    }

    public EventHandler<FormSubmitEvent> getOnSubmit() {
        return onSubmit.get();
    }

    public void setOnSubmit(EventHandler<FormSubmitEvent> onSubmit) {
        this.onSubmit.set(onSubmit);
    }

    private void attachItem(FormItem item) {
        item.configurationVersionProperty().addListener(itemConfigurationListener);
    }

    private void detachItem(FormItem item) {
        item.configurationVersionProperty().removeListener(itemConfigurationListener);
        renderedItems.remove(item);
        item.setControl(null);
    }

    private void rebuild() {
        Map<FormItem, Object> currentValues = new IdentityHashMap<>();
        renderedItems.forEach((item, rendered) ->
            currentValues.put(item, item.readInput(rendered.control()).value()));
        renderedItems.clear();

        List<Node> rows = new ArrayList<>(items.size() + 1);
        for (FormItem item : items) {
            validateRequirements(item);
            String label = resolve(item.getLabelKey());
            String helperText = resolve(item.getHelperTextKey());
            Node control = item.createControl(label, helperText);
            Node row = item.createRow(control, label, helperText);
            Object currentValue = currentValues.get(item);
            if (currentValue != null) {
                item.writeValue(control, currentValue);
            }
            renderedItems.put(item, new RenderedItem(row, control));
            item.setControl(control);
            rows.add(row);
        }
        rows.add(submitButton);
        getChildren().setAll(rows);
    }

    private void validateRequirements(FormItem item) {
        for (Requirement requirement : item.getRequirements()) {
            if (!requirement.supports(item.getValueType())) {
                throw new IllegalArgumentException(requirement.getClass().getSimpleName()
                                                   + " does not support " + item.getValueType().getTypeName());
            }
        }
    }

    private String resolve(ValidationResult result) {
        String[] arguments = Arrays.stream(result.getArguments()).map(String::valueOf).toArray(String[]::new);
        return I18n.get(result.getMessageKey(), arguments);
    }

    private String resolve(String key) {
        return key == null || key.isBlank() ? "" : I18n.get(key);
    }

    private void updateSubmitButtonText() {
        submitButton.setText(resolve(submitTextKey.get()));
        submitButton.setAlignment(Pos.CENTER);
    }

    private record RenderedItem(Node row, Node control) {
    }
}
