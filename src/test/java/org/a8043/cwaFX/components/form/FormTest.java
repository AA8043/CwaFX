package org.a8043.cwaFX.components.form;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.a8043.cwaFX.components.MaterialTextField;
import org.a8043.cwaFX.components.form.custom.CustomFormItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormTest {
    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void rendersConfiguredControlTypes() throws Exception {
        onFx(() -> {
            Form form = new Form();
            form.getItems().addAll(new TextFormItem(), new PasswordFormItem(), new TextAreaFormItem(),
                new ComboBoxFormItem(), new NumberFormItem(), new CheckBoxFormItem(), new DateFormItem());

            assertInstanceOf(MaterialTextField.class, form.getItems().get(0).getControl());
            assertEquals(MaterialTextField.Type.NORMAL,
                ((MaterialTextField) form.getItems().get(0).getControl()).getType());
            assertEquals(MaterialTextField.Type.PASSWORD,
                ((MaterialTextField) form.getItems().get(1).getControl()).getType());
            assertInstanceOf(TextArea.class, form.getItems().get(2).getControl());
            assertInstanceOf(ComboBox.class, form.getItems().get(3).getControl());
            assertInstanceOf(MaterialTextField.class, form.getItems().get(4).getControl());
            assertInstanceOf(CheckBox.class, form.getItems().get(5).getControl());
            assertInstanceOf(DatePicker.class, form.getItems().get(6).getControl());
            return null;
        });
    }

    @Test
    void blocksInvalidSubmitAndDispatchesOrderedValues() throws Exception {
        onFx(() -> {
            Form form = new Form();
            AtomicReference<FormSubmitEvent> submitted = new AtomicReference<>();
            form.setOnSubmit(submitted::set);

            FormItem name = new TextFormItem();
            name.setRequired(true);
            TextLengthRequirement length = new TextLengthRequirement();
            length.setMinLength(3);
            name.getRequirements().add(length);

            FormItem number = new NumberFormItem();
            NumberRangeRequirement range = new NumberRangeRequirement();
            range.setMin(1);
            range.setMax(10);
            number.getRequirements().add(range);

            FormItem optional = new TextFormItem();
            form.getItems().addAll(name, number, optional);

            assertFalse(form.submit());
            assertFalse(form.getSubmitButton().isDisabled());
            assertTrue(((MaterialTextField) name.getControl()).isError());

            ((MaterialTextField) name.getControl()).setText("Ada");
            ((MaterialTextField) number.getControl()).setText("12");
            assertFalse(form.submit());

            ((MaterialTextField) number.getControl()).setText("4.5");
            assertTrue(form.submit());
            assertNotNull(submitted.get());
            assertArrayEquals(new Object[]{"Ada", 4.5d, null}, submitted.get().getValues());
            assertFalse(form.isSubmitting());
            assertFalse(form.getSubmitButton().isDisabled());
            return null;
        });
    }

    @Test
    void loadsNestedItemsAndSubmitHandlerFromFxml() throws Exception {
        onFx(() -> {
            FXMLLoader loader = new FXMLLoader(FormTest.class.getResource("FormTest.fxml"));
            Form form = loader.load();
            FxmlController controller = loader.getController();

            assertEquals(2, form.getItems().size());
            MaterialTextField username = (MaterialTextField) form.getItems().get(0).getControl();
            username.setText("Ada");
            assertTrue(form.submit());
            assertNotNull(controller.event);
            assertArrayEquals(new Object[]{"Ada", null}, controller.event.getValues());
            return null;
        });
    }

    @Test
    void materialTextFieldPreservesTextWhenSwitchingPasswordMode() throws Exception {
        onFx(() -> {
            MaterialTextField field = new MaterialTextField();
            field.setText("secret");
            field.setType(MaterialTextField.Type.PASSWORD);

            assertEquals("secret", field.getText());
            assertEquals(MaterialTextField.Type.PASSWORD, field.getType());
            assertTrue(field.getTextField().getStyleClass().contains("password-field"));
            return null;
        });
    }

    @Test
    void materialTextFieldUpdatesLabelsErrorStateAndNullType() throws Exception {
        onFx(() -> {
            MaterialTextField field = new MaterialTextField("original");
            field.setLabelText("updated");
            field.setHelperText("help");
            field.setError(true);
            field.setType(null);

            assertEquals("updated", field.getLabelText());
            assertEquals("help", field.getHelperText());
            assertTrue(field.isError());
            assertTrue(field.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("mtf-error")));
            assertEquals(MaterialTextField.Type.NORMAL, field.getType());
            return null;
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void submitsValuesFromStandardControlsAndRequiresCheckboxSelection() throws Exception {
        onFx(() -> {
            Form form = new Form();
            AtomicReference<FormSubmitEvent> submitted = new AtomicReference<>();
            form.setOnSubmit(submitted::set);

            ComboBoxFormItem choice = new ComboBoxFormItem();
            choice.getOptions().addAll("one", "two");
            FormItem accepted = new CheckBoxFormItem();
            accepted.setRequired(true);
            FormItem date = new DateFormItem();
            form.getItems().addAll(choice, accepted, date);

            ((ComboBox<String>) choice.getControl()).setValue("two");
            ((DatePicker) date.getControl()).setValue(LocalDate.of(2026, 8, 8));
            assertFalse(form.submit());
            assertTrue(((CheckBox) accepted.getControl()).getPseudoClassStates()
                .contains(javafx.css.PseudoClass.getPseudoClass("form-error")));

            ((CheckBox) accepted.getControl()).setSelected(true);
            assertTrue(form.submit());
            assertArrayEquals(new Object[]{"two", true, LocalDate.of(2026, 8, 8)}, submitted.get().getValues());
            return null;
        });
    }

    @Test
    void rebuildPreservesValuesAndClearsControlsOfRemovedItems() throws Exception {
        onFx(() -> {
            Form form = new Form();
            FormItem text = new TextFormItem();
            form.getItems().add(text);
            MaterialTextField originalControl = (MaterialTextField) text.getControl();
            originalControl.setText("preserved");

            text.setHelperTextKey("form.submit");
            MaterialTextField rebuiltControl = (MaterialTextField) text.getControl();
            assertNotSame(originalControl, rebuiltControl);
            assertEquals("preserved", rebuiltControl.getText());

            form.getItems().remove(text);
            assertNull(text.getControl());
            assertEquals(1, form.getChildren().size());
            assertEquals(form.getSubmitButton(), form.getChildren().get(0));
            return null;
        });
    }

    @Test
    void supportsExternalItemsAndSubclassConfigurationRebuilds() throws Exception {
        onFx(() -> {
            Form form = new Form();
            CustomFormItem item = new CustomFormItem();
            AtomicReference<FormSubmitEvent> submitted = new AtomicReference<>();
            form.setOnSubmit(submitted::set);
            form.getItems().add(item);

            TextField originalControl = (TextField) item.getControl();
            originalControl.setText("custom value");
            item.setPromptText("updated prompt");

            TextField rebuiltControl = (TextField) item.getControl();
            assertNotSame(originalControl, rebuiltControl);
            assertEquals("updated prompt", rebuiltControl.getPromptText());
            assertEquals("custom value", rebuiltControl.getText());
            assertTrue(form.submit());
            assertArrayEquals(new Object[]{"custom value"}, submitted.get().getValues());
            return null;
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void comboBoxOptionsRebuildControlAndPreserveSelection() throws Exception {
        onFx(() -> {
            Form form = new Form();
            ComboBoxFormItem item = new ComboBoxFormItem();
            item.getOptions().add("one");
            form.getItems().add(item);

            ComboBox<String> originalControl = (ComboBox<String>) item.getControl();
            originalControl.setValue("one");
            item.getOptions().add("two");

            ComboBox<String> rebuiltControl = (ComboBox<String>) item.getControl();
            assertNotSame(originalControl, rebuiltControl);
            assertEquals(java.util.List.of("one", "two"), rebuiltControl.getItems());
            assertEquals("one", rebuiltControl.getValue());
            return null;
        });
    }

    public static class FxmlController {
        @FXML
        private Form form;
        private FormSubmitEvent event;

        @FXML
        private void submit(FormSubmitEvent event) {
            this.event = event;
        }
    }

    private static <T> T onFx(Callable<T> callable) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return callable.call();
        }
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
