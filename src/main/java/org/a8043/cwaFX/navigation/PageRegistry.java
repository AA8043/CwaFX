package org.a8043.cwaFX.navigation;

import javafx.scene.Node;
import org.a8043.cwaFX.BeanKey;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of FXML pages available to windows in the application.
 */
public class PageRegistry {
    private final Map<String, PageDefinition> pages = new ConcurrentHashMap<>();

    public void register(String name, Node node, Object controller) {
        String normalizedName = normalize(name);
        PageDefinition definition = new PageDefinition(normalizedName,
            Objects.requireNonNull(node, "The page node must not be null."), controller);
        if (pages.putIfAbsent(normalizedName, definition) != null) {
            throw new IllegalStateException("A page named '" + normalizedName + "' is already registered.");
        }
    }

    public PageDefinition get(String name) {
        String normalizedName = normalize(name);
        PageDefinition definition = pages.get(normalizedName);
        if (definition == null) {
            throw new IllegalArgumentException("No page named '" + normalizedName + "' is registered.");
        }
        return definition;
    }

    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The page name must not be blank.");
        }
        return new BeanKey(Node.class, name).getName();
    }
}
