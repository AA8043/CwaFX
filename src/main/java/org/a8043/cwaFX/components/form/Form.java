package org.a8043.cwaFX.components.form;

import javafx.beans.DefaultProperty;
import javafx.beans.InvalidationListener;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.components.MaterialTextField;
import org.a8043.cwaFX.window.WindowCreator;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@DefaultProperty("items")
public class Form extends VBox {
    public static final String DEFAULT_SUBMIT_TEXT_KEY = "form.submit";

    private static final PseudoClass PSEUDO_ERROR = PseudoClass.getPseudoClass("form-error");

    @Getter
    private final ObservableList<FormItem> items = FXCollections.observableArrayList();
    private final Map<FormItem, InputAdapter> adapters = new IdentityHashMap<>();
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
                InputAdapter adapter = adapters.get(item);
                adapter.clearError();

                InputValue inputValue = adapter.readValue();
                ValidationResult failure = inputValue.error();
                Object value = inputValue.value();

                if (failure == null && isMissing(item, value) && item.isRequired()) {
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
                    adapter.showError(resolve(failure));
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
            submitting.set(false);
            WindowCreator.findByScene(getScene())
                .showTipModal(I18n.get("form.error", exception.getCause().getCause().getMessage()));
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
        item.labelKeyProperty().addListener(itemConfigurationListener);
        item.helperTextKeyProperty().addListener(itemConfigurationListener);
        item.typeProperty().addListener(itemConfigurationListener);
        item.requiredProperty().addListener(itemConfigurationListener);
        item.requiredMessageKeyProperty().addListener(itemConfigurationListener);
        item.getOptions().addListener(itemConfigurationListener);
        item.getRequirements().addListener(itemConfigurationListener);
    }

    private void detachItem(FormItem item) {
        item.labelKeyProperty().removeListener(itemConfigurationListener);
        item.helperTextKeyProperty().removeListener(itemConfigurationListener);
        item.typeProperty().removeListener(itemConfigurationListener);
        item.requiredProperty().removeListener(itemConfigurationListener);
        item.requiredMessageKeyProperty().removeListener(itemConfigurationListener);
        item.getOptions().removeListener(itemConfigurationListener);
        item.getRequirements().removeListener(itemConfigurationListener);
        item.setControl(null);
    }

    private void rebuild() {
        Map<FormItem, Object> currentValues = new IdentityHashMap<>();
        adapters.forEach((item, adapter) -> currentValues.put(item, adapter.readValue().value()));
        adapters.clear();

        List<Node> rows = new ArrayList<>(items.size() + 1);
        for (FormItem item : items) {
            validateRequirements(item);
            InputAdapter adapter = createAdapter(item);
            Object currentValue = currentValues.get(item);
            if (currentValue != null) {
                adapter.setValue(currentValue);
            }
            adapters.put(item, adapter);
            item.setControl(adapter.control());
            rows.add(adapter.row());
        }
        rows.add(submitButton);
        getChildren().setAll(rows);
    }

    private void validateRequirements(FormItem item) {
        for (Requirement requirement : item.getRequirements()) {
            if (!requirement.supports(item.getType())) {
                throw new IllegalArgumentException(requirement.getClass().getSimpleName()
                                                   + " does not support " + item.getType());
            }
        }
    }

    private InputAdapter createAdapter(FormItem item) {
        return switch (item.getType()) {
            case TEXT -> materialTextAdapter(item, false, MaterialTextField.Type.NORMAL);
            case PASSWORD -> materialTextAdapter(item, false, MaterialTextField.Type.PASSWORD);
            case NUMBER -> materialTextAdapter(item, true, MaterialTextField.Type.NORMAL);
            case TEXT_AREA -> textAreaAdapter(item);
            case COMBO_BOX -> comboBoxAdapter(item);
            case CHECK_BOX -> checkBoxAdapter(item);
            case DATE -> dateAdapter(item);
        };
    }

