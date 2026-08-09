package org.a8043.cwaFX.navigation;

import javafx.scene.Node;
import lombok.Value;

/**
 * A loaded FXML page and its controller.
 */
@Value
public class PageDefinition {
    String name;
    Node node;
    Object controller;
}
