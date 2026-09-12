package org.a8043.cwaFX.components.form;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormItemTest {
    @Test
    void isAbstractAndExposesConcreteValueTypes() {
        assertTrue(Modifier.isAbstract(FormItem.class.getModifiers()));
        assertEquals(String.class, new TextFormItem().getValueType());
        assertEquals(String.class, new PasswordFormItem().getValueType());
        assertEquals(String.class, new TextAreaFormItem().getValueType());
        assertEquals(String.class, new ComboBoxFormItem().getValueType());
        assertEquals(Double.class, new NumberFormItem().getValueType());
        assertEquals(Boolean.class, new CheckBoxFormItem().getValueType());
        assertEquals(java.time.LocalDate.class, new DateFormItem().getValueType());
    }

    @Test
    void exposesStableDefaults() {
        FormItem item = new TextFormItem();

        assertEquals("", item.getLabelKey());
        assertEquals("", item.getHelperTextKey());
        assertEquals(String.class, item.getValueType());
        assertEquals(false, item.isRequired());
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());
    }

    @Test
    void normalizesNullAndBlankConfigurationValues() {
        FormItem item = new TextFormItem();

        item.setLabelKey(null);
        item.setHelperTextKey(null);
        item.setRequiredMessageKey(null);
        assertEquals("", item.getLabelKey());
        assertEquals("", item.getHelperTextKey());
        assertEquals(String.class, item.getValueType());
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());

        item.setRequiredMessageKey("  ");
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());
        item.setRequiredMessageKey("custom.required");
        assertEquals("custom.required", item.getRequiredMessageKey());
    }
}
