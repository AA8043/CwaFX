package org.a8043.cwaFX.tasks;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.CwaPlatform;
import org.a8043.cwaFX.CwaFX;
import org.a8043.cwaFX.events.DestroyEvent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class that manages the execution of tasks in a thread pool.
 */
@Slf4j
public class Tasks implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final boolean cancelOnShutdown;
    private final AtomicBoolean closed = new AtomicBoolean();
    @Getter
    private final ObservableList<Task<?>> tasks = FXCollections.observableArrayList();

    public Tasks(CwaFX cwaFX) {
        int threadCount = cwaFX.getConfig().getTaskThreadPoolSize();
        executor = new ThreadPoolExecutor(threadCount, threadCount,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        cancelOnShutdown = cwaFX.getConfig().isCancelTasksOnShutdown();
        cwaFX.addOnEvent(event -> {
            if (event instanceof DestroyEvent) close();
        });
    }

    /**
     * Executes a task in the thread pool and adds it to the list of tasks.
     * @param task the task to execute
     */
    public void executeTask(Task<?> task) {
        if (closed.get()) throw new RejectedExecutionException("The task manager is closed.");
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, e -> removeTask(task));
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, e -> {
            log.error("Task failed: {}", task.getTitle(), task.getException());
            removeTask(task);
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, e -> removeTask(task));
        boolean tracked = CwaPlatform.runOnFxThread(() -> {
            if (closed.get()) return false;
            tasks.add(task);
            return true;
        });
        if (!tracked) throw new RejectedExecutionException("The task manager is closed.");
        try {
            executor.execute(task);
        } catch (RejectedExecutionException exception) {
            removeTask(task);
            throw exception;
        }
    }

    private void removeTask(Task<?> task) {
        if (javafx.application.Platform.isFxApplicationThread()) {
            tasks.remove(task);
        } else if (!closed.get()) {
            CwaPlatform.runOnFxThread(() -> {
                tasks.remove(task);
                return null;
            });
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        if (cancelOnShutdown) executor.shutdownNow();
        else executor.shutdown();

        Runnable clearTasks = () -> {
            if (cancelOnShutdown) tasks.forEach(Task::cancel);
            tasks.clear();
        };
        if (javafx.application.Platform.isFxApplicationThread()) {
            clearTasks.run();
        } else if (CwaFX.getInstance() != null) {
            try {
                CwaPlatform.runOnFxThread(() -> {
                    clearTasks.run();
                    return null;
                });
            } catch (IllegalStateException ignored) {
                tasks.clear();
            }
        } else {
            tasks.clear();
        }
    }

    boolean isClosed() {
        return closed.get();
    }

    /**
     * Creates a view that displays the list of tasks.
     * @return a TasksView that displays the list of tasks
     */
    public TasksView createView() {
        return new TasksView(tasks);
    }
}
