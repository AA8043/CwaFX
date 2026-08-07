package org.a8043.cwaFX.tasks;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

public class TasksView extends ListView<Task<?>> {
    public TasksView(ObservableList<Task<?>> tasks) {
        super(tasks);
        setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Task<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    setGraphic(new HBox(
                        new Label() {{
                            textProperty().bind(item.titleProperty());
                        }},
                        new ProgressBar() {{
                            progressProperty().bind(item.progressProperty());
                        }},
                        new Button("x") {{
                            getStyleClass().add("cancel-task-button");
                            setOnAction(e -> item.cancel());
                        }}
                    ) {{
                        setPadding(new Insets(0, 4, 0, 0));
                    }});
                } else {
                    setGraphic(null);
                }
            }
        });
    }
}
