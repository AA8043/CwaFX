package org.a8043.cwaFX;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
public class I18n {
    @Getter
    private static ResourceBundle langBundle;

    static void init(Class<?> clazz) {
        Locale locale = Locale.getDefault();
        log.info("Language: {}", locale.getDisplayName());
        try {
            langBundle = ResourceBundle.getBundle("languages.messages", locale, clazz.getModule());
        } catch (MissingResourceException e) {
            log.warn("Missing language file for locale: {}. Falling back to default.", locale);
            langBundle = ResourceBundle.getBundle("languages.messages", Locale.ENGLISH, clazz.getModule());
        }
    }

    public static String get(String key, String... args) {
        String str;
        try {
            str = langBundle.getString(key);
        } catch (Exception e) {
            log.warn("Missing i18n key: {}", key);
            str = key;
        }
        return MessageFormat.format(str, (Object[]) args);
    }
}
