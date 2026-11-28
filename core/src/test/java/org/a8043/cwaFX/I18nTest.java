package org.a8043.cwaFX;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nTest {
    @Test
    void applicationBundleOverridesFrameworkBundleAndMissingKeysFallBack() {
        I18n.load(I18nTest.class, Locale.ENGLISH);

        assertEquals("Overridden submit", I18n.get("form.submit"));
        assertEquals("This field is required.", I18n.get("form.validation.required"));
        assertEquals("missing.key", I18n.get("missing.key"));

        I18n.load(I18nTest.class, Locale.SIMPLIFIED_CHINESE);
        assertEquals("此项为必填项。", I18n.get("form.validation.required"));
    }
}
