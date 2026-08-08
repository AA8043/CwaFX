package org.a8043.cwaFX;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.*;

@Slf4j
public class I18n {
    private static ResourceBundle appBundle;
    private static ResourceBundle frameworkBundle;
    @Getter
    private static final ResourceBundle bundle = new ResourceBundle() {
        @Override
        protected Object handleGetObject(String key) {
            return appBundle.containsKey(key) ? appBundle.getObject(key) : frameworkBundle.getObject(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            Set<String> keys = new HashSet<>();
            keys.addAll(appBundle.keySet());
            keys.addAll(frameworkBundle.keySet());
            return Collections.enumeration(keys);
        }
    };

    static void load(Class<?> clazz, Locale locale) {
        log.info("Language: {}", locale.getDisplayName());
        appBundle = loadAppBundle(locale, clazz.getModule());
        frameworkBundle = loadFrameworkBundle(locale);
    }

    public static String get(String key, String... args) {
        String str;
        try {
            str = bundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing i18n key: {}", key);
            str = key;
        }
        return MessageFormat.format(str, (Object[]) args);
    }

    private static ResourceBundle loadAppBundle(Locale locale, Module module) {
        try {
            return ResourceBundle.getBundle("languages.messages", locale, module);
        } catch (MissingResourceException e) {
            if (locale == Locale.ENGLISH) {
                log.error("Failed to load app bundle for locale: {}", locale);
                return new ResourceBundle() {
                    @Override
                    protected Object handleGetObject(String key) {
                        return null;
                    }

                    @Override
                    public Enumeration<String> getKeys() {
                        return Collections.enumeration(new HashSet<>());
                    }
                };
            }
            return loadAppBundle(Locale.ENGLISH, module);
        }
    }

    private static ResourceBundle loadFrameworkBundle(Locale locale) {
        try {
            return ResourceBundle.getBundle("defaultLanguages.messages", locale, I18n.class.getModule());
        } catch (MissingResourceException e) {
            log.error("Failed to load framework bundle for locale: {}", locale);
            return loadFrameworkBundle(Locale.ENGLISH);
        }
    }
}
