package org.a8043.cwaFX;

import javafx.application.Platform;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Provides a static facade for interacting with the active CwaFX application.
 */
@Getter
public class CwaPlatform {
    private static CwaFX cwa;
    private static AppContext context;

    static void setCwa(CwaFX cwa) {
        CwaPlatform.cwa = cwa;
        context = cwa.getContext();
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
        } else {
            AtomicReference<R> result = new AtomicReference<>();
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    result.set(task.get());
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            return result.get();
        }
    }

    /**
     * Exit the application.
     */
    public static void exit() {
        Platform.exit();
    }
}
