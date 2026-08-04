package org.a8043.cwaFX;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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

    @Bean
    public static class Consumer {
        @Autowired
        private Node node;
    }
}
