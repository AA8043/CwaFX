package org.a8043.cwaFX.annotationHandlers;

import com.google.auto.service.AutoService;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.annotations.bean.Service;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.util.ArrayList;
import java.util.List;

@AutoService(AnnotationHandler.class)
public class ServiceHandler implements AnnotationHandler<Service> {
    @Override
    public void onEvent(Class<?> clazz, Service annotation, Event event, AppContext context) {
        if (!(event instanceof InitEvent init) || init.getSequence() != 1) {
            return;
        }

        Class<?>[] interfaces = clazz.getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalStateException("Service class must implement at least one interface: " + clazz.getName());
        }

        List<Object> implementations = context.getBeans(clazz);
        for (Class<?> serviceInterface : interfaces) {
            String name = serviceInterface.getSimpleName() + "Services";
            List<Object> services = context.findBean(List.class, name);
            if (services == null) {
                services = new ArrayList<>();
                context.addBean(new BeanKey(List.class, name), services);
            }
            services.addAll(implementations);
        }
    }

    @Override
    public Class<Service> getType() {
        return Service.class;
    }
}
