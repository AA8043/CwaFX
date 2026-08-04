package org.a8043.cwaFX.annotationHandlers;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.FieldAccessor;
import org.a8043.cwaFX.Util;
import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.Initialize;
import org.a8043.cwaFX.annotations.bean.PreDestroy;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;
import org.a8043.cwaFX.events.NewBeanEvent;

@Slf4j
@AutoService(AnnotationHandler.class)
public class BeanHandler implements AnnotationHandler<Bean> {
    @SneakyThrows
    @Override
    public void onEvent(Class<?> clazz, Bean annotation, Event event, AppContext context) {
        switch (event) {
            case InitEvent init -> {
                if (init.getSequence() == 0 && annotation.single()) {
                    context.getBean(clazz, "");
                } else if (init.getSequence() == 1) {
                    injectDependency(clazz, context);
                } else if (init.getSequence() == 2) {
                    Util.getMethods(clazz, Initialize.class).forEach(method ->
                        context.getBeans(clazz).forEach(bean -> Util.invokeMethod(method, bean)));
                }
            }

            case DestroyEvent ignored -> context.getClasses().getMethods(clazz, PreDestroy.class).forEach(method ->
                context.getBeans(clazz).forEach(bean -> Util.invokeMethod(method, bean)));

            case NewBeanEvent newBean -> {
                if (newBean.getObject().getClass().equals(clazz)) {
                    injectDependency(clazz, context);
                }

                if (context.getInjectionFailures().containsKey(newBean.getKey())) {
                    FieldAccessor field = context.getInjectionFailures().remove(newBean.getKey());
                    field.set(newBean.getObject());
                }
            }

            default -> {
            }
        }
    }

    private static void injectDependency(Class<?> clazz, AppContext context) {
        context.getClasses().getFields(clazz, Autowired.class).forEach(field ->
            context.getBeans(clazz).forEach(bean -> {
                String configuredName = field.getAnnotation(Autowired.class).name();
                String dependencyName = configuredName.isEmpty() ? field.getName() : configuredName;
                Object dependency = context.getBean(field.getType(), configuredName);
                if (dependency == null) {
                    log.debug("Inj fail: {}, ({})", dependencyName, field.getType());
                    context.getInjectionFailures().put(new BeanKey(field.getType(), dependencyName),
                        new FieldAccessor(field, bean));
                    return;
                }

                field.setAccessible(true);
                try {
                    field.set(bean, dependency);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }));
    }

    @Override
    public Class<Bean> getType() {
        return Bean.class;
    }
}
