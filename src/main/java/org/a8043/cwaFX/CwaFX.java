package org.a8043.cwaFX;

import cn.hutool.core.util.ClassUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotationHandlers.AnnotationHandler;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;

import java.lang.annotation.Annotation;
import java.util.*;

@Slf4j
public class CwaFX {
    private static final List<AnnotationHandler<?>> ANNOTATION_HANDLERS = new ArrayList<>();

    static {
        ServiceLoader<AnnotationHandler> annotationHandlers = ServiceLoader.load(AnnotationHandler.class);
        annotationHandlers.forEach(ANNOTATION_HANDLERS::add);
    }

    public static void start(Class<?> clazz, String[] args) {
        new CwaFX(clazz, args).startApp();
    }

    private final Class<?> clazz;
    @Getter
    private final AppContext context;

    public CwaFX(Class<?> clazz, String[] args) {
        this.clazz = clazz;
        context = new AppContext(this, args);
    }

    private void startApp() {
        log.info("Starting application with class: {}", clazz.getName());

        log.info("Starting JavaFX application...");
        new Thread(() -> FXApp.launch(FXApp.class, context.getArgs())).start();

        log.info("Scanning classes...");
        ClassUtil.scanPackage(clazz.getPackageName()).forEach(c ->
            context.getClasses().getClassMap().put(c, Arrays.stream(c.getAnnotations()).toList()));

        log.info("Initializing for the first time...");
        notifyEvent(new InitEvent(0));

        log.info("Initializing for the second time...");
        notifyEvent(new InitEvent(1));

        log.info("Initializing for the third time...");
        notifyEvent(new InitEvent(2));

        log.info("Initializing beans...");
        notifyEvent(new InitEvent(3));
    }

    private void notifyEvent(Event event) {
        context.getClasses().getClassMap().forEach((clazz, annotations) -> annotations.forEach(annotation ->
            ANNOTATION_HANDLERS.stream()
                .filter(handler -> handler.getType().isAssignableFrom(annotation.annotationType()))
                .map(handler -> (AnnotationHandler<Annotation>) handler)
                .forEach(handler -> handler.onEvent(clazz, annotation, event, context))));
    }
}
