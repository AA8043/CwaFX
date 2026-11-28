package org.a8043.cwaFX;

import cn.hutool.json.JSONObject;
import org.a8043.cwaFX.annotationHandlers.SettingsHandler;
import org.a8043.cwaFX.annotations.Settings;
import org.a8043.cwaFX.annotations.settings.Deserializer;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.InitEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void restoresFieldsAndReportsCustomDeserializerFailures(boolean checked, @TempDir Path directory)
        throws Exception {
        Path settingsFile = directory.resolve("settings.json");
        String contents = "{\"value\":\"saved value\"}";
        Files.writeString(settingsFile, contents);
        RecordingCwaFX cwaFX = new RecordingCwaFX();
        setSettingsBaseDir(cwaFX, directory);
        Exception failure = checked
            ? new IOException("Invalid custom settings")
            : new IllegalArgumentException("Invalid custom settings");
        FailingSettings bean = cwaFX.getContext().getBean(FailingSettings.class, "", failure);

        assertDoesNotThrow(() -> new SettingsHandler().onEvent(FailingSettings.class,
            FailingSettings.class.getAnnotation(Settings.class), new InitEvent(1), cwaFX.getContext()));

        assertEquals("default", bean.getValue());
        assertEquals(1, cwaFX.corruptions.size());
        ConfigurationCorruptedEvent corrupted = cwaFX.corruptions.getFirst();
        assertSame(failure, corrupted.getCause());
        assertEquals(settingsFile.toFile(), corrupted.getFile());
        assertEquals(FailingSettings.class, corrupted.getConfigurationType());
        assertTrue(corrupted.isBackupCreated());
        assertEquals(contents, Files.readString(settingsFile));
        try (var files = Files.list(directory)) {
            List<Path> backups = files.filter(path -> path.getFileName().toString()
                .startsWith("settings.json.corrupt-")).toList();
            assertEquals(1, backups.size());
            assertEquals(contents, Files.readString(backups.getFirst()));
        }
    }

    @Test
    void propagatesDeserializerErrorsWithoutReportingCorruption(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("settings.json"), "{}");
        RecordingCwaFX cwaFX = new RecordingCwaFX();
        setSettingsBaseDir(cwaFX, directory);
        Error failure = new AssertionError("Deserializer failed");
        cwaFX.getContext().getBean(ErrorSettings.class, "", failure);

        Error thrown = assertThrows(AssertionError.class, () -> new SettingsHandler().onEvent(
            ErrorSettings.class, ErrorSettings.class.getAnnotation(Settings.class),
            new InitEvent(1), cwaFX.getContext()));

        assertSame(failure, thrown);
        assertTrue(cwaFX.corruptions.isEmpty());
    }

    @Test
    void rejectsInvalidDeserializerSignaturesWithoutReportingCorruption(@TempDir Path directory)
        throws Exception {
        Files.writeString(directory.resolve("settings.json"), "{}");
        RecordingCwaFX cwaFX = new RecordingCwaFX();
        setSettingsBaseDir(cwaFX, directory);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> new SettingsHandler().onEvent(InvalidSettings.class,
                InvalidSettings.class.getAnnotation(Settings.class), new InitEvent(1), cwaFX.getContext()));

        assertTrue(failure.getMessage().contains("must accept a single JSONObject parameter"));
        assertTrue(cwaFX.corruptions.isEmpty());
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

    private static class RecordingCwaFX extends CwaFX {
        private final List<ConfigurationCorruptedEvent> corruptions = new ArrayList<>();

        private RecordingCwaFX() {
            super(SettingsHandlerTest.class, new String[0]);
        }

        @Override
        public synchronized void publishUserEvent(org.a8043.cwaFX.userEvent.Event event) {
            if (event instanceof ConfigurationCorruptedEvent corrupted) {
                corruptions.add(corrupted);
            }
            super.publishUserEvent(event);
        }
    }

    @Settings("settings.json")
    public static class FailingSettings extends SavedSettings {
        private final Exception failure;

        public FailingSettings(Exception failure) {
            this.failure = failure;
        }

        @Deserializer
        private void deserialize(JSONObject json) throws Exception {
            setValue(json.getStr("value"));
            throw failure;
        }
    }

    @Settings("settings.json")
    public static class ErrorSettings extends SavedSettings {
        private final Error failure;

        public ErrorSettings(Error failure) {
            this.failure = failure;
        }

        @Deserializer
        private void deserialize(JSONObject json) {
            throw failure;
        }
    }

    @Settings("settings.json")
    public static class InvalidSettings {
        @Deserializer
        private void deserialize() {
        }
    }
}
