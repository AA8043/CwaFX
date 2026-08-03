package org.a8043.cwaFX.annotationHandlers;

import com.google.auto.service.AutoService;
import lombok.SneakyThrows;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.annotations.bean.Autowired;
import org.a8043.cwaFX.annotations.bean.Bean;
import org.a8043.cwaFX.annotations.bean.PreDestroy;
import org.a8043.cwaFX.events.DestroyEvent;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.lang.reflect.InvocationTargetException;

@AutoService(AnnotationHandler.class)
public class BeanHandler implements AnnotationHandler<Bean> {
    @SneakyThrows
    @Override
    public void onEvent(Class<?> clazz, Bean annotation, Event event, AppContext context) {
        switch (event) {
            case InitEvent init -> {
                if (init.getSequence() == 0 && annotation.single()) {
                    context.getBeans().put(new BeanKey(clazz, clazz.getSimpleName()),
                        clazz.getConstructor().newInstance());
                } else if (init.getSequence() == 1) {
                    context.getClasses().getFields(clazz, Autowired.class).forEach(field ->
                        context.getBeans(clazz).forEach(bean -> {
                            String name = field.getAnnotation(Autowired.class).name();
                            Object dependency = context.getBeans().get(new BeanKey(field.getType(),
                                name.isEmpty() ? field.getType().getSimpleName() : name));
                            field.setAccessible(true);
                            try {
                                field.set(bean, dependency);
                            } catch (IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                        }));
                }
            }

            case DestroyEvent ignored -> context.getClasses().getMethods(clazz, PreDestroy.class).forEach(method ->
                context.getBeans(clazz).forEach(bean -> {
                    method.setAccessible(true);
                    try {
                        method.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }));

            default -> {
            }
        }
    }

    @Override
    public Class<Bean> getType() {
        return Bean.class;
    }
}
