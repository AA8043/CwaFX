package org.a8043.cwaFX;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.AbstractConverter;
import cn.hutool.core.convert.ConverterRegistry;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.json.JSONObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.annotationHandlers.AnnotationHandler;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.InitEvent;
import org.a8043.cwaFX.keyMapping.KeyMappings;
import org.a8043.cwaFX.navigation.PageRegistry;
import org.a8043.cwaFX.tasks.Tasks;
import org.a8043.cwaFX.userEvent.EventPublisher;
import org.a8043.cwaFX.userEvent.UserEventWrapper;
import org.a8043.cwaFX.window.WindowCreator;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    private final AtomicReference<Throwable> fxLoadFailure = new AtomicReference<>();
    private final CompletableFuture<Void> fxStopped = new CompletableFuture<>();
    private final List<Consumer<Event>> onEventList = new ArrayList<>();
    private final List<org.a8043.cwaFX.userEvent.Event> pendingUserEvents = new ArrayList<>();
    private final Set<String> corruptionEvents = new HashSet<>();
    private boolean startupEventsDispatched;

    public CwaFX(Class<?> clazz, String[] args) {
        this.clazz = clazz;
        context = new AppContext(this, args);
        CwaPlatform.setCwa(this);
    }

    private void startApp() {
        log.info("Starting application with class: {}", clazz.getName());

        try {
            BeanUtil.fillBeanWithMap(new JSONObject(IoUtil.readUtf8(clazz.getResource("/app.json").openStream())),
                config, true);
        } catch (Exception e) {
            log.error("Error reading app.json configuration file.", e);
            throw new IllegalStateException("Unable to read app.json configuration", e);
        }

        context.addBean(new BeanKey(CwaFX.class, "CwaFX"), this);
        context.addBean(new BeanKey(AppContext.class, "AppContext"), context);
        context.addBean(new BeanKey(WindowCreator.class, "WindowCreator"), new WindowCreator(context));
        context.addBean(new BeanKey(PageRegistry.class, "PageRegistry"), new PageRegistry());
        context.addBean(new BeanKey(EventPublisher.class, "EventPublisher"), new EventPublisher(this));
        context.addBean(new BeanKey(KeyMappings.class, "KeyMappings"), new KeyMappings(this));

        log.info("Starting JavaFX application...");
        Thread fxThread = new Thread(() -> {
            try {
                FXApp.launch(FXApp.class, context.getArgs());
            } catch (Throwable throwable) {
                signalFxLoadFailure(throwable);
            }
        }, "cwafx-javafx-launcher");
        fxThread.start();
        try {
            if (!fxLoadLatch.await(30, TimeUnit.SECONDS)) {
                IllegalStateException timeout = new IllegalStateException("Timed out waiting for JavaFX to start");
                signalFxLoadFailure(timeout);
                try {
                    javafx.application.Platform.exit();
                } catch (IllegalStateException exitFailure) {
                    timeout.addSuppressed(exitFailure);
                }
                throw timeout;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for JavaFX to start", e);
        }
        Throwable startupFailure = fxLoadFailure.get();
        if (startupFailure != null) {
            if (startupFailure instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IllegalStateException("Unable to start JavaFX", startupFailure);
        }
        try {
            context.addBean(new BeanKey(FXApp.class, "FXApp"), FXApp.getInstance());
            context.addBean(new BeanKey(Tasks.class, "Tasks"), new Tasks(this));

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

            dispatchPendingUserEvents();

            log.info("Done starting application.");
        } catch (RuntimeException | Error failure) {
            javafx.application.Platform.exit();
            throw failure;
        }
    }

    public void notifyEvent(Event event) {
        log.debug("Notifying event: {}", event);
        boolean destroying = event instanceof org.a8043.cwaFX.events.DestroyEvent;
        List<Throwable> failures = new ArrayList<>();
        context.getClasses().getClassMap().forEach((clazz, annotations) -> annotations.forEach(annotation ->
            ANNOTATION_HANDLERS.stream()
                .filter(handler -> handler.getType().isAssignableFrom(annotation.annotationType()))
                .map(handler -> (AnnotationHandler<Annotation>) handler)
                .forEach(handler -> runEventCallback(destroying, failures,
                    () -> handler.onEvent(clazz, annotation, event, context)))));
        onEventList.forEach(callback -> runEventCallback(destroying, failures, () -> callback.accept(event)));

        if (event instanceof InitEvent init && init.getSequence() == 2) {
            context.assertNoPendingInjections();
            context.completeInitialization();
        }
        if (destroying && !failures.isEmpty()) throw aggregateFailures(failures);
    }

    private static void runEventCallback(boolean continueAfterFailure, List<Throwable> failures, Runnable callback) {
        if (!continueAfterFailure) {
            callback.run();
            return;
        }
        try {
            callback.run();
        } catch (RuntimeException | Error failure) {
            failures.add(failure);
        }
    }

    private static RuntimeException aggregateFailures(List<Throwable> failures) {
        Throwable first = failures.getFirst();
        for (int index = 1; index < failures.size(); index++) first.addSuppressed(failures.get(index));
        if (first instanceof Error error) throw error;
        return new IllegalStateException("One or more application shutdown callbacks failed", first);
    }

    void signalFxLoaded() {
        CwaPlatform.markFxAvailable();
        fxLoadLatch.countDown();
    }

    void signalFxLoadFailure(Throwable throwable) {
        fxLoadFailure.compareAndSet(null, throwable);
        CwaPlatform.markFxUnavailable();
        fxLoadLatch.countDown();
    }

    Throwable getFxLoadFailure() {
        return fxLoadFailure.get();
    }

    boolean isFxStartupPending() {
        return fxLoadLatch.getCount() != 0;
    }

    CompletableFuture<Void> getFxStopped() {
        return fxStopped;
    }

    void signalFxStopped() {
        CwaPlatform.markFxUnavailable();
        fxStopped.complete(null);
    }

    public synchronized void publishUserEvent(org.a8043.cwaFX.userEvent.Event event) {
        if (event instanceof ConfigurationCorruptedEvent corrupted) {
            String path = corrupted.getFile().toPath().toAbsolutePath().normalize().toString();
            if (!corruptionEvents.add(path)) return;
        }
        if (!startupEventsDispatched) {
            pendingUserEvents.add(event);
            return;
        }
        notifyEvent(new UserEventWrapper(event));
    }

    private synchronized void dispatchPendingUserEvents() {
        startupEventsDispatched = true;
        List<org.a8043.cwaFX.userEvent.Event> events = List.copyOf(pendingUserEvents);
        pendingUserEvents.clear();
        events.forEach(event -> notifyEvent(new UserEventWrapper(event)));
    }

    public void addOnEvent(Consumer<Event> onEvent) {
        onEventList.add(onEvent);
    }

    static {
        String userHome = System.getProperty("user.home");

        ConverterRegistry.getInstance().putCustom(File.class, new AbstractConverter<File>() {
            @Override
            protected File convertInternal(Object value) {
                if (value instanceof String str) {
                    return new File(str.replace("~", userHome));
                }
                return null;
            }
        });
    }
}