    private InputAdapter materialTextAdapter(FormItem item, boolean number, MaterialTextField.Type type) {
        MaterialTextField field = new MaterialTextField(resolve(item.getLabelKey()));
        field.setType(type);
        field.setHelperText(resolve(item.getHelperTextKey()));
        field.setMaxWidth(Double.MAX_VALUE);

        return new InputAdapter() {
            @Override
            public Node row() {
                return field;
            }

            @Override
            public Node control() {
                return field;
            }

            @Override
            public InputValue readValue() {
                String text = field.getText();
                if (text == null || text.isBlank()) {
                    return InputValue.empty();
                }
                if (!number) {
                    return InputValue.of(text);
                }
                try {
                    double value = Double.parseDouble(text);
                    return Double.isFinite(value) ? InputValue.of(value)
                        : InputValue.invalid("form.validation.number");
                } catch (NumberFormatException exception) {
                    return InputValue.invalid("form.validation.number");
                }
            }

            @Override
            public void setValue(Object value) {
                field.setText(value == null ? "" : String.valueOf(value));
            }

            @Override
            public void showError(String message) {
                field.setError(true);
                field.setHelperText(message);
            }

            @Override
            public void clearError() {
                field.setError(false);
                field.setHelperText(resolve(item.getHelperTextKey()));
            }
        };
    }

    private InputAdapter textAreaAdapter(FormItem item) {
        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(4);
        return standardAdapter(item, textArea,
            () -> stringValue(textArea.getText()),
            value -> textArea.setText(value instanceof String text ? text : ""));
    }

    private InputAdapter comboBoxAdapter(FormItem item) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().setAll(item.getOptions());
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return standardAdapter(item, comboBox,
            () -> InputValue.of(comboBox.getValue()),
            value -> comboBox.setValue(value instanceof String text ? text : null));
    }

    private InputAdapter checkBoxAdapter(FormItem item) {
        CheckBox checkBox = new CheckBox();
        return standardAdapter(item, checkBox,
            () -> InputValue.of(checkBox.isSelected()),
            value -> checkBox.setSelected(Boolean.TRUE.equals(value)));
    }

    private InputAdapter dateAdapter(FormItem item) {
        DatePicker datePicker = new DatePicker();
        datePicker.setMaxWidth(Double.MAX_VALUE);
        return standardAdapter(item, datePicker,
            () -> InputValue.of(datePicker.getValue()),
            value -> datePicker.setValue(value instanceof LocalDate date ? date : null));
    }

    private InputAdapter standardAdapter(FormItem item, Node control, Supplier<InputValue> reader,
                                         Consumer<Object> valueWriter) {
        VBox row = new VBox(4);
        row.getStyleClass().add("form-item");
        Label label = new Label(resolve(item.getLabelKey()));
        label.getStyleClass().add("form-item-label");
        Label helper = new Label(resolve(item.getHelperTextKey()));
        helper.getStyleClass().add("form-item-helper");
        control.getStyleClass().add("form-input");
        row.getChildren().addAll(label, control, helper);
        VBox.setVgrow(control, item.getType() == ItemType.TEXT_AREA ? Priority.ALWAYS : Priority.NEVER);

        return new InputAdapter() {
            @Override
            public Node row() {
                return row;
            }

            @Override
            public Node control() {
                return control;
            }

            @Override
            public InputValue readValue() {
                return reader.get();
            }

            @Override
            public void setValue(Object value) {
                valueWriter.accept(value);
            }

            @Override
            public void showError(String message) {
                row.pseudoClassStateChanged(PSEUDO_ERROR, true);
                control.pseudoClassStateChanged(PSEUDO_ERROR, true);
                helper.setText(message);
            }

            @Override
            public void clearError() {
                row.pseudoClassStateChanged(PSEUDO_ERROR, false);
                control.pseudoClassStateChanged(PSEUDO_ERROR, false);
                helper.setText(resolve(item.getHelperTextKey()));
            }
        };
    }

    private static InputValue stringValue(String text) {
        return text == null || text.isBlank() ? InputValue.empty() : InputValue.of(text);
    }

    private boolean isMissing(FormItem item, Object value) {
        return item.getType() == ItemType.CHECK_BOX ? !Boolean.TRUE.equals(value) : value == null;
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

    private interface InputAdapter {
        Node row();

        Node control();

        InputValue readValue();

        void setValue(Object value);

        void showError(String message);

        void clearError();
    }

    private record InputValue(Object value, ValidationResult error) {
        static InputValue of(Object value) {
            return new InputValue(value, null);
        }

        static InputValue empty() {
            return of(null);
        }

        static InputValue invalid(String messageKey) {
            return new InputValue(null, ValidationResult.invalid(messageKey));
        }
    }
}
