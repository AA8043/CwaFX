package org.a8043.cwaFX.keyMapping;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import javafx.scene.input.KeyCombination;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KeyMappings {
    private final File file = new File("keyMappings.json");
    @Getter
    private final List<KeyMapping> keyMappings = new ArrayList<>();
    private final JSONObject json;

    public KeyMappings() {
        if (file.exists()) {
            json = new JSONObject(FileUtil.readUtf8String(file));
        } else {
            json = new JSONObject();
        }
    }

    public void add(KeyMapping keyMapping) {
        String key = json.getStr(keyMapping.getName());
        if (key != null) {
            keyMapping = new KeyMapping(keyMapping.getName(), KeyCombination.valueOf(key), keyMapping.getOnlyIn());
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
        FileUtil.writeUtf8String(json.toString(), file);
    }
}
