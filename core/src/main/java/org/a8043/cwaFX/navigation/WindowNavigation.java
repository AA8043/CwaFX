package org.a8043.cwaFX.navigation;

import javafx.scene.Node;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Maintains the page history for one window.
 */
public final class WindowNavigation {
    private final PageRegistry pageRegistry;
    private final Consumer<Node> displayPage;
    private final Deque<PageEntry> history = new ArrayDeque<>();
    private final Object owner = new Object();
    private PageEntry current;

    public WindowNavigation(PageRegistry pageRegistry, Consumer<Node> displayPage) {
        this.pageRegistry = Objects.requireNonNull(pageRegistry, "The page registry must not be null.");
        this.displayPage = Objects.requireNonNull(displayPage, "The page display callback must not be null.");
    }

    public <T> void navigate(String pageName, T parameter) {
        navigate(pageName, parameter, null);
    }

    public <T, R> void navigate(String pageName, T parameter, Consumer<? super R> onResult) {
        PageEntry next = PageEntry.forPage(pageRegistry.get(pageName).create(), parameter, onResult, owner);
        PageEntry previous = current;
        try {
            activate(next);
        } catch (RuntimeException | Error failure) {
            restore(previous, failure);
            next.dispose();
            throw failure;
        }
        if (previous != null) history.push(previous);
        current = next;
    }

    public <T> void replace(String pageName, T parameter) {
        PageEntry next = PageEntry.forPage(pageRegistry.get(pageName).create(), parameter, null, owner);
        PageEntry previous = current;
        try {
            activate(next);
        } catch (RuntimeException | Error failure) {
            restore(previous, failure);
            next.dispose();
            throw failure;
        }
        current = next;
        if (previous != null) previous.dispose();
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    public void back() {
        back(null);
    }

    public <R> void back(R result) {
        if (history.isEmpty()) {
            throw new IllegalStateException("Cannot go back because the current page is the root page.");
        }

        PageEntry leaving = current;
        PageEntry target = history.peek();
        try {
            activate(target);
        } catch (RuntimeException | Error failure) {
            restore(leaving, failure);
            throw failure;
        }
        current = history.pop();
        try {
            leaving.deliverResult(result);
        } finally {
            leaving.dispose();
        }
    }

    private void activate(PageEntry entry) {
        entry.notifyController();
        displayPage.accept(entry.node());
    }

    private void restore(PageEntry entry, Throwable failure) {
        try {
            displayPage.accept(entry == null ? null : entry.node());
        } catch (RuntimeException | Error restoreFailure) {
            failure.addSuppressed(restoreFailure);
        }
    }

    public void close() {
        RuntimeException failure = null;
        if (current != null) {
            try {
                current.dispose();
            } catch (RuntimeException exception) {
                failure = exception;
            }
        }
        for (PageEntry entry : history) {
            try {
                entry.dispose();
            } catch (RuntimeException exception) {
                if (failure == null) failure = exception;
                else failure.addSuppressed(exception);
            }
        }
        history.clear();
        current = null;
        if (failure != null) throw failure;
    }

    private record PageEntry(Node node, Object controller, Object parameter, Consumer<Object> onResult,
                             PageDefinition page, Object owner) {
        private static PageEntry forPage(PageDefinition page, Object parameter, Consumer<?> onResult, Object owner) {
            page.claim(owner);
            return new PageEntry(page.getNode(), page.getController(), parameter, castResultConsumer(onResult),
                page, owner);
        }

        @SuppressWarnings("unchecked")
        private void notifyController() {
            if (controller instanceof Page<?> page) {
                ((Page<Object>) page).onNavigate(parameter);
            }
        }

        private void deliverResult(Object result) {
            if (onResult != null) {
                onResult.accept(result);
            }
        }

        private void dispose() {
            page.release(owner);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Consumer<Object> castResultConsumer(Consumer<?> onResult) {
            return onResult == null ? null : (Consumer) onResult;
        }
    }
}
