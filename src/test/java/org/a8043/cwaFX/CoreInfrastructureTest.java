package org.a8043.cwaFX;

import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Initialize;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.a8043.cwaFX.userEvent.Event;
import org.a8043.cwaFX.userEvent.EventPublisher;
import org.a8043.cwaFX.userEvent.UserEventWrapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CoreInfrastructureTest {
    @Test
    void beanKeyNormalizesNamesAndUsesBothFieldsForEquality() {
        BeanKey normalized = new BeanKey(String.class, "primary_service");

        assertEquals("primaryService", normalized.getName());
        assertEquals(new BeanKey(String.class, "primaryService"), normalized);
    }

    @Test
    void contextCreatesAndCachesSingletonBeansThenPublishesTheirCreation() {
        CwaFX cwaFX = new CwaFX(CoreInfrastructureTest.class, new String[0]);
        AppContext context = cwaFX.getContext();
        AtomicReference<NewBeanEvent> event = new AtomicReference<>();
        cwaFX.addOnEvent(received -> {
            if (received instanceof NewBeanEvent newBean) {
                event.set(newBean);
            }
        });

        PublicBean first = context.getBean(PublicBean.class, "");
        PublicBean second = context.getBean(PublicBean.class, "");

        assertNotNull(first);
        assertSame(first, second);
        assertSame(first, context.getBean(PublicBean.class, ""));
        assertNotNull(event.get());
        assertSame(first, event.get().getObject());
        assertEquals(new BeanKey(PublicBean.class, PublicBean.class.getSimpleName()), event.get().getKey());
    }

    @Test
    void contextReturnsNullWhenItCannotConstructABean() {
        AppContext context = new CwaFX(CoreInfrastructureTest.class, new String[0]).getContext();

        assertNull(context.getBean(PrivateConstructorBean.class, ""));
    }

    @Test
    void classesFindsRegisteredAnnotationsAndDeclaredMembers() {
        Classes classes = new Classes();
        classes.getClassMap().put(AnnotatedComponent.class,
            Arrays.stream(AnnotatedComponent.class.getAnnotations()).toList());

        assertNotNull(classes.getAnnotation(AnnotatedComponent.class, Bean.class));
        assertNull(classes.getAnnotation(PublicBean.class, Bean.class));
        assertEquals(1, classes.getFields(AnnotatedComponent.class, Autowired.class).size());
        assertEquals(1, classes.getMethods(AnnotatedComponent.class, Initialize.class).size());
    }

    @Test
    void eventPublisherWrapsApplicationEventsBeforeNotification() {
        CwaFX cwaFX = new CwaFX(CoreInfrastructureTest.class, new String[0]);
        AtomicReference<UserEventWrapper> received = new AtomicReference<>();
        cwaFX.addOnEvent(event -> {
            if (event instanceof UserEventWrapper wrapper) {
                received.set(wrapper);
            }
        });
        SampleUserEvent original = new SampleUserEvent();

        new EventPublisher(cwaFX).publish(original);

        assertNotNull(received.get());
        assertSame(original, received.get().getEvent());
    }

    public static class PublicBean {
    }

    public static class PrivateConstructorBean {
        private PrivateConstructorBean() {
        }
    }

    @Bean
    static class AnnotatedComponent {
        @Autowired
        private PublicBean dependency;

        @Initialize
        private void initialize() {
        }
    }

    static class SampleUserEvent extends Event {
    }
}
