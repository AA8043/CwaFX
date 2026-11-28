package org.a8043.cwaFX;

import cn.hutool.json.JSONObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonFileStore {
    public static JSONObject read(File file, CwaFX cwaFX, Class<?> configurationType) {
        if (!file.isFile()) return new JSONObject();
        try {
            return new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
        } catch (RuntimeException | IOException failure) {
            reportCorruption(file, cwaFX, configurationType, failure);
            return new JSONObject();
        }
    }

    public static void reportCorruption(File file, CwaFX cwaFX, Class<?> configurationType, Throwable failure) {
        boolean backupCreated = false;
        if (cwaFX.getConfig().isBackupCorruptSettings()) {
            try {
                Path backup = file.toPath().resolveSibling(file.getName()
                    + ".corrupt-" + Instant.now().toEpochMilli());
                Files.copy(file.toPath(), backup, StandardCopyOption.COPY_ATTRIBUTES);
                backupCreated = true;
            } catch (IOException backupFailure) {
                failure.addSuppressed(backupFailure);
            }
        }
        cwaFX.publishUserEvent(new ConfigurationCorruptedEvent(file, configurationType, failure, backupCreated));
    }

    public static void write(File file, String contents) {
        Path target = file.toPath();
        Path parent = target.toAbsolutePath().getParent();
        try {
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, file.getName() + ".", ".tmp");
            try {
                Files.writeString(temporary, contents, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Unable to atomically write " + file, failure);
        }
    }
}
