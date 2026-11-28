package org.a8043.cwaFX.annotationHandlers;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.FieldAccessor;
import org.a8043.cwaFX.Util;
import org.a8043.cwaFX.annotations.bean.*;
import org.a8043.cwaFX.events.*;
import org.a8043.cwaFX.userEvent.UserEventWrapper;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Map;

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

            case DestroyEvent ignored -> destroyBeans(clazz, context);

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
            context.getBeans(clazz).forEach(bean -> injectField(field, bean, context)));
    }

    public static void preparePageBean(Class<?> clazz, Object bean, AppContext context) {
        for (Field field : context.getClasses().getFields(clazz, Autowired.class)) {
            Autowired annotation = field.getAnnotation(Autowired.class);
            boolean explicitName = !annotation.name().isEmpty();
            String name = explicitName ? annotation.name() : field.getName();
            Object dependency = context.findBeanExact(field.getType(), name);
            if (dependency == null && !explicitName && !(field.getGenericType() instanceof ParameterizedType)) {
                dependency = context.findBean(field.getType(), "");
            }
            if (dependency == null) {
                throw new IllegalStateException("Unresolved @Autowired page dependency "
                                                + clazz.getName() + "." + field.getName() + " named '" + name + "'");
            }
            new FieldAccessor(field, bean).set(dependency);
        }
        Util.getMethods(clazz, Initialize.class).forEach(method -> Util.invokeMethod(method, bean));
    }

    public static void destroyPageBean(Class<?> clazz, Object bean) {
        Util.getMethods(clazz, PreDestroy.class).forEach(method -> Util.invokeMethod(method, bean));
    }

    private static void injectField(Field field, Object bean, AppContext context) {
        Autowired annotation = field.getAnnotation(Autowired.class);
        boolean explicitName = !annotation.name().isEmpty();
        String dependencyName = explicitName ? annotation.name() : field.getName();
        FieldAccessor accessor = new FieldAccessor(field, bean);
        Object dependency = context.findBeanExact(field.getType(), dependencyName);
        boolean allowTypeFallback = !explicitName && !(field.getGenericType() instanceof ParameterizedType);
        if (dependency == null && allowTypeFallback) {
            dependency = context.findBean(field.getType(), "");
        }
        if (dependency == null) {
            log.debug("Injection pending: {}.{} ({})", bean.getClass().getName(), field.getName(), field.getType());
            context.recordInjectionFailure(accessor, dependencyName, explicitName, allowTypeFallback);
            return;
        }
        accessor.set(dependency);
        context.clearInjectionFailure(accessor);
    }

    private static void injectPendingDependencies(NewBeanEvent newBean, AppContext context) {
        for (Map.Entry<FieldAccessor, AppContext.PendingDependency> entry : context.pendingInjections().entrySet()) {
            FieldAccessor accessor = entry.getKey();
            AppContext.PendingDependency pending = entry.getValue();
            Field field = accessor.getField();
            if (!newBean.getKey().getClazz().equals(field.getType())) continue;

            boolean exactName = newBean.getKey().getName().equalsIgnoreCase(pending.name());
            Object dependency = exactName ? newBean.getObject() : null;
            if (dependency == null && pending.typeFallback()) {
                dependency = context.findBean(field.getType(), "");
            }
            if (dependency != null) {
                accessor.set(dependency);
                context.clearInjectionFailure(accessor);
            }
        }
    }

    private static void initializeBean(Class<?> clazz, Object bean, AppContext context) {
        if (context.markBeanInitialized(bean)) {
            Util.getMethods(clazz, Initialize.class).forEach(method -> Util.invokeMethod(method, bean));
        }
    }

    private static void destroyBeans(Class<?> clazz, AppContext context) {
        Throwable failure = null;
        for (java.lang.reflect.Method method : context.getClasses().getMethods(clazz, PreDestroy.class)) {
            for (Object bean : context.getBeans(clazz)) {
                try {
                    Util.invokeMethod(method, bean);
                } catch (RuntimeException | Error destroyFailure) {
                    if (failure == null) failure = destroyFailure;
                    else failure.addSuppressed(destroyFailure);
                }
            }
        }
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new IllegalStateException("One or more bean cleanup callbacks failed", failure);
    }

    @Override
    public Class<Bean> getType() {
        return Bean.class;
    }
}
