package org.a8043.cwaFX;

import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Service;
import org.a8043.cwaFX.events.InitEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceHandlerTest {
    @Test
    void aggregatesRegisteredImplementationsAndInjectsTheirInterfaceList() {
        CwaFX cwaFX = new CwaFX(ServiceHandlerTest.class, new String[0]);
        AppContext context = cwaFX.getContext();
        register(context, FirstGreetingService.class, SecondGreetingService.class, GreetingConsumer.class);

        FirstGreetingService first = new FirstGreetingService();
        SecondGreetingService second = new SecondGreetingService();
        GreetingConsumer consumer = new GreetingConsumer();
        context.addBean(new BeanKey(FirstGreetingService.class, "firstGreetingService"), first);
        context.addBean(new BeanKey(SecondGreetingService.class, "secondGreetingService"), second);
        context.addBean(new BeanKey(GreetingConsumer.class, "greetingConsumer"), consumer);

        cwaFX.notifyEvent(new InitEvent(1));

        List<?> services = context.getBean(List.class, "GreetingServiceServices");
        assertEquals(2, services.size());
        assertTrue(services.containsAll(List.of(first, second)));
        assertSame(services, consumer.services);
    }

    @Test
    void registersEachDirectInterfaceListAndCreatesEmptyLists() {
        CwaFX cwaFX = new CwaFX(ServiceHandlerTest.class, new String[0]);
        AppContext context = cwaFX.getContext();
        register(context, MultiService.class, EmptyService.class);

        MultiService service = new MultiService();
        context.addBean(new BeanKey(MultiService.class, "multiService"), service);

        cwaFX.notifyEvent(new InitEvent(1));

        assertEquals(List.of(service), context.getBean(List.class, "GreetingServiceServices"));
        assertEquals(List.of(service), context.getBean(List.class, "AuditableServices"));
        assertTrue(context.getBean(List.class, "EmptyServiceContractServices").isEmpty());
    }

    @Test
    void rejectsServicesWithoutDirectInterfacesDuringSecondInitialization() {
        CwaFX cwaFX = new CwaFX(ServiceHandlerTest.class, new String[0]);
        AppContext context = cwaFX.getContext();
        register(context, InvalidService.class);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> cwaFX.notifyEvent(new InitEvent(1)));

        assertTrue(exception.getMessage().contains(InvalidService.class.getName()));
    }

    private static void register(AppContext context, Class<?>... classes) {
        Arrays.stream(classes).forEach(clazz -> context.getClasses().getClassMap().put(clazz,
            Arrays.stream(clazz.getAnnotations()).toList()));
    }

    interface GreetingService {
    }

    interface Auditable {
    }

    interface EmptyServiceContract {
    }

    @Bean
    @Service
    static class FirstGreetingService implements GreetingService {
    }

    @Bean
    @Service
    static class SecondGreetingService implements GreetingService {
    }

    @Bean
    @Service
    static class MultiService implements GreetingService, Auditable {
    }

    @Service
    static class EmptyService implements EmptyServiceContract {
    }

    @Service
    static class InvalidService {
    }

    @Bean
    static class GreetingConsumer {
        @Autowired(name = "GreetingServiceServices")
        private List<?> services;
    }
}
