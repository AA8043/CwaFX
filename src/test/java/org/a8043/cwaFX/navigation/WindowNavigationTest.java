package org.a8043.cwaFX.navigation;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class WindowNavigationTest {
    @Test
    void pushesPagesAndDeliversResultWhenGoingBack() {
        PageRegistry registry = new PageRegistry();
        TestPage home = new TestPage();
        TestPage detail = new TestPage();
        Node homeNode = new Rectangle();
        Node detailNode = new Rectangle();
        registry.register("home_page", homeNode, home);
        registry.register("detail", detailNode, detail);
        AtomicReference<Object> displayed = new AtomicReference<>();
        AtomicReference<Object> result = new AtomicReference<>();
        WindowNavigation navigation = new WindowNavigation(registry, displayed::set);

        navigation.navigate("home_page", "initial");
        navigation.navigate("detail", 42, result::set);

        assertSame(detailNode, displayed.get());
        assertEquals(List.of("initial"), home.parameters);
        assertEquals(List.of(42), detail.parameters);
        assertTrue(navigation.canGoBack());

        navigation.back("saved");

        assertSame(homeNode, displayed.get());
        assertEquals(List.of("initial", "initial"), home.parameters);
        assertEquals("saved", result.get());
        assertFalse(navigation.canGoBack());
    }

    @Test
    void replaceDiscardsCurrentPageAndPreservesEarlierHistory() {
        PageRegistry registry = new PageRegistry();
        TestPage home = new TestPage();
        TestPage detail = new TestPage();
        TestPage replacement = new TestPage();
        registry.register("home", new Rectangle(), home);
        registry.register("detail", new Rectangle(), detail);
        registry.register("replacement", new Rectangle(), replacement);
        AtomicReference<Node> displayed = new AtomicReference<>();
        WindowNavigation navigation = new WindowNavigation(registry, displayed::set);

        navigation.navigate("home", null);
        navigation.navigate("detail", null);
        navigation.replace("replacement", "new");

        assertTrue(navigation.canGoBack());
        navigation.back();
        assertEquals(2, home.parameters.size());
        assertNull(home.parameters.get(0));
        assertNull(home.parameters.get(1));
        assertFalse(navigation.canGoBack());
        assertThrows(IllegalStateException.class, () -> navigation.back());
    }

    @Test
    void reportsInvalidPageNamesAndRootBack() {
        PageRegistry registry = new PageRegistry();
        TestPage home = new TestPage();
        AtomicReference<Node> displayed = new AtomicReference<>();
        WindowNavigation navigation = new WindowNavigation(registry, displayed::set);

        assertThrows(IllegalArgumentException.class, () -> navigation.navigate("missing", null));
        assertThrows(IllegalStateException.class, navigation::back);
        Node homeNode = new Rectangle();
        registry.register("home", homeNode, home);
        navigation.navigate("home", "initial");
        assertThrows(IllegalArgumentException.class, () -> navigation.navigate("missing", null));
        assertSame(homeNode, displayed.get());
        assertFalse(navigation.canGoBack());
        assertThrows(IllegalStateException.class, () -> registry.register("home", new Rectangle(), new TestPage()));
    }

    @Test
    void factoryPagesAreIndependentAcrossWindows() {
        PageRegistry registry = new PageRegistry();
        AtomicInteger created = new AtomicInteger();
        registry.registerFactory("home", () -> new PageDefinition("home", new Rectangle(),
            new TestPage(created.incrementAndGet())));
        AtomicReference<Node> firstDisplayed = new AtomicReference<>();
        AtomicReference<Node> secondDisplayed = new AtomicReference<>();
        WindowNavigation first = new WindowNavigation(registry, firstDisplayed::set);
        WindowNavigation second = new WindowNavigation(registry, secondDisplayed::set);

        first.navigate("home", null);
        second.navigate("home", null);

        assertNotSame(firstDisplayed.get(), secondDisplayed.get());
        assertEquals(2, created.get());
    }

    @Test
    void failedNavigationLeavesCurrentPageAndHistoryUnchanged() {
        PageRegistry registry = new PageRegistry();
        TestPage home = new TestPage();
        TestPage failing = new TestPage();
        failing.failOnNavigate = true;
        Node homeNode = new Rectangle();
        registry.register("home", homeNode, home);
        registry.register("failing", new Rectangle(), failing);
        AtomicReference<Node> displayed = new AtomicReference<>();
        WindowNavigation navigation = new WindowNavigation(registry, displayed::set);
        navigation.navigate("home", null);

        assertThrows(IllegalStateException.class, () -> navigation.navigate("failing", null));
        assertSame(homeNode, displayed.get());
        assertFalse(navigation.canGoBack());
    }

    private static final class TestPage implements Page<Object> {
        private final List<Object> parameters = new ArrayList<>();
        private final int id;
        private boolean failOnNavigate;

        private TestPage() { this(0); }
        private TestPage(int id) { this.id = id; }

        @Override
        public void onNavigate(Object parameter) {
            if (failOnNavigate) throw new IllegalStateException("navigation failure");
            parameters.add(parameter);
        }
    }
}
