package org.a8043.cwaFX;

import javafx.application.Platform;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Provides a static facade for interacting with the active CwaFX application.
 */
@Getter
public class CwaPlatform {
    private static CwaFX cwa;
    private static AppContext context;
    private static volatile boolean fxAvailable;

    static void setCwa(CwaFX cwa) {
        CwaPlatform.cwa = cwa;
        context = cwa.getContext();
        fxAvailable = false;
    }

    static void markFxAvailable() {
        fxAvailable = true;
    }

    static void markFxUnavailable() {
        fxAvailable = false;
    }

    /**
     * Get a bean from the application context.<br>
     * If the bean does not exist, it will be created with the provided arguments.
     *
     * @param clazz the class of the bean to retrieve or create
     * @param args  the arguments to pass to the bean's constructor if it needs to be created
     * @param <T>   the type of the bean
     * @return the bean instance
     */
    public static <T> T getBean(Class<T> clazz, Object... args) {
        return getBean(UUID.randomUUID().toString(), clazz, args);
    }

    /**
     * Get a bean from the application context.<br>
     * If the bean does not exist, it will be created with the provided arguments.
     *
     * @param name  the name of the bean to retrieve or create
     * @param clazz the class of the bean to retrieve or create
     * @param args  the arguments to pass to the bean's constructor if it needs to be created
     * @param <T>   the type of the bean
     * @return the bean instance
     */
    public static <T> T getBean(String name, Class<T> clazz, Object... args) {
        return context.getBean(clazz, name, args);
    }

    /**
     * Run a task on the JavaFX Application Thread.<br>
     * If the current thread is the JavaFX Application Thread, the task will be executed immediately.
     *
     * @param task the task to run on the JavaFX Application Thread
     */
    public static void runOnFxThread(Runnable task) {
        if (!fxAvailable && !Platform.isFxApplicationThread()) {
            throw new IllegalStateException("The JavaFX application is not running.");
        }
        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    /**
     * Run a task on the JavaFX Application Thread and return the result.<br>
     * This method blocks until the task is completed.<br>
     * If the current thread is the JavaFX Application Thread, the task will be executed immediately.
     *
     * @param task the task to run on the JavaFX Application Thread
     * @param <R>  the type of the result
     * @return the result of the task
     */
    public static <R> R runOnFxThread(Supplier<R> task) {
        if (Platform.isFxApplicationThread()) {
            return task.get();
        }
        if (!fxAvailable) {
            throw new IllegalStateException("The JavaFX application is not running.");
        }
        CompletableFuture<R> result = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    result.complete(task.get());
                } catch (Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            });
        } catch (IllegalStateException exception) {
            throw new IllegalStateException("Unable to schedule work on the JavaFX application thread", exception);
        }
        CwaFX activeApp = cwa;
        try {
            if (activeApp == null) return result.get();
            CompletableFuture.anyOf(result, activeApp.getFxStopped()).get();
            if (!result.isDone()) throw new IllegalStateException("JavaFX stopped before the queued task ran.");
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for the JavaFX application thread", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        }
    }

    /**
     * Exit the application.
     */
    public static void exit() {
        Platform.exit();
    }
}
