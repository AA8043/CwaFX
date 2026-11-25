package org.a8043.cwaFX.navigation;

import javafx.scene.Node;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicBoolean;

/** A loaded page or a factory capable of creating an isolated page instance. */
public class PageDefinition {
    private final String name;
    private final Node node;
    private final Object controller;
    private final Supplier<PageDefinition> factory;
    private final Runnable disposer;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private Object owner;
    private int ownerReferences;

    public PageDefinition(String name, Node node, Object controller) {
        this(name, node, controller, null, null);
    }

    public PageDefinition(String name, Node node, Object controller, Runnable disposer) {
        this(name, node, controller, null, disposer);
    }

    public PageDefinition(String name, Supplier<PageDefinition> factory) {
        this(name, null, null, Objects.requireNonNull(factory, "Page factory must not be null."), null);
    }

    private PageDefinition(String name, Node node, Object controller, Supplier<PageDefinition> factory,
                           Runnable disposer) {
        this.name = Objects.requireNonNull(name, "Page name must not be null.");
        this.node = node;
        this.controller = controller;
        this.factory = factory;
        this.disposer = disposer;
    }

    public String getName() { return name; }
    public Node getNode() { return node; }
    public Object getController() { return controller; }

    public PageDefinition create() {
        return factory == null ? this : Objects.requireNonNull(factory.get(), "Page factory returned null.");
    }

    public void dispose() {
        if (disposed.compareAndSet(false, true) && disposer != null) disposer.run();
    }

    synchronized void claim(Object navigationOwner) {
        if (owner != null && owner != navigationOwner) {
            throw new IllegalStateException("A registered Node page cannot be active in multiple windows; register a page factory.");
        }
        owner = navigationOwner;
        ownerReferences++;
    }

    synchronized void release(Object navigationOwner) {
        if (owner != navigationOwner || ownerReferences == 0) return;
        ownerReferences--;
        if (ownerReferences == 0) {
            owner = null;
            dispose();
        }
    }
}
