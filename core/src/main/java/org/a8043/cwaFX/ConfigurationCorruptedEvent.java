package org.a8043.cwaFX;

import lombok.Getter;
import org.a8043.cwaFX.userEvent.Event;

import java.io.File;

/** Reports a JSON configuration file that could not be read. */
@Getter
public final class ConfigurationCorruptedEvent extends Event {
    private final File file;
    private final Class<?> configurationType;
    private final Throwable cause;
    private final boolean backupCreated;

    public ConfigurationCorruptedEvent(File file, Class<?> configurationType,
                                       Throwable cause, boolean backupCreated) {
        this.file = file;
        this.configurationType = configurationType;
        this.cause = cause;
        this.backupCreated = backupCreated;
    }
}
