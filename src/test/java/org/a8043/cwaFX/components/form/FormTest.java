package org.a8043.cwaFX.components.form;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import org.a8043.cwaFX.components.MaterialTextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormTest {
    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
        }
    }

    @Test
    void rendersConfiguredControlTypes() throws Exception {
        onFx(() -> {
            Form form = new Form();
            for (ItemType type : ItemType.values()) {
                FormItem item = new FormItem();
                item.setType(type);
                form.getItems().add(item);
            }

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

            FormItem name = new FormItem();
            name.setRequired(true);
            TextLengthRequirement length = new TextLengthRequirement();
            length.setMinLength(3);
            name.getRequirements().add(length);

            FormItem number = new FormItem();
            number.setType(ItemType.NUMBER);
            NumberRangeRequirement range = new NumberRangeRequirement();
            range.setMin(1);
            range.setMax(10);
            number.getRequirements().add(range);

            FormItem optional = new FormItem();
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
            assertTrue(form.isSubmitting());
            assertTrue(form.getSubmitButton().isDisabled());
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
