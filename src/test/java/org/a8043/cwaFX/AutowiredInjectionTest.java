package org.a8043.cwaFX;

import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import org.a8043.cwaFX.annotationHandlers.AnnotationHandler;
import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Initialize;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

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
    void doesNotInjectAnAmbiguousUnqualifiedDependencyWhenAnotherBeanIsAdded() {
        CwaFX cwaFX = new CwaFX(Consumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(Consumer.class,
            Arrays.stream(Consumer.class.getAnnotations()).toList());

        context.addBean(new BeanKey(Node.class, "first"), new Rectangle());
        context.addBean(new BeanKey(Node.class, "second"), new Rectangle());

        Consumer consumer = new Consumer();
        context.addBean(new BeanKey(Consumer.class, "consumer"), consumer);
        context.addBean(new BeanKey(Node.class, "third"), new Rectangle());

        assertNull(consumer.node);
    }

    @Test
    void prefersTheFieldNameWhenSeveralBeansOfTheSameTypeExist() {
        CwaFX cwaFX = new CwaFX(Consumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(Consumer.class,
            Arrays.stream(Consumer.class.getAnnotations()).toList());

        Node unrelated = new Rectangle();
        context.addBean(new BeanKey(Node.class, "anotherNode"), new Rectangle());
        context.addBean(new BeanKey(Node.class, "otherNode"), unrelated);
        Consumer consumer = new Consumer();
        context.addBean(new BeanKey(Consumer.class, "consumer"), consumer);

        assertNull(consumer.node);

        Node matching = new Rectangle();
        context.addBean(new BeanKey(Node.class, "node"), matching);

        assertSame(matching, consumer.node);
    }

    @Test
    void fallsBackToTheOnlyBeanWhenTheDefaultFieldNameDoesNotMatch() {
        CwaFX cwaFX = new CwaFX(LegacyConsumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(LegacyConsumer.class,
            Arrays.stream(LegacyConsumer.class.getAnnotations()).toList());

        Node onlyBean = new Rectangle();
        context.addBean(new BeanKey(Node.class, "WindowCreator"), onlyBean);
        LegacyConsumer consumer = new LegacyConsumer();
        context.addBean(new BeanKey(LegacyConsumer.class, "consumer"), consumer);

        assertSame(onlyBean, consumer.windows);
    }

    @Test
    void doesNotUseTypeFallbackForParameterizedDependencies() {
        CwaFX cwaFX = new CwaFX(GenericConsumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(GenericConsumer.class,
            Arrays.stream(GenericConsumer.class.getAnnotations()).toList());

        List<Object> unrelated = new ArrayList<>();
        unrelated.add(new Object());
        context.addBean(new BeanKey(List.class, "OtherServices"), unrelated);
        List<Object> matching = new ArrayList<>();
        context.addBean(new BeanKey(List.class, "Services"), matching);

        GenericConsumer consumer = new GenericConsumer();
        context.addBean(new BeanKey(GenericConsumer.class, "consumer"), consumer);

        assertSame(matching, consumer.services);
    }

    @Test
    void keepsParameterizedDependencyPendingUntilItsNamedBeanArrives() {
        CwaFX cwaFX = new CwaFX(GenericConsumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(GenericConsumer.class,
            Arrays.stream(GenericConsumer.class.getAnnotations()).toList());

        GenericConsumer consumer = new GenericConsumer();
        context.addBean(new BeanKey(GenericConsumer.class, "consumer"), consumer);

        List<Object> unrelated = new ArrayList<>();
        unrelated.add(new Object());
        context.addBean(new BeanKey(List.class, "OtherServices"), unrelated);
        assertNull(consumer.services);

        List<Object> matching = new ArrayList<>();
        context.addBean(new BeanKey(List.class, "Services"), matching);
        assertSame(matching, consumer.services);
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

    @Test
    void injectsAnInterfaceDependencyRegisteredAfterTheConsumer() {
        CwaFX cwaFX = new CwaFX(DelayedConsumer.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(DelayedConsumer.class,
            Arrays.stream(DelayedConsumer.class.getAnnotations()).toList());

        DelayedConsumer consumer = new DelayedConsumer();
        context.addBean(new BeanKey(DelayedConsumer.class, "consumer"), consumer);

        Dependency client = new DependencyImpl();
        context.addBean(new BeanKey(Dependency.class, "Client"), client);

        assertSame(client, consumer.client);
    }

    @Test
    void registersBeanBeforePublishingNewBeanEvent() throws ReflectiveOperationException {
        CwaFX cwaFX = new CwaFX(EventReentrantBean.class, new String[0]);
        AppContext context = cwaFX.getContext();
        context.getClasses().getClassMap().put(EventReentrantBean.class,
            Arrays.stream(EventReentrantBean.class.getAnnotations()).toList());

        AtomicReference<Object> observedBean = new AtomicReference<>();
        AnnotationHandler<Bean> handler = new AnnotationHandler<>() {
            @Override
            public void onEvent(Class<?> clazz, Bean annotation, Event event,
                                AppContext eventContext) {
                if (event instanceof NewBeanEvent && clazz == EventReentrantBean.class) {
                    observedBean.set(eventContext.getBean(EventReentrantBean.class, ""));
                }
            }

            @Override
            public Class<Bean> getType() {
                return Bean.class;
            }
        };

        List<AnnotationHandler<?>> handlers = annotationHandlers();
        handlers.add(handler);
        try {
            Object bean = context.getBean(EventReentrantBean.class, "");

            assertNotNull(bean);
            assertSame(bean, observedBean.get());
        } finally {
            handlers.remove(handler);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationHandler<?>> annotationHandlers() throws ReflectiveOperationException {
        Field field = CwaFX.class.getDeclaredField("ANNOTATION_HANDLERS");
        field.setAccessible(true);
        return (List<AnnotationHandler<?>>) field.get(null);
    }

    @Bean
    public static class Consumer {
        @Autowired
        private Node node;
    }

    @Bean
    public static class LegacyConsumer {
        @Autowired
        private Node windows;
    }

    @Bean
    public static class GenericConsumer {
        @Autowired
        private List<?> services;
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

    @Bean
    public static class DelayedConsumer {
        @Autowired
        private Dependency client;
    }

    private interface Dependency {
    }

    private static class DependencyImpl implements Dependency {
    }

    @Bean
    public static class EventReentrantBean {
    }
}
