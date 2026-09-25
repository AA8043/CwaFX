package org.a8043.cwaFX.tasks;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * A view that displays a list of tasks.
 */
public class TasksView extends ListView<Task<?>> {
    public TasksView(ObservableList<Task<?>> tasks) {
        super(tasks);
        setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setGraphic(new TaskBox(item));
                } else {
                    setGraphic(null);
                }
            }
        });
    }

    private static class TaskBox extends HBox {
        public TaskBox(Task<?> task) {
            ProgressBar progressBar = new ProgressBar() {{
                setMaxWidth(Double.MAX_VALUE);
                progressProperty().bind(task.progressProperty());
            }};
            getChildren().addAll(
                new Label() {{
                    textProperty().bind(task.titleProperty());
                }},
                progressBar,
                new Button("x") {{
                    getStyleClass().add("cancel-task-button");
                    setOnAction(e -> task.cancel());
                }}
            );
            HBox.setHgrow(progressBar, Priority.ALWAYS);

            setAlignment(Pos.CENTER);
            setSpacing(5);
        }
    }
}
