package org.a8043.cwaFX.annotationHandlers;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.FieldAccessor;
import org.a8043.cwaFX.Util;
import org.a8043.cwaFX.annotations.bean.*;
import org.a8043.cwaFX.events.*;
import org.a8043.cwaFX.userEvent.UserEventWrapper;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.ParameterizedType;

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
                    context.getBeans(clazz).forEach(bean -> initializeBean(clazz, bean, context));
                }
            }

            case DestroyEvent ignored -> context.getClasses().getMethods(clazz, PreDestroy.class).forEach(method ->
                context.getBeans(clazz).forEach(bean -> Util.invokeMethod(method, bean)));

            case NewBeanEvent newBean -> {
                if (newBean.getObject().getClass().equals(clazz)) {
                    injectDependency(clazz, context);
                    if (context.isInitializationComplete()) {
                        initializeBean(clazz, newBean.getObject(), context);
                    }
                }

                injectPendingDependencies(newBean, context);
            }

            case UserEventWrapper wrapper -> Util.getMethods(clazz, OnEvent.class).forEach(method -> {
                if (method.getAnnotation(OnEvent.class).value().equals(wrapper.getEvent().getClass())) {
                    context.getBeans(clazz).forEach(bean -> Util.invokeMethod(method, bean, wrapper.getEvent()));
                }
            });

            case KeyPressEvent key -> Util.getMethods(clazz, OnKeyPressed.class).forEach(method -> {
                if (method.getAnnotation(OnKeyPressed.class).value().equals(key.getName())) {
                    context.getBeans(clazz).forEach(bean -> Util.invokeMethod(method, bean));
                }
            });

            default -> {
            }
        }
    }

    private static void injectDependency(Class<?> clazz, AppContext context) {
        context.getClasses().getFields(clazz, Autowired.class).forEach(field ->
            context.getBeans(clazz).forEach(bean -> {
                String configuredName = field.getAnnotation(Autowired.class).name();
                String dependencyName = configuredName.isEmpty() ? field.getName() : configuredName;
                Object dependency = context.findBean(field.getType(), dependencyName);
                if (dependency == null && configuredName.isEmpty()
                    && !(field.getGenericType() instanceof ParameterizedType)) {
                    dependency = context.findBean(field.getType(), "");
                }
                if (dependency == null) {
                    log.debug("Inj fail: {}, ({})", dependencyName, field.getType());
                    context.getInjectionFailures().computeIfAbsent(new BeanKey(field.getType(), dependencyName),
                        k -> new ArrayList<>()).add(new FieldAccessor(field, bean));
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

    private static void injectPendingDependencies(NewBeanEvent newBean, AppContext context) {
        if (!newBean.getKey().getName().isEmpty()) {
            injectPendingDependencies(newBean.getKey(), newBean, context);
        }
        injectPendingDependencies(new BeanKey(newBean.getKey().getClazz(), ""), newBean, context);
    }

    private static void injectPendingDependencies(BeanKey key, NewBeanEvent newBean, AppContext context) {
        BeanKey matchingKey = context.getInjectionFailures().keySet().stream()
            .filter(candidate -> candidate.getClazz().equals(key.getClazz()))
            .filter(candidate -> key.getName().isEmpty()
                || candidate.getName().equalsIgnoreCase(key.getName()))
            .findFirst()
            .orElse(null);
        List<FieldAccessor> fields = matchingKey == null ? null : context.getInjectionFailures().get(matchingKey);
        if (fields == null) {
            return;
        }

        Object dependency;
        if (key.getName().isEmpty()) {
            List<FieldAccessor> typeFallbackFields = fields.stream()
                .filter(field -> !(field.getField().getGenericType() instanceof ParameterizedType))
                .toList();
            if (typeFallbackFields.isEmpty()) {
                return;
            }
            List<Object> beans = context.getBeansByKeyClass(key.getClazz());
            if (beans.size() != 1) {
                return;
            }
            dependency = beans.getFirst();
            matchingKey = context.getInjectionFailures().keySet().stream()
                .filter(candidate -> candidate.getClazz().equals(key.getClazz()))
                .findFirst()
                .orElse(null);
            if (matchingKey == null) {
                return;
            }
            fields = typeFallbackFields;
        } else {
            if (!key.getClazz().equals(newBean.getKey().getClazz())
                || !key.getName().equalsIgnoreCase(newBean.getKey().getName())) {
                return;
            }
            dependency = newBean.getObject();
        }

        context.getInjectionFailures().remove(matchingKey);
        fields.forEach(field -> field.set(dependency));
    }

    private static void initializeBean(Class<?> clazz, Object bean, AppContext context) {
        if (context.markBeanInitialized(bean)) {
            Util.getMethods(clazz, Initialize.class).forEach(method -> Util.invokeMethod(method, bean));
        }
    }

    @Override
    public Class<Bean> getType() {
        return Bean.class;
    }
}
