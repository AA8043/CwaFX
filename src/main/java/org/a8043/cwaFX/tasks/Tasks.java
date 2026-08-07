package org.a8043.cwaFX.tasks;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.CwaFX;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Tasks {
    private final ThreadPoolExecutor executor;
    @Getter
    private final ObservableList<Task<?>> tasks = FXCollections.observableArrayList();

    public Tasks(CwaFX cwaFX) {
        int threadCount = cwaFX.getConfig().getTaskThreadPoolSize();
        executor = new ThreadPoolExecutor(threadCount, threadCount,
            60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public void executeTask(Task<?> task) {
        task.setOnCancelled(e -> tasks.remove(task));
        task.setOnFailed(e -> {
            log.error("Task failed: {}", task.getTitle(), task.getException());
            tasks.remove(task);
        });
        task.setOnSucceeded(e -> tasks.remove(task));
        tasks.add(task);
        executor.execute(task);
    }

    public TasksView createView() {
        return new TasksView(tasks);
    }
}
