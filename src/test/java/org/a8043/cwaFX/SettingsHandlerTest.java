package org.a8043.cwaFX;

import org.a8043.cwaFX.annotationHandlers.SettingsHandler;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.InitEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsHandlerTest {
    @Test
    void persistsSettingsAndRestoresThemInANewContext() throws Exception {
        Path directory = Files.createTempDirectory("cwafx-settings-test-");
        Path settingsFile = directory.resolve("settings.json");
        SettingsHandler handler = new SettingsHandler();
        Settings settings = SavedSettings.class.getAnnotation(Settings.class);
        try {
            CwaFX writer = new CwaFX(SettingsHandlerTest.class, new String[0]);
            setSettingsBaseDir(writer, directory);
            SavedSettings saved = writer.getContext().getBean(SavedSettings.class, "");
            saved.setValue("saved value");

            handler.onEvent(SavedSettings.class, settings, new DestroyEvent(), writer.getContext());
            assertTrue(Files.exists(settingsFile));

            CwaFX reader = new CwaFX(SettingsHandlerTest.class, new String[0]);
            setSettingsBaseDir(reader, directory);
            handler.onEvent(SavedSettings.class, settings, new InitEvent(1), reader.getContext());

            assertEquals("saved value", reader.getContext().getBean(SavedSettings.class, "").getValue());
        } finally {
            Files.deleteIfExists(settingsFile);
            Files.deleteIfExists(directory);
        }
    }

    private static void setSettingsBaseDir(CwaFX cwaFX, Path directory) throws ReflectiveOperationException {
        Field field = AppConfig.class.getDeclaredField("settingsBaseDir");
        field.setAccessible(true);
        field.set(cwaFX.getConfig(), directory.toFile());
    }

    @Settings("settings.json")
    public static class SavedSettings {
        private String value = "default";

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
