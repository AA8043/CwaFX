package org.a8043.cwaFX.keyMapping;

import cn.hutool.json.JSONObject;
import javafx.scene.input.KeyCombination;
import lombok.Getter;
import org.a8043.cwaFX.CwaFX;
import org.a8043.cwaFX.JsonFileStore;
import org.a8043.cwaFX.events.DestroyEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyMappings {
    private final File file;
    @Getter
    private final List<KeyMapping> keyMappings = new ArrayList<>();
    private final JSONObject json;

    public KeyMappings(CwaFX cwaFX) {
        file = new File(cwaFX.getConfig().getSettingsBaseDir(), "keyMappings.json");
        JSONObject loaded = JsonFileStore.read(file, cwaFX, KeyMapping.class);
        try {
            for (String name : loaded.keySet()) KeyCombination.valueOf(loaded.getStr(name));
        } catch (RuntimeException failure) {
            JsonFileStore.reportCorruption(file, cwaFX, KeyMapping.class, failure);
            loaded = new JSONObject();
        }
        json = loaded;

        cwaFX.addOnEvent(e -> {
            if (e instanceof DestroyEvent) {
                JsonFileStore.write(file, json.toString());
            }
        });
    }

    public void add(KeyMapping keyMapping) {
        String key = json.getStr(keyMapping.getName());
        if (key != null) {
            keyMapping = new KeyMapping(keyMapping.getName(), KeyCombination.valueOf(key), keyMapping.getOnlyIn());
        } else {
            json.set(keyMapping.getName(), keyMapping.getKey().toString());
        }
        keyMappings.add(keyMapping);
    }

    public void setKey(String name, KeyCombination key) {
        KeyMapping mapping = keyMappings.stream()
            .filter(km -> km.getName().equals(name))
            .findFirst()
            .orElse(null);
        keyMappings.remove(mapping);
        keyMappings.add(new KeyMapping(name, key, mapping != null ? mapping.getOnlyIn() : null));
        json.set(name, key.getName());
    }

    public KeyCombination getKey(String name) {
        return keyMappings.stream().filter(km -> Objects.equals(km.getName(), name))
            .findFirst().map(KeyMapping::getKey).orElse(null);
    }
}
