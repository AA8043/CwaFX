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
    private PageEntry current;

    public WindowNavigation(PageRegistry pageRegistry, Consumer<Node> displayPage) {
        this.pageRegistry = Objects.requireNonNull(pageRegistry, "The page registry must not be null.");
        this.displayPage = Objects.requireNonNull(displayPage, "The page display callback must not be null.");
    }

    public <T> void navigate(String pageName, T parameter) {
        navigate(pageName, parameter, null);
    }

    public <T, R> void navigate(String pageName, T parameter, Consumer<? super R> onResult) {
        PageEntry next = PageEntry.forPage(pageRegistry.get(pageName), parameter, onResult);
        if (current != null) {
            history.push(current);
        }
        current = next;
        activate(current);
    }

    public <T> void replace(String pageName, T parameter) {
        PageEntry next = PageEntry.forPage(pageRegistry.get(pageName), parameter, null);
        current = next;
        activate(current);
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
        current = history.pop();
        activate(current);
        leaving.deliverResult(result);
    }

    private void activate(PageEntry entry) {
        entry.notifyController();
        displayPage.accept(entry.node());
    }

    private record PageEntry(Node node, Object controller, Object parameter, Consumer<Object> onResult) {
        private static PageEntry forPage(PageDefinition page, Object parameter, Consumer<?> onResult) {
            return new PageEntry(page.getNode(), page.getController(), parameter, castResultConsumer(onResult));
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

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static Consumer<Object> castResultConsumer(Consumer<?> onResult) {
            return onResult == null ? null : (Consumer) onResult;
        }
    }
}
