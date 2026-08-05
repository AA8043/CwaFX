package org.a8043.cwaFX;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Initialize;
import org.a8043.cwaFX.events.InitEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AutowiredInjectionTest {
    @Test
    void injectsSingleExistingBeanByTypeWhenNameIsOmitted() {
        CwaFX cwaFX = new CwaFX(Consumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(Consumer.class,
            Arrays.stream(Consumer.class.getAnnotations()).toList());

        Node node = new Rectangle();
        context.addBean(new BeanKey(Node.class, "node"), node);

        Consumer consumer = new Consumer();
        context.addBean(new BeanKey(Consumer.class, "consumer"), consumer);

        assertSame(node, consumer.node);
    }

    @Test
    void initializesBeanCreatedAfterApplicationStartup() {
        CwaFX cwaFX = new CwaFX(InitializedConsumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(InitializedConsumer.class,
            Arrays.stream(InitializedConsumer.class.getAnnotations()).toList());
        cwaFX.notifyEvent(new InitEvent(2));

        Node node = new Rectangle();
        context.addBean(new BeanKey(Node.class, "node"), node);
        InitializedConsumer consumer = new InitializedConsumer();
        context.addBean(new BeanKey(InitializedConsumer.class, "consumer"), consumer);

        assertSame(node, consumer.node);
        assertSame(node, consumer.initializedNode);
        assertEquals(1, consumer.initializeCount);
    }

    @Bean
    public static class Consumer {
        @Autowired
        private Node node;
    }

    @Bean
    public static class InitializedConsumer {
        @Autowired
        private Node node;
        private Node initializedNode;
        private int initializeCount;

        @Initialize
        private void initialize() {
            initializedNode = node;
            initializeCount++;
        }
    }
}
