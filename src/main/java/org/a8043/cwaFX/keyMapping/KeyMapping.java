package org.a8043.cwaFX.keyMapping;

import javafx.scene.Node;
import javafx.scene.input.KeyCombination;
import lombok.Value;

@Value
public class KeyMapping {
    String name;
    KeyCombination key;
    Node onlyIn;
}
