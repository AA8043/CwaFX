package org.a8043.cwaFX.annotationHandlers;

import javafx.application.Platform;
import javafx.scene.Node;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.CwaFX;
import org.a8043.cwaFX.annotations.view.FxmlView;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlViewHandlerTest {
    @BeforeAll
    static void startToolkit() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(started::countDown);
            assertTrue(started.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void loadsFxmlWithoutWaitingForTheFxThreadToRunLater() throws Exception {
        CwaFX cwaFX = new CwaFX(TestView.class, new String[0]);
        AppContext context = cwaFX.getContext();
        TestView controller = new TestView();
        FxmlViewHandler handler = new FxmlViewHandler();
        FutureTask<Void> task = new FutureTask<>(() -> {
            handler.onEvent(TestView.class, TestView.class.getAnnotation(FxmlView.class),
                new NewBeanEvent(new BeanKey(TestView.class, "TestView"), controller), context);
            return null;
        });

        Platform.runLater(task);

        task.get(5, TimeUnit.SECONDS);
        assertNotNull(context.getBean(Node.class, "TestViewNode"));
    }

    @Test
    void usesTheNamedPrototypeBeanToNameItsViewNode() throws Exception {
        CwaFX cwaFX = new CwaFX(TestView.class, new String[0]);
        AppContext context = cwaFX.getContext();
        FxmlViewHandler handler = new FxmlViewHandler();
        BeanKey key = new BeanKey(TestView.class, "session-42");

        handler.onEvent(TestView.class, TestView.class.getAnnotation(FxmlView.class),
            new NewBeanEvent(key, new TestView()), context);

        assertNotNull(context.getBean(Node.class, key.getName() + "Node"));
    }

    @FxmlView(fxml = "org/a8043/cwaFX/annotationHandlers/FxmlViewHandlerTest.fxml")
    public static class TestView {
    }
}
