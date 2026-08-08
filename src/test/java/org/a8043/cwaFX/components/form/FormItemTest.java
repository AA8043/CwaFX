package org.a8043.cwaFX.components.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormItemTest {
    @Test
    void exposesStableDefaults() {
        FormItem item = new FormItem();

        assertEquals("", item.getLabelKey());
        assertEquals("", item.getHelperTextKey());
        assertEquals(ItemType.TEXT, item.getType());
        assertEquals(false, item.isRequired());
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());
    }

    @Test
    void normalizesNullAndBlankConfigurationValues() {
        FormItem item = new FormItem();

        item.setLabelKey(null);
        item.setHelperTextKey(null);
        item.setType(null);
        item.setRequiredMessageKey(null);
        assertEquals("", item.getLabelKey());
        assertEquals("", item.getHelperTextKey());
        assertEquals(ItemType.TEXT, item.getType());
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());

        item.setRequiredMessageKey("  ");
        assertEquals(FormItem.DEFAULT_REQUIRED_MESSAGE_KEY, item.getRequiredMessageKey());
        item.setRequiredMessageKey("custom.required");
        assertEquals("custom.required", item.getRequiredMessageKey());
    }
}
