package org.a8043.cwaFX;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.stream.StreamUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotationHandlers.AnnotationHandler;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;
import org.a8043.cwaFX.tasks.Tasks;
import org.a8043.cwaFX.userEvent.EventPublisher;
import org.a8043.cwaFX.window.WindowCreator;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class CwaFX {
    private static final List<AnnotationHandler<?>> ANNOTATION_HANDLERS = new ArrayList<>();

    static {
        ServiceLoader<AnnotationHandler> annotationHandlers = ServiceLoader.load(AnnotationHandler.class);
        annotationHandlers.forEach(ANNOTATION_HANDLERS::add);
    }

    @Getter
    private static CwaFX instance;

    public static void start(Class<?> clazz, String[] args) {
        (instance = new CwaFX(clazz, args)).startApp();
    }

    @Getter
    private final Class<?> clazz;
    @Getter
    private final AppContext context;
    @Getter
    private final AppConfig config = new AppConfig();
    @Getter(AccessLevel.PACKAGE)
    private final CountDownLatch fxLoadLatch = new CountDownLatch(1);

    public CwaFX(Class<?> clazz, String[] args) {
        this.clazz = clazz;
        context = new AppContext(this, args);
    }

    private void startApp() {
        log.info("Starting application with class: {}", clazz.getName());
        
        try {
            BeanUtil.fillBeanWithMap(new JSONObject(IoUtil.readUtf8(clazz.getResource("/app.json").openStream())),
                config, true);
        } catch (Exception e) {
            log.error("Error reading app.json configuration file.", e);
            return;
        }

        context.addBean(new BeanKey(CwaFX.class, "CwaFX"), this);
        context.addBean(new BeanKey(AppContext.class, "AppContext"), context);
        context.addBean(new BeanKey(WindowCreator.class, "WindowCreator"), new WindowCreator(context));
        context.addBean(new BeanKey(EventPublisher.class, "EventPublisher"), new EventPublisher(this));

        log.info("Starting JavaFX application...");
        new Thread(() -> FXApp.launch(FXApp.class, context.getArgs())).start();
        try {
            fxLoadLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        context.addBean(new BeanKey(FXApp.class, "FXApp"), FXApp.getInstance());
        context.addBean(new BeanKey(Tasks.class, "Tasks"), new Tasks());

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

        log.info("Done starting application.");
    }

    public void notifyEvent(Event event) {
        log.debug("Notifying event: {}", event);
        context.getClasses().getClassMap().forEach((clazz, annotations) -> annotations.forEach(annotation ->
            ANNOTATION_HANDLERS.stream()
                .filter(handler -> handler.getType().isAssignableFrom(annotation.annotationType()))
                .map(handler -> (AnnotationHandler<Annotation>) handler)
                .forEach(handler -> handler.onEvent(clazz, annotation, event, context))));

        if (event instanceof InitEvent init && init.getSequence() == 2) {
            context.completeInitialization();
        }
    }
}
